package broker

import (
	"encoding/base64"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestOperationState_EncodeAndDecode(t *testing.T) {
	tests := []struct {
		name string
		op   *OperationState
	}{
		{
			name: "provision operation",
			op: &OperationState{
				Type:                       OperationProvision,
				BackingCFServiceInstanceID: "cf-guid-123",
			},
		},
		{
			name: "deprovision operation with job",
			op: &OperationState{
				Type:                       OperationDeprovision,
				BackingCFServiceInstanceID: "cf-guid-456",
				JobGUID:                    "job-789",
			},
		},
		{
			name: "bind operation",
			op: &OperationState{
				Type:                       OperationBind,
				BackingCFServiceInstanceID: "cf-guid-abc",
				JobGUID:                    "job-def",
			},
		},
		{
			name: "unbind operation",
			op: &OperationState{
				Type:    OperationUnbind,
				JobGUID: "job-unbind",
			},
		},
		{
			name: "update operation",
			op: &OperationState{
				Type:                       OperationUpdate,
				BackingCFServiceInstanceID: "cf-guid-update",
				JobGUID:                    "job-update",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			encoded := tt.op.Encode()
			assert.NotEmpty(t, encoded)

			decoded, err := DecodeOperationState(encoded)
			require.NoError(t, err)
			assert.Equal(t, tt.op.Type, decoded.Type)
			assert.Equal(t, tt.op.BackingCFServiceInstanceID, decoded.BackingCFServiceInstanceID)
			assert.Equal(t, tt.op.JobGUID, decoded.JobGUID)
		})
	}
}

func TestDecodeOperationState_RawJSON(t *testing.T) {
	// Test backwards compatibility with raw JSON
	op := &OperationState{
		Type:                       OperationProvision,
		BackingCFServiceInstanceID: "guid-123",
	}

	rawJSON, _ := json.Marshal(op)

	decoded, err := DecodeOperationState(string(rawJSON))
	require.NoError(t, err)
	assert.Equal(t, OperationProvision, decoded.Type)
	assert.Equal(t, "guid-123", decoded.BackingCFServiceInstanceID)
}

func TestDecodeOperationState_Invalid(t *testing.T) {
	tests := []struct {
		name    string
		input   string
		wantErr bool
	}{
		{
			name:    "empty string",
			input:   "",
			wantErr: true,
		},
		{
			name:    "invalid base64",
			input:   "not-valid-base64!!!",
			wantErr: true,
		},
		{
			name:    "valid base64 but not JSON",
			input:   base64.StdEncoding.EncodeToString([]byte("not json")),
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := DecodeOperationState(tt.input)
			if tt.wantErr {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func TestNewProvisionOperation(t *testing.T) {
	op := NewProvisionOperation("cf-instance-guid")

	assert.Equal(t, OperationProvision, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Empty(t, op.JobGUID)
}

func TestNewDeprovisionOperation(t *testing.T) {
	op := NewDeprovisionOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationDeprovision, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewUpdateOperation(t *testing.T) {
	op := NewUpdateOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationUpdate, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewBindOperation(t *testing.T) {
	op := NewBindOperation("cf-instance-guid", "job-guid")

	assert.Equal(t, OperationBind, op.Type)
	assert.Equal(t, "cf-instance-guid", op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestNewUnbindOperation(t *testing.T) {
	op := NewUnbindOperation("job-guid")

	assert.Equal(t, OperationUnbind, op.Type)
	assert.Empty(t, op.BackingCFServiceInstanceID)
	assert.Equal(t, "job-guid", op.JobGUID)
}

func TestBrokerConfig_Validation(t *testing.T) {
	tests := []struct {
		name    string
		cfg     *Config
		wantErr string
	}{
		{
			name:    "nil config",
			cfg:     nil,
			wantErr: "config is required",
		},
		{
			name:    "missing CF client",
			cfg:     &Config{},
			wantErr: "CF client is required",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := New(tt.cfg)
			require.Error(t, err)
			assert.Contains(t, err.Error(), tt.wantErr)
		})
	}
}
