package cfclient

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestClientConfig_Validation(t *testing.T) {
	tests := []struct {
		name    string
		config  *ClientConfig
		wantErr bool
	}{
		{
			name:    "nil config",
			config:  nil,
			wantErr: true,
		},
		{
			name: "valid config",
			config: &ClientConfig{
				APIHost:      "https://api.cf.example.com",
				Username:     "admin",
				Password:     "password",
				DefaultOrg:   "test-org",
				DefaultSpace: "test-space",
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// We can't actually create a client without a real CF endpoint,
			// but we can test the config validation in New()
			if tt.config == nil {
				_, err := New(nil, tt.config)
				assert.Error(t, err)
				assert.Contains(t, err.Error(), "config is required")
			}
		})
	}
}

func TestServiceInstanceParams(t *testing.T) {
	params := &ServiceInstanceParams{
		Name:            "test-instance",
		ServicePlanGUID: "plan-guid-123",
		SpaceGUID:       "space-guid-456",
		Parameters: map[string]interface{}{
			"key": "value",
		},
		Tags: []string{"tag1", "tag2"},
		Labels: map[string]*string{
			"label-key": ptrStr("label-value"),
		},
		Annotations: map[string]*string{
			"annotation-key": ptrStr("annotation-value"),
		},
	}

	assert.Equal(t, "test-instance", params.Name)
	assert.Equal(t, "plan-guid-123", params.ServicePlanGUID)
	assert.Equal(t, "space-guid-456", params.SpaceGUID)
	assert.Equal(t, "value", params.Parameters["key"])
	assert.Contains(t, params.Tags, "tag1")
	assert.Equal(t, "label-value", *params.Labels["label-key"])
	assert.Equal(t, "annotation-value", *params.Annotations["annotation-key"])
}

func TestServiceBindingParams(t *testing.T) {
	params := &ServiceBindingParams{
		Name:                "test-binding",
		ServiceInstanceGUID: "instance-guid-123",
		Parameters: map[string]interface{}{
			"role": "admin",
		},
	}

	assert.Equal(t, "test-binding", params.Name)
	assert.Equal(t, "instance-guid-123", params.ServiceInstanceGUID)
	assert.Equal(t, "admin", params.Parameters["role"])
}

func TestUpdateServiceInstanceParams(t *testing.T) {
	name := "new-name"
	planGUID := "new-plan-guid"

	params := &UpdateServiceInstanceParams{
		Name:            &name,
		ServicePlanGUID: &planGUID,
		Parameters: map[string]interface{}{
			"new-param": "new-value",
		},
		Tags: []string{"new-tag"},
	}

	assert.Equal(t, "new-name", *params.Name)
	assert.Equal(t, "new-plan-guid", *params.ServicePlanGUID)
	assert.Equal(t, "new-value", params.Parameters["new-param"])
	assert.Contains(t, params.Tags, "new-tag")
}

func TestCreateServiceInstanceResult(t *testing.T) {
	// Test sync result
	syncResult := &CreateServiceInstanceResult{
		ServiceInstance: nil, // Would be populated by actual call
		JobGUID:         "",
		IsAsync:         false,
	}
	assert.False(t, syncResult.IsAsync)
	assert.Empty(t, syncResult.JobGUID)

	// Test async result
	asyncResult := &CreateServiceInstanceResult{
		ServiceInstance: nil,
		JobGUID:         "job-guid-123",
		IsAsync:         true,
	}
	assert.True(t, asyncResult.IsAsync)
	assert.Equal(t, "job-guid-123", asyncResult.JobGUID)
}

func TestDeleteServiceInstanceResult(t *testing.T) {
	// Test sync result
	syncResult := &DeleteServiceInstanceResult{
		JobGUID: "",
		IsAsync: false,
	}
	assert.False(t, syncResult.IsAsync)

	// Test async result
	asyncResult := &DeleteServiceInstanceResult{
		JobGUID: "job-guid-456",
		IsAsync: true,
	}
	assert.True(t, asyncResult.IsAsync)
	assert.Equal(t, "job-guid-456", asyncResult.JobGUID)
}

func TestCreateServiceBindingResult(t *testing.T) {
	// Test sync result
	syncResult := &CreateServiceBindingResult{
		ServiceBinding: nil,
		JobGUID:        "",
		IsAsync:        false,
	}
	assert.False(t, syncResult.IsAsync)

	// Test async result
	asyncResult := &CreateServiceBindingResult{
		ServiceBinding: nil,
		JobGUID:        "binding-job-123",
		IsAsync:        true,
	}
	assert.True(t, asyncResult.IsAsync)
	assert.Equal(t, "binding-job-123", asyncResult.JobGUID)
}

// Helper function to create string pointers
func ptrStr(s string) *string {
	return &s
}
