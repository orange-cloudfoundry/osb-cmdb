package catalog

import (
	"context"
	"testing"

	"github.com/pivotal-cf/brokerapi/v11/domain"
	"github.com/stretchr/testify/assert"
)

func TestMapper_IsExcluded(t *testing.T) {
	tests := []struct {
		name            string
		excludePatterns []string
		serviceName     string
		want            bool
	}{
		{
			name:            "no exclusions",
			excludePatterns: nil,
			serviceName:     "my-service",
			want:            false,
		},
		{
			name:            "exact match",
			excludePatterns: []string{"my-service"},
			serviceName:     "my-service",
			want:            true,
		},
		{
			name:            "no match",
			excludePatterns: []string{"other-service"},
			serviceName:     "my-service",
			want:            false,
		},
		{
			name:            "glob pattern match",
			excludePatterns: []string{"test-*"},
			serviceName:     "test-broker",
			want:            true,
		},
		{
			name:            "glob pattern no match",
			excludePatterns: []string{"test-*"},
			serviceName:     "prod-broker",
			want:            false,
		},
		{
			name:            "multiple patterns",
			excludePatterns: []string{"test-*", "dev-*"},
			serviceName:     "dev-service",
			want:            true,
		},
		{
			name:            "prefix pattern",
			excludePatterns: []string{"internal*"},
			serviceName:     "internal-service",
			want:            true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mapper := NewMapper(&Config{ExcludedServices: tt.excludePatterns})
			got := mapper.IsExcluded(tt.serviceName)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestDynamicCatalog_StartStopIsIdempotent(t *testing.T) {
	c := NewDynamicCatalog(nil, &Config{RefreshInterval: "1h"})
	c.Start(context.Background())
	c.Start(context.Background())
	c.Stop()
	c.Stop()

	// A stopped catalog can be started and stopped again.
	c.Start(context.Background())
	c.Stop()
}

func TestMapper_ApplySuffix(t *testing.T) {
	tests := []struct {
		name   string
		suffix string
		input  string
		want   string
	}{
		{
			name:   "no suffix",
			suffix: "",
			input:  "my-service",
			want:   "my-service",
		},
		{
			name:   "with suffix",
			suffix: "-cmdb",
			input:  "my-service",
			want:   "my-service-cmdb",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mapper := NewMapper(&Config{ServiceSuffix: tt.suffix})
			got := mapper.ApplySuffix(tt.input)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestMapper_RemoveSuffix(t *testing.T) {
	tests := []struct {
		name   string
		suffix string
		input  string
		want   string
	}{
		{
			name:   "no suffix configured",
			suffix: "",
			input:  "my-service",
			want:   "my-service",
		},
		{
			name:   "remove suffix",
			suffix: "-cmdb",
			input:  "my-service-cmdb",
			want:   "my-service",
		},
		{
			name:   "no suffix present",
			suffix: "-cmdb",
			input:  "my-service",
			want:   "my-service",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mapper := NewMapper(&Config{ServiceSuffix: tt.suffix})
			got := mapper.RemoveSuffix(tt.input)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestDynamicCatalog_GetService(t *testing.T) {
	// Create catalog without CF client for unit tests
	catalog := &DynamicCatalog{
		serviceIndex: make(map[string]*domain.Service),
		planIndex:    make(map[string]*domain.ServicePlan),
	}

	// Add a service directly to the index
	svc := &domain.Service{
		ID:          "svc-123",
		Name:        "test-service",
		Description: "A test service",
	}
	catalog.serviceIndex["svc-123"] = svc

	// Test GetService
	found := catalog.GetService("svc-123")
	assert.NotNil(t, found)
	assert.Equal(t, "svc-123", found.ID)
	assert.Equal(t, "test-service", found.Name)

	// Test not found
	notFound := catalog.GetService("nonexistent")
	assert.Nil(t, notFound)
}

func TestDynamicCatalog_GetPlan(t *testing.T) {
	// Create catalog without CF client for unit tests
	catalog := &DynamicCatalog{
		serviceIndex: make(map[string]*domain.Service),
		planIndex:    make(map[string]*domain.ServicePlan),
	}

	// Add a plan directly to the index
	plan := &domain.ServicePlan{
		ID:          "plan-456",
		Name:        "small",
		Description: "Small plan",
	}
	catalog.planIndex["plan-456"] = plan

	// Test GetPlan
	found := catalog.GetPlan("plan-456")
	assert.NotNil(t, found)
	assert.Equal(t, "plan-456", found.ID)
	assert.Equal(t, "small", found.Name)

	// Test not found
	notFound := catalog.GetPlan("nonexistent")
	assert.Nil(t, notFound)
}

func TestDynamicCatalog_GetPlanForService(t *testing.T) {
	catalog := &DynamicCatalog{
		serviceIndex: make(map[string]*domain.Service),
		planIndex:    make(map[string]*domain.ServicePlan),
	}

	// Add a service with plans
	svc := &domain.Service{
		ID:   "svc-123",
		Name: "test-service",
		Plans: []domain.ServicePlan{
			{ID: "plan-1", Name: "small"},
			{ID: "plan-2", Name: "large"},
		},
	}
	catalog.serviceIndex["svc-123"] = svc

	// Test GetPlanForService
	found := catalog.GetPlanForService("svc-123", "plan-1")
	assert.NotNil(t, found)
	assert.Equal(t, "plan-1", found.ID)
	assert.Equal(t, "small", found.Name)

	// Test plan not in service
	notFound := catalog.GetPlanForService("svc-123", "plan-99")
	assert.Nil(t, notFound)

	// Test service not found
	noService := catalog.GetPlanForService("nonexistent", "plan-1")
	assert.Nil(t, noService)
}

func TestNewMapper(t *testing.T) {
	// Test with nil config
	mapper := NewMapper(nil)
	assert.NotNil(t, mapper)
	assert.NotNil(t, mapper.config)

	// Test with config
	config := &Config{
		ServiceSuffix:    "-cmdb",
		ExcludedServices: []string{"test-*"},
	}
	mapper = NewMapper(config)
	assert.NotNil(t, mapper)
	assert.Equal(t, "-cmdb", mapper.config.ServiceSuffix)
	assert.Equal(t, []string{"test-*"}, mapper.config.ExcludedServices)
}
