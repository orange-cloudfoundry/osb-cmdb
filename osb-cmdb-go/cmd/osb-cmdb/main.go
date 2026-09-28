// Package main is the entry point for the OSB-CMDB service broker.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"code.cloudfoundry.org/brokerapi/v13"
	"code.cloudfoundry.org/brokerapi/v13/domain"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/broker"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/catalog"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/cfclient"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/config"
	osbcontext "github.com/orange-cloudfoundry/osb-cmdb-go/internal/context"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/metadata"
)

// Version is set at build time via -ldflags.
var Version = "dev"

// Commit and BuildDate are set at build time via -ldflags.
var (
	Commit    = "unknown"
	BuildDate = "unknown"
)

func main() {
	if err := run(); err != nil {
		slog.Error("failed to start broker", "error", err)
		os.Exit(1)
	}
}

func run() error {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		return fmt.Errorf("loading config: %w", err)
	}

	// Setup logging
	level := parseLogLevel(cfg.LogLevel)
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: level}))
	slog.SetDefault(logger)

	slog.Info("starting osb-cmdb broker", "version", Version, "port", cfg.Port)

	handler, stopCatalog, err := newHandler(context.Background(), cfg)
	if err != nil {
		return err
	}
	defer stopCatalog()

	server := &http.Server{
		Addr:         fmt.Sprintf(":%d", cfg.Port),
		Handler:      handler,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Graceful shutdown
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	go func() {
		slog.Info("listening", "addr", server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("server error", "error", err)
		}
	}()

	<-ctx.Done()
	slog.Info("shutting down gracefully")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		return fmt.Errorf("shutdown: %w", err)
	}

	slog.Info("shutdown complete")
	return nil
}

func newHandler(ctx context.Context, cfg *config.Config) (http.Handler, func(), error) {
	cf, err := cfclient.New(ctx, &cfclient.ClientConfig{
		APIHost:      cfg.CFAPIHost,
		Username:     cfg.CFUsername,
		Password:     cfg.CFPassword,
		ClientID:     cfg.CFClientID,
		ClientSecret: cfg.CFClientSecret,
		DefaultOrg:   cfg.CFDefaultOrg,
		DefaultSpace: cfg.CFDefaultSpace,
		SkipSSL:      cfg.CFSkipSSL,
	})
	if err != nil {
		return nil, nil, fmt.Errorf("creating CF client: %w", err)
	}

	defaultSpaceGUID, err := cf.GetDefaultSpaceGUID(ctx)
	if err != nil {
		return nil, nil, fmt.Errorf("resolving default CF space: %w", err)
	}

	catalogService := catalog.NewDynamicCatalog(cf, &catalog.Config{
		ServiceSuffix:    cfg.ServiceSuffix,
		ExcludedServices: cfg.ExcludedServices,
		RefreshInterval:  cfg.CatalogRefreshInterval.String(),
	})
	if err := catalogService.Refresh(ctx); err != nil {
		return nil, nil, fmt.Errorf("initial catalog refresh: %w", err)
	}
	catalogService.Start(ctx)

	serviceBroker, err := broker.New(&broker.Config{
		CFClient:         cf,
		Catalog:          catalogService,
		MetadataService:  metadata.NewFormatter(),
		ContextParser:    osbcontext.NewParser(),
		DefaultSpaceGUID: defaultSpaceGUID,
	})
	if err != nil {
		catalogService.Stop()
		return nil, nil, fmt.Errorf("creating broker: %w", err)
	}

	return newHTTPHandler(serviceBroker, cfg), catalogService.Stop, nil
}

func newHTTPHandler(serviceBroker domain.ServiceBroker, cfg *config.Config) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", writeOK)
	mux.HandleFunc("GET /ready", writeOK)
	mux.Handle("/", brokerapi.New(serviceBroker, slog.Default(), brokerapi.BrokerCredentials{
		Username: cfg.Username,
		Password: cfg.Password,
	}))
	return mux
}

func writeOK(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
	if _, err := w.Write([]byte("OK")); err != nil {
		slog.Error("writing health response", "error", err)
	}
}

func parseLogLevel(level string) slog.Level {
	switch level {
	case "debug":
		return slog.LevelDebug
	case "info":
		return slog.LevelInfo
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}
