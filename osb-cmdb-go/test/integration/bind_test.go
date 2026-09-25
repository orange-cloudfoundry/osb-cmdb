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

// TestBind_Sync tests synchronous binding.
func TestBind_Sync(t *testing.T) {
	broker := &mockBroker{
		bindResponse: domain.Binding{
			Credentials: map[string]interface{}{
				"username": "db-user",
				"password": "db-pass",
				"host":     "db.example.com",
				"port":     5432,
			},
		},
	}

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
		server.URL+"/v2/service_instances/instance-123/service_bindings/binding-456?accepts_incomplete=true",
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

	credentials := result["credentials"].(map[string]interface{})
	assert.Equal(t, "db-user", credentials["username"])
	assert.Equal(t, "db-pass", credentials["password"])
	assert.Equal(t, "db.example.com", credentials["host"])
}

// TestBind_Async tests asynchronous binding.
func TestBind_Async(t *testing.T) {
	broker := &mockBroker{
		bindResponse: domain.Binding{
			IsAsync:       true,
			OperationData: "bind-operation-123",
		},
	}

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
		server.URL+"/v2/service_instances/instance-123/service_bindings/binding-456?accepts_incomplete=true",
		bytes.NewReader(bodyBytes))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusAccepted, resp.StatusCode)
}

// TestUnbind_Sync tests synchronous unbinding.
func TestUnbind_Sync(t *testing.T) {
	broker := &mockBroker{
		unbindResponse: domain.UnbindSpec{
			IsAsync: false,
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodDelete,
		server.URL+"/v2/service_instances/instance-123/service_bindings/binding-456?service_id=service-123&plan_id=plan-456&accepts_incomplete=true",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)
}
