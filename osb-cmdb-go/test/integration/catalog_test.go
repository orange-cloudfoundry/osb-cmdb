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

// TestCatalog_Success tests successful catalog fetch.
func TestCatalog_Success(t *testing.T) {
	broker := &mockBroker{
		catalog: []domain.Service{
			{
				ID:          "mysql-service-id",
				Name:        "mysql",
				Description: "MySQL Database",
				Bindable:    true,
				Tags:        []string{"database", "mysql"},
				Plans: []domain.ServicePlan{
					{
						ID:          "small-plan-id",
						Name:        "small",
						Description: "Small MySQL instance",
					},
					{
						ID:          "large-plan-id",
						Name:        "large",
						Description: "Large MySQL instance",
					},
				},
			},
			{
				ID:          "redis-service-id",
				Name:        "redis",
				Description: "Redis Cache",
				Bindable:    true,
				Tags:        []string{"cache", "redis"},
				Plans: []domain.ServicePlan{
					{
						ID:          "standard-plan-id",
						Name:        "standard",
						Description: "Standard Redis instance",
					},
				},
			},
		},
	}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet, server.URL+"/v2/catalog", nil)
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

	services := result["services"].([]interface{})
	assert.Len(t, services, 2)

	// Check first service
	mysqlService := services[0].(map[string]interface{})
	assert.Equal(t, "mysql-service-id", mysqlService["id"])
	assert.Equal(t, "mysql", mysqlService["name"])
	assert.Equal(t, "MySQL Database", mysqlService["description"])
	assert.True(t, mysqlService["bindable"].(bool))

	plans := mysqlService["plans"].([]interface{})
	assert.Len(t, plans, 2)
}

// TestCatalog_Unauthenticated tests catalog without auth.
func TestCatalog_Unauthenticated(t *testing.T) {
	broker := &mockBroker{}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet, server.URL+"/v2/catalog", nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	// No auth

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
}

// TestCatalog_WrongCredentials tests catalog with wrong credentials.
func TestCatalog_WrongCredentials(t *testing.T) {
	broker := &mockBroker{}

	handler := brokerapi.New(broker, newTestLogger(), brokerapi.BrokerCredentials{
		Username: "user",
		Password: "pass",
	})

	server := httptest.NewServer(handler)
	defer server.Close()

	req, _ := http.NewRequest(http.MethodGet, server.URL+"/v2/catalog", nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("wrong", "credentials")

	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() { require.NoError(t, resp.Body.Close()) }()

	assert.Equal(t, http.StatusUnauthorized, resp.StatusCode)
}
