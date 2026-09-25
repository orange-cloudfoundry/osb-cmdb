package middleware

import (
	"log/slog"
	"net/http"
	"time"
)

// responseWriter wraps http.ResponseWriter to capture status code.
type responseWriter struct {
	http.ResponseWriter
	statusCode int
	written    int64
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

func (rw *responseWriter) Write(b []byte) (int, error) {
	n, err := rw.ResponseWriter.Write(b)
	rw.written += int64(n)
	return n, err
}

// Logger is HTTP request logging middleware.
type Logger struct {
	logger *slog.Logger
}

// NewLogger creates a new logging middleware.
func NewLogger() *Logger {
	return &Logger{
		logger: slog.Default().With("component", "http"),
	}
}

// Handler wraps an http.Handler with request logging.
func (l *Logger) Handler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		// Wrap response writer to capture status
		rw := &responseWriter{
			ResponseWriter: w,
			statusCode:     http.StatusOK,
		}

		// Call the next handler
		next.ServeHTTP(rw, r)

		// Log the request
		duration := time.Since(start)

		// Get request ID if present
		requestID := r.Header.Get("X-Request-ID")
		if requestID == "" {
			requestID = r.Header.Get("X-Correlation-ID")
		}

		// Get originating identity platform
		originatingIdentity := r.Header.Get("X-Broker-API-Originating-Identity")
		var platform string
		if len(originatingIdentity) > 0 {
			// Extract just the platform name (first word)
			for i, c := range originatingIdentity {
				if c == ' ' {
					platform = originatingIdentity[:i]
					break
				}
			}
		}

		l.logger.Info("request completed",
			"method", r.Method,
			"path", r.URL.Path,
			"status", rw.statusCode,
			"duration_ms", duration.Milliseconds(),
			"bytes", rw.written,
			"request_id", requestID,
			"platform", platform,
			"remote_addr", r.RemoteAddr,
		)
	})
}

// Middleware returns the handler function for use with http middleware chains.
func (l *Logger) Middleware(next http.Handler) http.Handler {
	return l.Handler(next)
}

// RecoveryLogger logs panics and recovers.
type RecoveryLogger struct {
	logger *slog.Logger
}

// NewRecoveryLogger creates a new panic recovery middleware.
func NewRecoveryLogger() *RecoveryLogger {
	return &RecoveryLogger{
		logger: slog.Default().With("component", "recovery"),
	}
}

// Handler wraps an http.Handler with panic recovery.
func (r *RecoveryLogger) Handler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		defer func() {
			if err := recover(); err != nil {
				r.logger.Error("panic recovered",
					"error", err,
					"path", req.URL.Path,
					"method", req.Method,
				)
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			}
		}()

		next.ServeHTTP(w, req)
	})
}

// Middleware returns the handler function for use with http middleware chains.
func (r *RecoveryLogger) Middleware(next http.Handler) http.Handler {
	return r.Handler(next)
}
