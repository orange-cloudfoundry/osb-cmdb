package catalog

import (
	"context"
	"log/slog"
	"sync"
	"time"

	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/cfclient"
	"github.com/pivotal-cf/brokerapi/v11/domain"
)

// DynamicCatalog fetches and caches services from the CF marketplace.
type DynamicCatalog struct {
	cfClient *cfclient.Client
	config   *Config
	logger   *slog.Logger

	// Thread-safe catalog storage
	mu       sync.RWMutex
	services []domain.Service

	// Indexes for fast lookup
	serviceIndex map[string]*domain.Service
	planIndex    map[string]*domain.ServicePlan

	// Refresh control
	stopCh chan struct{}
	wg     sync.WaitGroup
}

// NewDynamicCatalog creates a new dynamic catalog service.
func NewDynamicCatalog(cfClient *cfclient.Client, config *Config) *DynamicCatalog {
	return &DynamicCatalog{
		cfClient:     cfClient,
		config:       config,
		logger:       slog.Default().With("component", "catalog"),
		services:     []domain.Service{},
		serviceIndex: make(map[string]*domain.Service),
		planIndex:    make(map[string]*domain.ServicePlan),
		stopCh:       make(chan struct{}),
	}
}

// GetServices returns all services in the catalog.
func (c *DynamicCatalog) GetServices() []domain.Service {
	c.mu.RLock()
	defer c.mu.RUnlock()

	// Return a copy to prevent external modification
	result := make([]domain.Service, len(c.services))
	copy(result, c.services)
	return result
}

// GetService returns a service by ID, or nil if not found.
func (c *DynamicCatalog) GetService(serviceID string) *domain.Service {
	c.mu.RLock()
	defer c.mu.RUnlock()

	if svc, ok := c.serviceIndex[serviceID]; ok {
		// Return a copy
		copy := *svc
		return &copy
	}
	return nil
}

// GetPlan returns a service plan by ID, or nil if not found.
func (c *DynamicCatalog) GetPlan(planID string) *domain.ServicePlan {
	c.mu.RLock()
	defer c.mu.RUnlock()

	if plan, ok := c.planIndex[planID]; ok {
		// Return a copy
		copy := *plan
		return &copy
	}
	return nil
}

// GetPlanForService returns a plan for a specific service, or nil if not found.
func (c *DynamicCatalog) GetPlanForService(serviceID, planID string) *domain.ServicePlan {
	c.mu.RLock()
	defer c.mu.RUnlock()

	svc, ok := c.serviceIndex[serviceID]
	if !ok {
		return nil
	}

	for i := range svc.Plans {
		if svc.Plans[i].ID == planID {
			// Return a copy
			copy := svc.Plans[i]
			return &copy
		}
	}
	return nil
}

// Refresh fetches the latest catalog from the backing CF.
func (c *DynamicCatalog) Refresh(ctx context.Context) error {
	c.logger.Debug("refreshing catalog")

	mapper := NewMapper(c.config)

	// Fetch service offerings from CF
	offerings, err := c.cfClient.ListServiceOfferings(ctx)
	if err != nil {
		c.logger.Error("failed to list service offerings", "error", err)
		return err
	}

	var services []domain.Service
	serviceIndex := make(map[string]*domain.Service)
	planIndex := make(map[string]*domain.ServicePlan)

	for _, offering := range offerings {
		// Check exclusion
		if mapper.IsExcluded(offering.Name) {
			c.logger.Debug("excluding service", "name", offering.Name)
			continue
		}

		// Fetch plans for this service
		plans, err := c.cfClient.ListServicePlans(ctx, offering.GUID)
		if err != nil {
			c.logger.Warn("failed to list plans for service",
				"service", offering.Name,
				"guid", offering.GUID,
				"error", err)
			continue
		}

		// Map to OSB format
		svc := mapper.MapService(offering, plans)
		services = append(services, svc)

		// Build indexes
		serviceIndex[svc.ID] = &services[len(services)-1]
		for i := range svc.Plans {
			planIndex[svc.Plans[i].ID] = &services[len(services)-1].Plans[i]
		}
	}

	// Update catalog atomically
	c.mu.Lock()
	c.services = services
	c.serviceIndex = serviceIndex
	c.planIndex = planIndex
	c.mu.Unlock()

	c.logger.Info("catalog refreshed", "services", len(services))
	return nil
}

// Start begins periodic catalog refresh.
func (c *DynamicCatalog) Start(ctx context.Context) {
	if c.config.RefreshInterval == "" || c.config.RefreshInterval == "0" {
		c.logger.Info("periodic catalog refresh disabled")
		return
	}

	interval, err := time.ParseDuration(c.config.RefreshInterval)
	if err != nil {
		c.logger.Warn("invalid refresh interval, using default",
			"configured", c.config.RefreshInterval,
			"default", "5m")
		interval = 5 * time.Minute
	}

	c.logger.Info("starting periodic catalog refresh", "interval", interval)

	c.wg.Add(1)
	go func() {
		defer c.wg.Done()

		ticker := time.NewTicker(interval)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				if err := c.Refresh(ctx); err != nil {
					c.logger.Error("periodic refresh failed", "error", err)
					// Continue with existing catalog on error
				}
			case <-c.stopCh:
				c.logger.Info("stopping periodic catalog refresh")
				return
			case <-ctx.Done():
				c.logger.Info("context cancelled, stopping refresh")
				return
			}
		}
	}()
}

// Stop stops periodic catalog refresh.
func (c *DynamicCatalog) Stop() {
	close(c.stopCh)
	c.wg.Wait()
}
