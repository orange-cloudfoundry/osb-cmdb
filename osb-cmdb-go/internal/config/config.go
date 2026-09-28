// Package config provides configuration loading from environment variables.
package config

import (
	"time"

	"github.com/kelseyhightower/envconfig"
)

// Config holds all configuration for the OSB-CMDB broker.
type Config struct {
	// Server configuration
	Port     int    `envconfig:"PORT" default:"8080"`
	Username string `envconfig:"BROKER_USERNAME" required:"true"`
	Password string `envconfig:"BROKER_PASSWORD" required:"true"`

	// Cloud Foundry configuration
	CFAPIHost      string `envconfig:"CF_API_HOST" required:"true"`
	CFUsername     string `envconfig:"CF_USERNAME" required:"true"`
	CFPassword     string `envconfig:"CF_PASSWORD" required:"true"`
	CFClientID     string `envconfig:"CF_CLIENT_ID"`
	CFClientSecret string `envconfig:"CF_CLIENT_SECRET"`
	CFDefaultOrg   string `envconfig:"CF_DEFAULT_ORG" required:"true"`
	CFDefaultSpace string `envconfig:"CF_DEFAULT_SPACE" required:"true"`
	CFSkipSSL      bool   `envconfig:"CF_SKIP_SSL_VALIDATION" default:"false"`

	// Catalog configuration
	CatalogRefreshInterval time.Duration `envconfig:"CATALOG_REFRESH_INTERVAL" default:"5m"`
	ServiceSuffix          string        `envconfig:"SERVICE_SUFFIX"`
	ExcludedServices       []string      `envconfig:"EXCLUDED_SERVICES"`

	// Feature flags
	PropagateMetadata bool `envconfig:"PROPAGATE_METADATA" default:"true"`

	// Logging
	LogLevel string `envconfig:"LOG_LEVEL" default:"info"`
}

// Load reads configuration from environment variables.
func Load() (*Config, error) {
	var cfg Config
	if err := envconfig.Process("", &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}

// LoadWithPrefix reads configuration with a custom prefix.
func LoadWithPrefix(prefix string) (*Config, error) {
	var cfg Config
	if err := envconfig.Process(prefix, &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}
