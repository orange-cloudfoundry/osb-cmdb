package cfclient

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/cloudfoundry/go-cfclient/v3/client"
	"github.com/cloudfoundry/go-cfclient/v3/resource"
)

// CreateServiceBindingResult holds the result of creating a service binding.
type CreateServiceBindingResult struct {
	ServiceBinding *resource.ServiceCredentialBinding
	JobGUID        string
	IsAsync        bool
}

// CreateServiceCredentialBinding creates a service key (credential binding) for a service instance.
func (c *Client) CreateServiceCredentialBinding(ctx context.Context, params *ServiceBindingParams) (*CreateServiceBindingResult, error) {
	if params == nil {
		return nil, fmt.Errorf("params is required")
	}

	c.logger.Debug("creating service credential binding",
		"name", params.Name,
		"service_instance_guid", params.ServiceInstanceGUID)

	// Use the constructor for creating a service key binding
	req := resource.NewServiceCredentialBindingCreateKey(params.ServiceInstanceGUID, params.Name)

	if params.Parameters != nil {
		paramsJSON, err := json.Marshal(params.Parameters)
		if err != nil {
			return nil, fmt.Errorf("marshaling parameters: %w", err)
		}
		req.WithJSONParameters(string(paramsJSON))
	}

	jobGUID, binding, err := c.cf.ServiceCredentialBindings.Create(ctx, req)
	if err != nil {
		return nil, fmt.Errorf("creating service credential binding: %w", err)
	}

	result := &CreateServiceBindingResult{
		ServiceBinding: binding,
		JobGUID:        jobGUID,
		IsAsync:        jobGUID != "",
	}

	var bindingGUID string
	if binding != nil {
		bindingGUID = binding.GUID
	}

	c.logger.Debug("service credential binding create initiated",
		"guid", bindingGUID,
		"job_guid", jobGUID,
		"async", result.IsAsync)

	return result, nil
}

// DeleteServiceBindingResult holds the result of deleting a service binding.
type DeleteServiceBindingResult struct {
	JobGUID string
	IsAsync bool
}

// DeleteServiceCredentialBinding deletes a service credential binding (service key).
func (c *Client) DeleteServiceCredentialBinding(ctx context.Context, guid string) (*DeleteServiceBindingResult, error) {
	c.logger.Debug("deleting service credential binding", "guid", guid)

	jobGUID, err := c.cf.ServiceCredentialBindings.Delete(ctx, guid)
	if err != nil {
		return nil, fmt.Errorf("deleting service credential binding: %w", err)
	}

	result := &DeleteServiceBindingResult{
		JobGUID: jobGUID,
		IsAsync: jobGUID != "",
	}

	c.logger.Debug("service credential binding delete initiated",
		"guid", guid,
		"job_guid", jobGUID,
		"async", result.IsAsync)

	return result, nil
}

// GetServiceCredentialBinding retrieves a service credential binding by GUID.
func (c *Client) GetServiceCredentialBinding(ctx context.Context, guid string) (*resource.ServiceCredentialBinding, error) {
	c.logger.Debug("getting service credential binding", "guid", guid)
	return c.cf.ServiceCredentialBindings.Get(ctx, guid)
}

// GetServiceCredentialBindingByName retrieves a service binding by name for a service instance.
func (c *Client) GetServiceCredentialBindingByName(ctx context.Context, name, serviceInstanceGUID string) (*resource.ServiceCredentialBinding, error) {
	c.logger.Debug("getting service credential binding by name",
		"name", name,
		"service_instance_guid", serviceInstanceGUID)

	bindings, _, err := c.cf.ServiceCredentialBindings.List(ctx, &client.ServiceCredentialBindingListOptions{
		Names:                client.Filter{Values: []string{name}},
		ServiceInstanceGUIDs: client.Filter{Values: []string{serviceInstanceGUID}},
	})
	if err != nil {
		return nil, fmt.Errorf("listing service credential bindings: %w", err)
	}

	if len(bindings) == 0 {
		return nil, nil // Not found, but not an error
	}

	return bindings[0], nil
}

// GetServiceBindingDetails retrieves the credentials for a service binding.
func (c *Client) GetServiceBindingDetails(ctx context.Context, guid string) (map[string]interface{}, error) {
	c.logger.Debug("getting service binding details", "guid", guid)

	details, err := c.cf.ServiceCredentialBindings.GetDetails(ctx, guid)
	if err != nil {
		return nil, fmt.Errorf("getting service binding details: %w", err)
	}

	return details.Credentials, nil
}

// ListServiceBindingsForInstance lists all service bindings for a service instance.
func (c *Client) ListServiceBindingsForInstance(ctx context.Context, serviceInstanceGUID string) ([]*resource.ServiceCredentialBinding, error) {
	c.logger.Debug("listing service bindings for instance", "service_instance_guid", serviceInstanceGUID)

	bindings, _, err := c.cf.ServiceCredentialBindings.List(ctx, &client.ServiceCredentialBindingListOptions{
		ServiceInstanceGUIDs: client.Filter{Values: []string{serviceInstanceGUID}},
		Type:                 client.Filter{Values: []string{"key"}},
	})
	return bindings, err
}

// DeleteAllServiceBindings deletes all service bindings for a service instance.
// This is used during deprovision to clean up before deleting the instance.
func (c *Client) DeleteAllServiceBindings(ctx context.Context, serviceInstanceGUID string) error {
	c.logger.Debug("deleting all service bindings", "service_instance_guid", serviceInstanceGUID)

	bindings, err := c.ListServiceBindingsForInstance(ctx, serviceInstanceGUID)
	if err != nil {
		return fmt.Errorf("listing bindings: %w", err)
	}

	for _, binding := range bindings {
		result, err := c.DeleteServiceCredentialBinding(ctx, binding.GUID)
		if err != nil {
			return fmt.Errorf("deleting binding %s: %w", binding.GUID, err)
		}

		// If async, wait for completion
		if result.IsAsync && result.JobGUID != "" {
			if err := c.WaitForJob(ctx, result.JobGUID, 5*60); err != nil {
				return fmt.Errorf("waiting for binding deletion %s: %w", binding.GUID, err)
			}
		}
	}

	c.logger.Debug("all service bindings deleted", "count", len(bindings))
	return nil
}

// GetBindingLastOperation retrieves the last operation status from a job.
// This is used for async binding operations.
func (c *Client) GetBindingLastOperation(ctx context.Context, jobGUID string) (*resource.Job, error) {
	c.logger.Debug("getting binding job status", "job_guid", jobGUID)
	return c.cf.Jobs.Get(ctx, jobGUID)
}
