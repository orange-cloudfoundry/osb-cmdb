package integration

import (
	"context"
	"log/slog"
	"os"

	"github.com/pivotal-cf/brokerapi/v11/domain"
)

// mockBroker is a simple mock broker for integration tests.
type mockBroker struct {
	provisionResponse   domain.ProvisionedServiceSpec
	provisionError      error
	deprovisionResponse domain.DeprovisionServiceSpec
	deprovisionError    error
	bindResponse        domain.Binding
	bindError           error
	unbindResponse      domain.UnbindSpec
	unbindError         error
	getInstanceResponse domain.GetInstanceDetailsSpec
	getInstanceError    error
	updateResponse      domain.UpdateServiceSpec
	updateError         error
	lastOpResponse      domain.LastOperation
	lastOpError         error
	catalog             []domain.Service
}

func (b *mockBroker) Services(ctx context.Context) ([]domain.Service, error) {
	if b.catalog != nil {
		return b.catalog, nil
	}
	return []domain.Service{
		{
			ID:          "service-123",
			Name:        "test-service",
			Description: "A test service",
			Bindable:    true,
			Plans: []domain.ServicePlan{
				{
					ID:          "plan-456",
					Name:        "small",
					Description: "Small plan",
				},
			},
		},
	}, nil
}

func (b *mockBroker) Provision(ctx context.Context, instanceID string, details domain.ProvisionDetails, asyncAllowed bool) (domain.ProvisionedServiceSpec, error) {
	return b.provisionResponse, b.provisionError
}

func (b *mockBroker) Deprovision(ctx context.Context, instanceID string, details domain.DeprovisionDetails, asyncAllowed bool) (domain.DeprovisionServiceSpec, error) {
	return b.deprovisionResponse, b.deprovisionError
}

func (b *mockBroker) GetInstance(ctx context.Context, instanceID string, details domain.FetchInstanceDetails) (domain.GetInstanceDetailsSpec, error) {
	return b.getInstanceResponse, b.getInstanceError
}

func (b *mockBroker) Update(ctx context.Context, instanceID string, details domain.UpdateDetails, asyncAllowed bool) (domain.UpdateServiceSpec, error) {
	return b.updateResponse, b.updateError
}

func (b *mockBroker) LastOperation(ctx context.Context, instanceID string, details domain.PollDetails) (domain.LastOperation, error) {
	return b.lastOpResponse, b.lastOpError
}

func (b *mockBroker) Bind(ctx context.Context, instanceID, bindingID string, details domain.BindDetails, asyncAllowed bool) (domain.Binding, error) {
	return b.bindResponse, b.bindError
}

func (b *mockBroker) Unbind(ctx context.Context, instanceID, bindingID string, details domain.UnbindDetails, asyncAllowed bool) (domain.UnbindSpec, error) {
	return b.unbindResponse, b.unbindError
}

func (b *mockBroker) GetBinding(ctx context.Context, instanceID, bindingID string, details domain.FetchBindingDetails) (domain.GetBindingSpec, error) {
	return domain.GetBindingSpec{}, nil
}

func (b *mockBroker) LastBindingOperation(ctx context.Context, instanceID, bindingID string, details domain.PollDetails) (domain.LastOperation, error) {
	return b.lastOpResponse, b.lastOpError
}

func newTestLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelError}))
}
