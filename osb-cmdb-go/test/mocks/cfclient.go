// Package mocks provides mock implementations for testing.
package mocks

import (
	"context"
	"time"

	"github.com/cloudfoundry/go-cfclient/v3/resource"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/cfclient"
	"github.com/stretchr/testify/mock"
)

// MockCFClient is a mock implementation of the CF client for testing.
type MockCFClient struct {
	mock.Mock
}

// CreateServiceInstance mocks creating a service instance.
func (m *MockCFClient) CreateServiceInstance(ctx context.Context, params *cfclient.ServiceInstanceParams) (*cfclient.CreateServiceInstanceResult, error) {
	args := m.Called(ctx, params)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*cfclient.CreateServiceInstanceResult), args.Error(1)
}

// DeleteServiceInstance mocks deleting a service instance.
func (m *MockCFClient) DeleteServiceInstance(ctx context.Context, guid string) (*cfclient.DeleteServiceInstanceResult, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*cfclient.DeleteServiceInstanceResult), args.Error(1)
}

// UpdateServiceInstance mocks updating a service instance.
func (m *MockCFClient) UpdateServiceInstance(ctx context.Context, guid string, params *cfclient.UpdateServiceInstanceParams) (*cfclient.UpdateServiceInstanceResult, error) {
	args := m.Called(ctx, guid, params)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*cfclient.UpdateServiceInstanceResult), args.Error(1)
}

// GetServiceInstance mocks getting a service instance.
func (m *MockCFClient) GetServiceInstance(ctx context.Context, guid string) (*resource.ServiceInstance, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.ServiceInstance), args.Error(1)
}

// GetServiceInstanceByName mocks getting a service instance by name.
func (m *MockCFClient) GetServiceInstanceByName(ctx context.Context, name string) (*resource.ServiceInstance, error) {
	args := m.Called(ctx, name)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.ServiceInstance), args.Error(1)
}

// GetServiceInstanceByNameInSpace mocks getting a service instance by name in a space.
func (m *MockCFClient) GetServiceInstanceByNameInSpace(ctx context.Context, name, spaceGUID string) (*resource.ServiceInstance, error) {
	args := m.Called(ctx, name, spaceGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.ServiceInstance), args.Error(1)
}

// ListServiceInstancesByLabel mocks listing service instances by label.
func (m *MockCFClient) ListServiceInstancesByLabel(ctx context.Context, spaceGUID, labelSelector string) ([]*resource.ServiceInstance, error) {
	args := m.Called(ctx, spaceGUID, labelSelector)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]*resource.ServiceInstance), args.Error(1)
}

// GetServiceInstanceParameters mocks getting service instance parameters.
func (m *MockCFClient) GetServiceInstanceParameters(ctx context.Context, guid string) (map[string]interface{}, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(map[string]interface{}), args.Error(1)
}

// CreateServiceCredentialBinding mocks creating a service binding.
func (m *MockCFClient) CreateServiceCredentialBinding(ctx context.Context, params *cfclient.ServiceBindingParams) (*cfclient.CreateServiceBindingResult, error) {
	args := m.Called(ctx, params)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*cfclient.CreateServiceBindingResult), args.Error(1)
}

// DeleteServiceCredentialBinding mocks deleting a service binding.
func (m *MockCFClient) DeleteServiceCredentialBinding(ctx context.Context, guid string) (*cfclient.DeleteServiceBindingResult, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*cfclient.DeleteServiceBindingResult), args.Error(1)
}

// GetServiceCredentialBinding mocks getting a service binding.
func (m *MockCFClient) GetServiceCredentialBinding(ctx context.Context, guid string) (*resource.ServiceCredentialBinding, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.ServiceCredentialBinding), args.Error(1)
}

// GetServiceCredentialBindingByName mocks getting a service binding by name.
func (m *MockCFClient) GetServiceCredentialBindingByName(ctx context.Context, name, serviceInstanceGUID string) (*resource.ServiceCredentialBinding, error) {
	args := m.Called(ctx, name, serviceInstanceGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.ServiceCredentialBinding), args.Error(1)
}

// GetServiceBindingDetails mocks getting binding credentials.
func (m *MockCFClient) GetServiceBindingDetails(ctx context.Context, guid string) (map[string]interface{}, error) {
	args := m.Called(ctx, guid)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(map[string]interface{}), args.Error(1)
}

// ListServiceBindingsForInstance mocks listing bindings for an instance.
func (m *MockCFClient) ListServiceBindingsForInstance(ctx context.Context, serviceInstanceGUID string) ([]*resource.ServiceCredentialBinding, error) {
	args := m.Called(ctx, serviceInstanceGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]*resource.ServiceCredentialBinding), args.Error(1)
}

// DeleteAllServiceBindings mocks deleting all bindings.
func (m *MockCFClient) DeleteAllServiceBindings(ctx context.Context, serviceInstanceGUID string) error {
	args := m.Called(ctx, serviceInstanceGUID)
	return args.Error(0)
}

// GetBindingLastOperation mocks getting binding job status.
func (m *MockCFClient) GetBindingLastOperation(ctx context.Context, jobGUID string) (*resource.Job, error) {
	args := m.Called(ctx, jobGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.Job), args.Error(1)
}

// GetOrCreateSpace mocks getting or creating a space.
func (m *MockCFClient) GetOrCreateSpace(ctx context.Context, spaceName, orgGUID string) (*resource.Space, error) {
	args := m.Called(ctx, spaceName, orgGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*resource.Space), args.Error(1)
}

// GetDefaultOrgGUID mocks getting the default org GUID.
func (m *MockCFClient) GetDefaultOrgGUID(ctx context.Context) (string, error) {
	args := m.Called(ctx)
	return args.String(0), args.Error(1)
}

// GetDefaultSpaceGUID mocks getting the default space GUID.
func (m *MockCFClient) GetDefaultSpaceGUID(ctx context.Context) (string, error) {
	args := m.Called(ctx)
	return args.String(0), args.Error(1)
}

// WaitForJob mocks waiting for a job.
func (m *MockCFClient) WaitForJob(ctx context.Context, jobGUID string, timeout time.Duration) error {
	args := m.Called(ctx, jobGUID, timeout)
	return args.Error(0)
}

// ListServiceOfferings mocks listing service offerings.
func (m *MockCFClient) ListServiceOfferings(ctx context.Context) ([]*resource.ServiceOffering, error) {
	args := m.Called(ctx)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]*resource.ServiceOffering), args.Error(1)
}

// ListServicePlans mocks listing service plans.
func (m *MockCFClient) ListServicePlans(ctx context.Context, serviceOfferingGUID string) ([]*resource.ServicePlan, error) {
	args := m.Called(ctx, serviceOfferingGUID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]*resource.ServicePlan), args.Error(1)
}
