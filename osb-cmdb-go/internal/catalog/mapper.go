package catalog

import (
	"encoding/json"
	"path/filepath"
	"strings"

	"github.com/cloudfoundry/go-cfclient/v3/resource"
	"github.com/pivotal-cf/brokerapi/v11/domain"
)

// Mapper converts CF resources to OSB format.
type Mapper struct {
	config *Config
}

// NewMapper creates a new catalog mapper.
func NewMapper(config *Config) *Mapper {
	if config == nil {
		config = &Config{}
	}
	return &Mapper{config: config}
}

// MapService converts a CF service offering to an OSB service definition.
func (m *Mapper) MapService(offering *resource.ServiceOffering, plans []*resource.ServicePlan) domain.Service {
	// Apply suffix to service name if configured
	name := offering.Name
	if m.config.ServiceSuffix != "" {
		name = name + m.config.ServiceSuffix
	}

	// Parse broker catalog metadata
	metadata := &domain.ServiceMetadata{}
	if offering.BrokerCatalog.Metadata != nil {
		var rawMetadata map[string]interface{}
		if err := json.Unmarshal(*offering.BrokerCatalog.Metadata, &rawMetadata); err == nil {
			if v, ok := rawMetadata["displayName"].(string); ok {
				metadata.DisplayName = v
			}
			if v, ok := rawMetadata["imageUrl"].(string); ok {
				metadata.ImageUrl = v
			}
			if v, ok := rawMetadata["longDescription"].(string); ok {
				metadata.LongDescription = v
			}
			if v, ok := rawMetadata["providerDisplayName"].(string); ok {
				metadata.ProviderDisplayName = v
			}
			if v, ok := rawMetadata["documentationUrl"].(string); ok {
				metadata.DocumentationUrl = v
			}
			if v, ok := rawMetadata["supportUrl"].(string); ok {
				metadata.SupportUrl = v
			}
			if v, ok := rawMetadata["shareable"].(bool); ok {
				metadata.Shareable = &v
			}
			metadata.AdditionalMetadata = rawMetadata
		}
	}

	// Map plans
	var osbPlans []domain.ServicePlan
	for _, plan := range plans {
		osbPlans = append(osbPlans, m.MapPlan(plan))
	}

	// Get features from broker catalog
	features := offering.BrokerCatalog.Features

	return domain.Service{
		ID:                   offering.GUID,
		Name:                 name,
		Description:          offering.Description,
		Bindable:             features.Bindable,
		Tags:                 offering.Tags,
		Metadata:             metadata,
		Plans:                osbPlans,
		PlanUpdatable:        features.PlanUpdateable,
		InstancesRetrievable: features.InstancesRetrievable,
		BindingsRetrievable:  features.BindingsRetrievable,
		AllowContextUpdates:  features.AllowContextUpdates,
	}
}

// MapPlan converts a CF service plan to an OSB service plan.
func (m *Mapper) MapPlan(plan *resource.ServicePlan) domain.ServicePlan {
	// Parse broker catalog metadata
	var metadata *domain.ServicePlanMetadata
	if plan.BrokerCatalog.Metadata != nil {
		var rawMetadata map[string]interface{}
		if err := json.Unmarshal(*plan.BrokerCatalog.Metadata, &rawMetadata); err == nil {
			metadata = &domain.ServicePlanMetadata{
				AdditionalMetadata: rawMetadata,
			}
			if v, ok := rawMetadata["displayName"].(string); ok {
				metadata.DisplayName = v
			}
			if bullets, ok := rawMetadata["bullets"].([]interface{}); ok {
				for _, b := range bullets {
					if s, ok := b.(string); ok {
						metadata.Bullets = append(metadata.Bullets, s)
					}
				}
			}
		}
	}

	// Map schemas - convert *json.RawMessage to map[string]interface{}
	var schemas *domain.ServiceSchemas
	if plan.Schemas.ServiceInstance.Create.Parameters != nil ||
		plan.Schemas.ServiceInstance.Update.Parameters != nil ||
		plan.Schemas.ServiceBinding.Create.Parameters != nil {
		schemas = &domain.ServiceSchemas{}

		if plan.Schemas.ServiceInstance.Create.Parameters != nil {
			var params map[string]interface{}
			if err := json.Unmarshal(*plan.Schemas.ServiceInstance.Create.Parameters, &params); err == nil {
				schemas.Instance.Create = domain.Schema{Parameters: params}
			}
		}
		if plan.Schemas.ServiceInstance.Update.Parameters != nil {
			var params map[string]interface{}
			if err := json.Unmarshal(*plan.Schemas.ServiceInstance.Update.Parameters, &params); err == nil {
				schemas.Instance.Update = domain.Schema{Parameters: params}
			}
		}
		if plan.Schemas.ServiceBinding.Create.Parameters != nil {
			var params map[string]interface{}
			if err := json.Unmarshal(*plan.Schemas.ServiceBinding.Create.Parameters, &params); err == nil {
				schemas.Binding.Create = domain.Schema{Parameters: params}
			}
		}
	}

	// Map maintenance info
	var maintenanceInfo *domain.MaintenanceInfo
	if plan.MaintenanceInfo.Version != "" {
		maintenanceInfo = &domain.MaintenanceInfo{
			Version:     plan.MaintenanceInfo.Version,
			Description: plan.MaintenanceInfo.Description,
		}
	}

	// Determine bindable from broker catalog features
	bindable := plan.BrokerCatalog.Features.Bindable

	return domain.ServicePlan{
		ID:              plan.GUID,
		Name:            plan.Name,
		Description:     plan.Description,
		Free:            &plan.Free,
		Bindable:        &bindable,
		Metadata:        metadata,
		Schemas:         schemas,
		MaintenanceInfo: maintenanceInfo,
	}
}

// IsExcluded checks if a service name matches any exclusion pattern.
func (m *Mapper) IsExcluded(serviceName string) bool {
	for _, pattern := range m.config.ExcludedServices {
		// Support glob patterns
		matched, err := filepath.Match(pattern, serviceName)
		if err != nil {
			// Invalid pattern, skip
			continue
		}
		if matched {
			return true
		}
		// Also support simple prefix matching for convenience
		if strings.HasSuffix(pattern, "*") {
			prefix := strings.TrimSuffix(pattern, "*")
			if strings.HasPrefix(serviceName, prefix) {
				return true
			}
		}
	}
	return false
}

// ApplySuffix adds the configured suffix to a service name.
func (m *Mapper) ApplySuffix(name string) string {
	if m.config.ServiceSuffix != "" {
		return name + m.config.ServiceSuffix
	}
	return name
}

// RemoveSuffix removes the configured suffix from a service name.
func (m *Mapper) RemoveSuffix(name string) string {
	if m.config.ServiceSuffix != "" && strings.HasSuffix(name, m.config.ServiceSuffix) {
		return strings.TrimSuffix(name, m.config.ServiceSuffix)
	}
	return name
}
