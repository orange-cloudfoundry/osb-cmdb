package cfclient

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/cloudfoundry/go-cfclient/v3/client"
	"github.com/cloudfoundry/go-cfclient/v3/resource"
)

// CreateServiceInstanceResult holds the result of creating a service instance.
type CreateServiceInstanceResult struct {
	ServiceInstance *resource.ServiceInstance
	JobGUID         string
	IsAsync         bool
}

// CreateServiceInstance creates a managed service instance in CF.
// If the operation is async, JobGUID will be set and IsAsync will be true.
func (c *Client) CreateServiceInstance(ctx context.Context, params *ServiceInstanceParams) (*CreateServiceInstanceResult, error) {
	if params == nil {
		return nil, fmt.Errorf("params is required")
	}

	c.logger.Debug("creating service instance",
		"name", params.Name,
		"plan_guid", params.ServicePlanGUID,
		"space_guid", params.SpaceGUID)

	// Use the constructor
	req := resource.NewServiceInstanceCreateManaged(params.Name, params.SpaceGUID, params.ServicePlanGUID)

	if params.Parameters != nil {
		paramsJSON, err := json.Marshal(params.Parameters)
		if err != nil {
			return nil, fmt.Errorf("marshaling parameters: %w", err)
		}
		req.WithParameters(paramsJSON)
	}
	if params.Tags != nil {
		req.WithTags(params.Tags)
	}
	if params.Labels != nil || params.Annotations != nil {
		req.Metadata = &resource.Metadata{
			Labels:      params.Labels,
			Annotations: params.Annotations,
		}
	}

	// CreateManaged returns a jobGUID for async operation
	jobGUID, err := c.cf.ServiceInstances.CreateManaged(ctx, req)
	if err != nil {
		return nil, fmt.Errorf("creating service instance: %w", err)
	}

	result := &CreateServiceInstanceResult{
		JobGUID: jobGUID,
		IsAsync: jobGUID != "",
	}

	c.logger.Debug("service instance create initiated",
		"job_guid", jobGUID,
		"async", result.IsAsync)

	return result, nil
}

// DeleteServiceInstanceResult holds the result of deleting a service instance.
type DeleteServiceInstanceResult struct {
	JobGUID string
	IsAsync bool
}

// DeleteServiceInstance deletes a service instance.
// If the operation is async, JobGUID will be set and IsAsync will be true.
func (c *Client) DeleteServiceInstance(ctx context.Context, guid string) (*DeleteServiceInstanceResult, error) {
	c.logger.Debug("deleting service instance", "guid", guid)

	jobGUID, err := c.cf.ServiceInstances.Delete(ctx, guid)
	if err != nil {
		return nil, fmt.Errorf("deleting service instance: %w", err)
	}

	result := &DeleteServiceInstanceResult{
		JobGUID: jobGUID,
		IsAsync: jobGUID != "",
	}

	c.logger.Debug("service instance delete initiated",
		"guid", guid,
		"job_guid", jobGUID,
		"async", result.IsAsync)

	return result, nil
}

// UpdateServiceInstanceParams holds parameters for updating a service instance.
type UpdateServiceInstanceParams struct {
	Name            *string
	ServicePlanGUID *string
	Parameters      map[string]interface{}
	Tags            []string
	Labels          map[string]*string
	Annotations     map[string]*string
	MaintenanceInfo *resource.ServiceInstanceMaintenanceInfo
}

// UpdateServiceInstanceResult holds the result of updating a service instance.
type UpdateServiceInstanceResult struct {
	ServiceInstance *resource.ServiceInstance
	JobGUID         string
	IsAsync         bool
}

// UpdateServiceInstance updates a service instance.
func (c *Client) UpdateServiceInstance(ctx context.Context, guid string, params *UpdateServiceInstanceParams) (*UpdateServiceInstanceResult, error) {
	if params == nil {
		return nil, fmt.Errorf("params is required")
	}

	c.logger.Debug("updating service instance", "guid", guid)

	req := resource.NewServiceInstanceManagedUpdate()

	if params.Name != nil {
		req.WithName(*params.Name)
	}
	if params.ServicePlanGUID != nil {
		req.WithServicePlan(*params.ServicePlanGUID)
	}
	if params.Parameters != nil {
		paramsJSON, err := json.Marshal(params.Parameters)
		if err != nil {
			return nil, fmt.Errorf("marshaling parameters: %w", err)
		}
		req.WithParameters(paramsJSON)
	}
	if params.Tags != nil {
		req.WithTags(params.Tags)
	}
	if params.Labels != nil || params.Annotations != nil {
		req.Metadata = &resource.Metadata{
			Labels:      params.Labels,
			Annotations: params.Annotations,
		}
	}
	if params.MaintenanceInfo != nil {
		req.WithMaintenanceInfo(params.MaintenanceInfo.Version, params.MaintenanceInfo.Description)
	}

	jobGUID, si, err := c.cf.ServiceInstances.UpdateManaged(ctx, guid, req)
	if err != nil {
		return nil, fmt.Errorf("updating service instance: %w", err)
	}

	result := &UpdateServiceInstanceResult{
		ServiceInstance: si,
		JobGUID:         jobGUID,
		IsAsync:         jobGUID != "",
	}

	c.logger.Debug("service instance update initiated",
		"guid", guid,
		"job_guid", jobGUID,
		"async", result.IsAsync)

	return result, nil
}

// GetServiceInstance retrieves a service instance by GUID.
func (c *Client) GetServiceInstance(ctx context.Context, guid string) (*resource.ServiceInstance, error) {
	c.logger.Debug("getting service instance", "guid", guid)
	return c.cf.ServiceInstances.Get(ctx, guid)
}

// GetServiceInstanceByName retrieves a service instance by name in the default space.
func (c *Client) GetServiceInstanceByName(ctx context.Context, name string) (*resource.ServiceInstance, error) {
	spaceGUID, err := c.GetDefaultSpaceGUID(ctx)
	if err != nil {
		return nil, err
	}

	return c.GetServiceInstanceByNameInSpace(ctx, name, spaceGUID)
}

// GetServiceInstanceByNameInSpace retrieves a service instance by name in a specific space.
func (c *Client) GetServiceInstanceByNameInSpace(ctx context.Context, name, spaceGUID string) (*resource.ServiceInstance, error) {
	c.logger.Debug("getting service instance by name", "name", name, "space_guid", spaceGUID)

	instances, _, err := c.cf.ServiceInstances.List(ctx, &client.ServiceInstanceListOptions{
		Names:      client.Filter{Values: []string{name}},
		SpaceGUIDs: client.Filter{Values: []string{spaceGUID}},
	})
	if err != nil {
		return nil, fmt.Errorf("listing service instances: %w", err)
	}

	if len(instances) == 0 {
		return nil, nil // Not found, but not an error
	}

	return instances[0], nil
}

// ListServiceInstancesByLabel lists service instances matching a label selector.
// labelKey and labelValue are used to find instances with matching labels.
func (c *Client) ListServiceInstancesByLabel(ctx context.Context, spaceGUID, labelKey, labelValue string) ([]*resource.ServiceInstance, error) {
	c.logger.Debug("listing service instances by label",
		"space_guid", spaceGUID,
		"label_key", labelKey,
		"label_value", labelValue)

	opts := client.NewServiceInstanceListOptions()
	opts.SpaceGUIDs = client.Filter{Values: []string{spaceGUID}}
	opts.LabelSel = make(client.LabelSelector)
	opts.LabelSel.EqualTo(labelKey, labelValue)

	instances, _, err := c.cf.ServiceInstances.List(ctx, opts)
	return instances, err
}

// GetServiceInstanceParameters retrieves the parameters for a service instance.
func (c *Client) GetServiceInstanceParameters(ctx context.Context, guid string) (map[string]interface{}, error) {
	c.logger.Debug("getting service instance parameters", "guid", guid)
	rawParams, err := c.cf.ServiceInstances.GetManagedParameters(ctx, guid)
	if err != nil {
		return nil, err
	}
	if rawParams == nil {
		return nil, nil
	}

	var params map[string]interface{}
	if err := json.Unmarshal(*rawParams, &params); err != nil {
		return nil, fmt.Errorf("unmarshaling parameters: %w", err)
	}
	return params, nil
}

// GetLastOperation retrieves the last operation for a service instance.
func (c *Client) GetLastOperation(ctx context.Context, guid string) (resource.LastOperation, error) {
	si, err := c.cf.ServiceInstances.Get(ctx, guid)
	if err != nil {
		return resource.LastOperation{}, fmt.Errorf("getting service instance: %w", err)
	}
	return si.LastOperation, nil
}

// WaitForServiceInstanceOperation waits for a service instance operation to complete.
func (c *Client) WaitForServiceInstanceOperation(ctx context.Context, jobGUID string, timeout time.Duration) error {
	return c.WaitForJob(ctx, jobGUID, timeout)
}
