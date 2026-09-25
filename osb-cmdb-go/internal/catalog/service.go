// Package catalog provides dynamic service catalog management.
package catalog

import (
	"context"

	"github.com/pivotal-cf/brokerapi/v11/domain"
)

// Service provides access to the service catalog.
type Service interface {
	// GetServices returns all services in the catalog.
	GetServices() []domain.Service

	// GetService returns a service by ID, or nil if not found.
	GetService(serviceID string) *domain.Service

	// GetPlan returns a service plan by ID, or nil if not found.
	GetPlan(planID string) *domain.ServicePlan

	// GetPlanForService returns a plan for a specific service, or nil if not found.
	GetPlanForService(serviceID, planID string) *domain.ServicePlan

	// Refresh fetches the latest catalog from the backing CF.
	Refresh(ctx context.Context) error

	// Start begins periodic catalog refresh.
	Start(ctx context.Context)

	// Stop stops periodic catalog refresh.
	Stop()
}

// Config holds catalog service configuration.
type Config struct {
	// ServiceSuffix is appended to all service names.
	ServiceSuffix string

	// ExcludedServices is a list of glob patterns for services to exclude.
	ExcludedServices []string

	// RefreshInterval is how often to refresh the catalog.
	// Set to 0 to disable periodic refresh.
	RefreshInterval string
}
