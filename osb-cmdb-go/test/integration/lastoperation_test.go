package integration

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"code.cloudfoundry.org/brokerapi/v13"
	"code.cloudfoundry.org/brokerapi/v13/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// TestLastOperation_InProgress tests polling an in-progress operation.
func TestLastOperation_InProgress(t *testing.T) {
	broker := &mockBroker{
		lastOpResponse: domain.LastOperation{
			State:       domain.InProgress,
			Description: "Creating service instance",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet,
		server.URL+"/v2/service_instances/instance-123/last_operation?service_id=service-123&plan_id=plan-456",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)

	var result map[string]interface{}
	respBody, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, json.Unmarshal(respBody, &result))

	assert.Equal(t, "in progress", result["state"])
	assert.Equal(t, "Creating service instance", result["description"])
}

// TestLastOperation_Succeeded tests polling a succeeded operation.
func TestLastOperation_Succeeded(t *testing.T) {
	broker := &mockBroker{
		lastOpResponse: domain.LastOperation{
			State:       domain.Succeeded,
			Description: "Service instance created",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet,
		server.URL+"/v2/service_instances/instance-123/last_operation?service_id=service-123&plan_id=plan-456",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)

	var result map[string]interface{}
	respBody, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, json.Unmarshal(respBody, &result))

	assert.Equal(t, "succeeded", result["state"])
}

// TestLastOperation_Failed tests polling a failed operation.
func TestLastOperation_Failed(t *testing.T) {
	broker := &mockBroker{
		lastOpResponse: domain.LastOperation{
			State:       domain.Failed,
			Description: "Service instance creation failed: quota exceeded",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet,
		server.URL+"/v2/service_instances/instance-123/last_operation?service_id=service-123&plan_id=plan-456",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)

	var result map[string]interface{}
	respBody, err := io.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, json.Unmarshal(respBody, &result))

	assert.Equal(t, "failed", result["state"])
	assert.Contains(t, result["description"], "quota exceeded")
}

// TestLastOperation_WithOperationData tests polling with operation data.
func TestLastOperation_WithOperationData(t *testing.T) {
	broker := &mockBroker{
		lastOpResponse: domain.LastOperation{
			State:       domain.InProgress,
			Description: "Processing",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet,
		server.URL+"/v2/service_instances/instance-123/last_operation?service_id=service-123&plan_id=plan-456&operation=operation-data-123",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)
}
