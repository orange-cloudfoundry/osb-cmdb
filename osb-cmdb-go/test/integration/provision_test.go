package integration

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/pivotal-cf/brokerapi/v11"
	"github.com/pivotal-cf/brokerapi/v11/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// TestProvision_Sync tests synchronous provisioning.
func TestProvision_Sync(t *testing.T) {
	// Setup mock broker
	broker := &mockBroker{
		provisionResponse: domain.ProvisionedServiceSpec{
			IsAsync:      false,
			DashboardURL: "https://dashboard.example.com",
		},
	}

	// Create brokerapi handler
	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	// Create test server
	server := httptest.NewServer(handler)
	defer server.Close()

	// Make provision request
	body := map[string]interface{}{
		"service_id":        "service-123",
		"plan_id":           "plan-456",
		"organization_guid": "org-guid",
		"space_guid":        "space-guid",
	}
	bodyBytes, _ := json.Marshal(body)

	req, _ := http.NewRequest(http.MethodPut,
		server.URL+"/v2/service_instances/instance-123?accepts_incomplete=true",
		bytes.NewReader(bodyBytes))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusCreated, resp.StatusCode)

	var result map[string]interface{}
	respBody, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, json.Unmarshal(respBody, &result))

	assert.Equal(t, "https://dashboard.example.com", result["dashboard_url"])
}

// TestProvision_Async tests asynchronous provisioning.
func TestProvision_Async(t *testing.T) {
	// Setup mock broker
	broker := &mockBroker{
		provisionResponse: domain.ProvisionedServiceSpec{
			IsAsync:       true,
			OperationData: "operation-data-123",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	body := map[string]interface{}{
		"service_id":        "service-123",
		"plan_id":           "plan-456",
		"organization_guid": "org-guid",
		"space_guid":        "space-guid",
	}
	bodyBytes, _ := json.Marshal(body)

	req, _ := http.NewRequest(http.MethodPut,
		server.URL+"/v2/service_instances/instance-123?accepts_incomplete=true",
		bytes.NewReader(bodyBytes))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusAccepted, resp.StatusCode)

	var result map[string]interface{}
	respBody, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, json.Unmarshal(respBody, &result))

	assert.Equal(t, "operation-data-123", result["operation"])
}

// TestProvision_Duplicate tests duplicate instance detection.
func TestProvision_Duplicate(t *testing.T) {
	broker := &mockBroker{
		provisionResponse: domain.ProvisionedServiceSpec{
			AlreadyExists: true,
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	body := map[string]interface{}{
		"service_id":        "service-123",
		"plan_id":           "plan-456",
		"organization_guid": "org-guid",
		"space_guid":        "space-guid",
	}
	bodyBytes, _ := json.Marshal(body)

	req, _ := http.NewRequest(http.MethodPut,
		server.URL+"/v2/service_instances/instance-123?accepts_incomplete=true",
		bytes.NewReader(bodyBytes))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)
}

// TestProvision_Unauthorized tests authentication.
func TestProvision_Unauthorized(t *testing.T) {
	broker := &mockBroker{}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	body := map[string]interface{}{
		"service_id": "service-123",
		"plan_id":    "plan-456",
	}
	bodyBytes, _ := json.Marshal(body)

	req, _ := http.NewRequest(http.MethodPut,
		server.URL+"/v2/service_instances/instance-123",
		bytes.NewReader(bodyBytes))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Broker-API-Version", "2.14")
	// No auth

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
}
