// Package middleware provides HTTP middleware for the OSB broker.
package middleware

import (
	"log/slog"
	"net/http"
)

// APIInfoValidator validates the X-Api-Info-Location header if required.
type APIInfoValidator struct {
	required bool
	logger   *slog.Logger
}

// NewAPIInfoValidator creates a new API info validator middleware.
func NewAPIInfoValidator(required bool) *APIInfoValidator {
	return &APIInfoValidator{
		required: required,
		logger:   slog.Default().With("component", "apiinfo"),
	}
}

// Handler wraps an http.Handler with API info validation.
func (v *APIInfoValidator) Handler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		apiInfoLocation := r.Header.Get("X-Api-Info-Location")

		if v.required && apiInfoLocation == "" {
			v.logger.Warn("missing X-Api-Info-Location header",
				"path", r.URL.Path,
				"method", r.Method)
			http.Error(w, "X-Api-Info-Location header required", http.StatusBadRequest)
			return
		}

		if apiInfoLocation != "" {
			v.logger.Debug("X-Api-Info-Location header present",
				"location", apiInfoLocation,
				"path", r.URL.Path)
		}

		next.ServeHTTP(w, r)
	})
}

// Middleware returns the handler function for use with http middleware chains.
func (v *APIInfoValidator) Middleware(next http.Handler) http.Handler {
	return v.Handler(next)
}
