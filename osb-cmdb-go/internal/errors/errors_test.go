package errors

import (
	"errors"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRedactURLs(t *testing.T) {
	tests := []struct {
		name  string
		input string
		want  string
	}{
		{
			name:  "no URLs",
			input: "simple error message",
			want:  "simple error message",
		},
		{
			name:  "single HTTP URL",
			input: "failed to connect to http://api.cf.example.com/v3/resources",
			want:  "failed to connect to redacted-url",
		},
		{
			name:  "single HTTPS URL",
			input: "failed to connect to https://api.cf.example.com/v3/resources",
			want:  "failed to connect to redacted-url",
		},
		{
			name:  "multiple URLs",
			input: "error from https://api.cf.example.com and http://other.example.com",
			want:  "error from redacted-url and redacted-url",
		},
		{
			name:  "URL with path and query",
			input: "GET https://api.cf.example.com/v3/service_instances?names=test failed",
			want:  "GET redacted-url failed",
		},
		{
			name:  "URL in JSON",
			input: `{"error":"https://api.example.com/v3/error"}`,
			want:  `{"error":"redacted-url"}`,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := RedactURLs(tt.input)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestRedactError(t *testing.T) {
	t.Run("nil error", func(t *testing.T) {
		result := RedactError(nil)
		assert.Nil(t, result)
	})

	t.Run("error without URL", func(t *testing.T) {
		err := errors.New("simple error")
		result := RedactError(err)
		assert.Equal(t, "simple error", result.Error())
	})

	t.Run("error with URL", func(t *testing.T) {
		err := errors.New("failed to connect to https://api.cf.example.com/v3")
		result := RedactError(err)
		assert.Equal(t, "failed to connect to redacted-url", result.Error())
	})

	t.Run("unwrap preserves original", func(t *testing.T) {
		original := errors.New("https://secret.url.com/api")
		result := RedactError(original)

		var redactedErr *RedactedError
		if errors.As(result, &redactedErr) {
			assert.Contains(t, redactedErr.Unwrap().Error(), "https://secret.url.com")
		}
	})
}

func TestWrapError(t *testing.T) {
	t.Run("nil error", func(t *testing.T) {
		result := WrapError(nil, "context")
		assert.Nil(t, result)
	})

	t.Run("wrap with context", func(t *testing.T) {
		err := errors.New("original error")
		result := WrapError(err, "doing operation")
		assert.Contains(t, result.Error(), "doing operation")
		assert.Contains(t, result.Error(), "original error")
	})

	t.Run("wrap and redact", func(t *testing.T) {
		err := errors.New("error at https://api.example.com")
		result := WrapError(err, "CF API call")
		assert.Contains(t, result.Error(), "CF API call")
		assert.Contains(t, result.Error(), "redacted-url")
		assert.NotContains(t, result.Error(), "api.example.com")
	})
}

func TestNotFoundError(t *testing.T) {
	err := NewNotFoundError("ServiceInstance", "guid-123")
	assert.Equal(t, "ServiceInstance not found: guid-123", err.Error())
	assert.True(t, IsNotFound(err))
	assert.False(t, IsNotFound(errors.New("other error")))
	assert.False(t, IsNotFound(nil))
}

func TestConflictError(t *testing.T) {
	t.Run("with default message", func(t *testing.T) {
		err := NewConflictError("ServiceInstance", "guid-123", "")
		assert.Equal(t, "ServiceInstance already exists: guid-123", err.Error())
		assert.True(t, IsConflict(err))
	})

	t.Run("with custom message", func(t *testing.T) {
		err := NewConflictError("ServiceInstance", "guid-123", "custom conflict message")
		assert.Equal(t, "custom conflict message", err.Error())
		assert.True(t, IsConflict(err))
	})

	t.Run("not conflict", func(t *testing.T) {
		assert.False(t, IsConflict(errors.New("other error")))
		assert.False(t, IsConflict(nil))
	})
}

func TestRedactedError_Unwrap(t *testing.T) {
	original := fmt.Errorf("wrapped: %w", errors.New("inner"))
	redacted := RedactError(original)

	// Should be unwrappable if it's a RedactedError
	var redactedErr *RedactedError
	if errors.As(redacted, &redactedErr) {
		unwrapped := redactedErr.Unwrap()
		assert.Contains(t, unwrapped.Error(), "wrapped")
	}
}
