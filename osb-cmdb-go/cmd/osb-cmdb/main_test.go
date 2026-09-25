package main

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"code.cloudfoundry.org/brokerapi/v13/domain"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/config"
	"github.com/stretchr/testify/require"
)

type wiringBroker struct{}

func (wiringBroker) Services(context.Context) ([]domain.Service, error) {
	return []domain.Service{{ID: "service-id", Name: "service", Plans: []domain.ServicePlan{{ID: "plan-id", Name: "plan"}}}}, nil
}

func (wiringBroker) Provision(context.Context, string, domain.ProvisionDetails, bool) (domain.ProvisionedServiceSpec, error) {
	return domain.ProvisionedServiceSpec{}, nil
}
func (wiringBroker) Deprovision(context.Context, string, domain.DeprovisionDetails, bool) (domain.DeprovisionServiceSpec, error) {
	return domain.DeprovisionServiceSpec{}, nil
}
func (wiringBroker) GetInstance(context.Context, string, domain.FetchInstanceDetails) (domain.GetInstanceDetailsSpec, error) {
	return domain.GetInstanceDetailsSpec{}, nil
}
func (wiringBroker) Update(context.Context, string, domain.UpdateDetails, bool) (domain.UpdateServiceSpec, error) {
	return domain.UpdateServiceSpec{}, nil
}
func (wiringBroker) LastOperation(context.Context, string, domain.PollDetails) (domain.LastOperation, error) {
	return domain.LastOperation{}, nil
}
func (wiringBroker) Bind(context.Context, string, string, domain.BindDetails, bool) (domain.Binding, error) {
	return domain.Binding{}, nil
}
func (wiringBroker) Unbind(context.Context, string, string, domain.UnbindDetails, bool) (domain.UnbindSpec, error) {
	return domain.UnbindSpec{}, nil
}
func (wiringBroker) GetBinding(context.Context, string, string, domain.FetchBindingDetails) (domain.GetBindingSpec, error) {
	return domain.GetBindingSpec{}, nil
}
func (wiringBroker) LastBindingOperation(context.Context, string, string, domain.PollDetails) (domain.LastOperation, error) {
	return domain.LastOperation{}, nil
}

func TestNewHTTPHandlerMountsCatalogRoute(t *testing.T) {
	handler := newHTTPHandler(wiringBroker{}, &config.Config{Username: "user", Password: "pass"})
	req := httptest.NewRequest(http.MethodGet, "/v2/catalog", nil)
	req.Header.Set("X-Broker-API-Version", "2.14")
	req.SetBasicAuth("user", "pass")
	resp := httptest.NewRecorder()

	handler.ServeHTTP(resp, req)

	require.Equal(t, http.StatusOK, resp.Code)
	require.Contains(t, resp.Body.String(), `"id":"service-id"`)
}

func TestNewHTTPHandlerReadiness(t *testing.T) {
	handler := newHTTPHandler(wiringBroker{}, &config.Config{})
	resp := httptest.NewRecorder()

	handler.ServeHTTP(resp, httptest.NewRequest(http.MethodGet, "/ready", nil))

	require.Equal(t, http.StatusOK, resp.Code)
}
