package integration

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/pivotal-cf/brokerapi/v11"
	"github.com/pivotal-cf/brokerapi/v11/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// TestDeprovision_Sync tests synchronous deprovisioning.
func TestDeprovision_Sync(t *testing.T) {
	broker := &mockBroker{
		deprovisionResponse: domain.DeprovisionServiceSpec{
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
		server.URL+"/v2/service_instances/instance-123?service_id=service-123&plan_id=plan-456&accepts_incomplete=true",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusOK, resp.StatusCode)
}

// TestDeprovision_Async tests asynchronous deprovisioning.
func TestDeprovision_Async(t *testing.T) {
	broker := &mockBroker{
		deprovisionResponse: domain.DeprovisionServiceSpec{
			IsAsync:       true,
			OperationData: "deprovision-operation",
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodDelete,
		server.URL+"/v2/service_instances/instance-123?service_id=service-123&plan_id=plan-456&accepts_incomplete=true",
		nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusAccepted, resp.StatusCode)
}
