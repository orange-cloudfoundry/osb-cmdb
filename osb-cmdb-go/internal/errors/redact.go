// Package errors provides error handling utilities for OSB-CMDB.
package errors

import (
	"errors"
	"fmt"
	"regexp"
)

// urlPattern matches URLs in error messages.
// Matches http:// and https:// URLs with optional paths and query strings.
var urlPattern = regexp.MustCompile(`https?://[^\s"'<>]+`)

// RedactedError wraps an error and redacts sensitive URLs from its message.
type RedactedError struct {
	original error
	redacted string
}

// Error returns the redacted error message.
func (e *RedactedError) Error() string {
	return e.redacted
}

// Unwrap returns the original error.
func (e *RedactedError) Unwrap() error {
	return e.original
}

// RedactError wraps an error and redacts URLs from its message.
// This is used to prevent leaking CF API URLs to OSB clients.
func RedactError(err error) error {
	if err == nil {
		return nil
	}

	redacted := RedactURLs(err.Error())
	if redacted == err.Error() {
		// No URLs to redact, return original error
		return err
	}

	return &RedactedError{
		original: err,
		redacted: redacted,
	}
}

// RedactURLs replaces all URLs in a string with "redacted-url".
func RedactURLs(s string) string {
	return urlPattern.ReplaceAllString(s, "redacted-url")
}

// WrapError wraps an error with additional context and redacts URLs.
func WrapError(err error, message string) error {
	if err == nil {
		return nil
	}
	wrapped := fmt.Errorf("%s: %w", message, err)
	return RedactError(wrapped)
}

// IsNotFound checks if an error represents a "not found" condition.
func IsNotFound(err error) bool {
	if err == nil {
		return false
	}
	// Check for common CF client not found errors
	var notFoundErr *NotFoundError
	return errors.As(err, &notFoundErr)
}

// NotFoundError represents a resource not found error.
type NotFoundError struct {
	Resource string
	ID       string
}

func (e *NotFoundError) Error() string {
	return fmt.Sprintf("%s not found: %s", e.Resource, e.ID)
}

// NewNotFoundError creates a new NotFoundError.
func NewNotFoundError(resource, id string) error {
	return &NotFoundError{Resource: resource, ID: id}
}

// ConflictError represents a resource conflict error.
type ConflictError struct {
	Resource string
	ID       string
	Message  string
}

func (e *ConflictError) Error() string {
	if e.Message != "" {
		return e.Message
	}
	return fmt.Sprintf("%s already exists: %s", e.Resource, e.ID)
}

// NewConflictError creates a new ConflictError.
func NewConflictError(resource, id, message string) error {
	return &ConflictError{Resource: resource, ID: id, Message: message}
}

// IsConflict checks if an error represents a conflict condition.
func IsConflict(err error) bool {
	if err == nil {
		return false
	}
	var conflictErr *ConflictError
	return errors.As(err, &conflictErr)
}
