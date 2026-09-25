package mocks

import (
	"context"

	"code.cloudfoundry.org/brokerapi/v13/domain"
	"github.com/stretchr/testify/mock"
)

// MockCatalogService is a mock implementation of catalog.Service.
type MockCatalogService struct {
	mock.Mock
}

// GetServices mocks getting all services.
func (m *MockCatalogService) GetServices() []domain.Service {
	args := m.Called()
	if args.Get(0) == nil {
		return nil
	}
	return args.Get(0).([]domain.Service)
}

// GetService mocks getting a service by ID.
func (m *MockCatalogService) GetService(serviceID string) *domain.Service {
	args := m.Called(serviceID)
	if args.Get(0) == nil {
		return nil
	}
	return args.Get(0).(*domain.Service)
}

// GetPlan mocks getting a plan by ID.
func (m *MockCatalogService) GetPlan(planID string) *domain.ServicePlan {
	args := m.Called(planID)
	if args.Get(0) == nil {
		return nil
	}
	return args.Get(0).(*domain.ServicePlan)
}

// GetPlanForService mocks getting a plan for a specific service.
func (m *MockCatalogService) GetPlanForService(serviceID, planID string) *domain.ServicePlan {
	args := m.Called(serviceID, planID)
	if args.Get(0) == nil {
		return nil
	}
	return args.Get(0).(*domain.ServicePlan)
}

// Refresh mocks refreshing the catalog.
func (m *MockCatalogService) Refresh(ctx context.Context) error {
	args := m.Called(ctx)
	return args.Error(0)
}

// Start mocks starting periodic refresh.
func (m *MockCatalogService) Start(ctx context.Context) {
	m.Called(ctx)
}

// Stop mocks stopping periodic refresh.
func (m *MockCatalogService) Stop() {
	m.Called()
}

// TestService returns a test service for use in tests.
func TestService() domain.Service {
	return domain.Service{
		ID:          "service-id-123",
		Name:        "test-service",
		Description: "A test service for testing",
		Bindable:    true,
		Plans: []domain.ServicePlan{
			TestPlan(),
			{
				ID:          "plan-id-large",
				Name:        "large",
				Description: "Large plan",
			},
		},
	}
}

// TestPlan returns a test plan for use in tests.
func TestPlan() domain.ServicePlan {
	free := true
	return domain.ServicePlan{
		ID:          "plan-id-small",
		Name:        "small",
		Description: "Small plan",
		Free:        &free,
	}
}
