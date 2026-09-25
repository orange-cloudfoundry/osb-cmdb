// Package broker implements the OSB service broker interface.
package broker

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"reflect"

	"code.cloudfoundry.org/brokerapi/v13/domain"
	"code.cloudfoundry.org/brokerapi/v13/domain/apiresponses"
	"code.cloudfoundry.org/brokerapi/v13/middlewares"
	"github.com/cloudfoundry/go-cfclient/v3/resource"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/catalog"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/cfclient"
	osbcontext "github.com/orange-cloudfoundry/osb-cmdb-go/internal/context"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/errors"
	"github.com/orange-cloudfoundry/osb-cmdb-go/internal/metadata"
)

// Broker implements the domain.ServiceBroker interface.
type Broker struct {
	cfClient         *cfclient.Client
	catalog          catalog.Service
	metadataService  metadata.Service
	contextParser    *osbcontext.Parser
	defaultSpaceGUID string
	logger           *slog.Logger
}

// Config holds broker configuration.
type Config struct {
	CFClient         *cfclient.Client
	Catalog          catalog.Service
	MetadataService  metadata.Service
	ContextParser    *osbcontext.Parser
	DefaultSpaceGUID string
}

// New creates a new OSB broker.
func New(cfg *Config) (*Broker, error) {
	if cfg == nil {
		return nil, fmt.Errorf("config is required")
	}
	if cfg.CFClient == nil {
		return nil, fmt.Errorf("CF client is required")
	}
	if cfg.Catalog == nil {
		return nil, fmt.Errorf("catalog service is required")
	}

	return &Broker{
		cfClient:         cfg.CFClient,
		catalog:          cfg.Catalog,
		metadataService:  cfg.MetadataService,
		contextParser:    cfg.ContextParser,
		defaultSpaceGUID: cfg.DefaultSpaceGUID,
		logger:           slog.Default().With("component", "broker"),
	}, nil
}

// Services returns the catalog of services offered by the broker.
func (b *Broker) Services(ctx context.Context) ([]domain.Service, error) {
	b.logger.Debug("services requested")
	return b.catalog.GetServices(), nil
}

// Provision creates a new service instance.
func (b *Broker) Provision(ctx context.Context, instanceID string, details domain.ProvisionDetails, asyncAllowed bool) (domain.ProvisionedServiceSpec, error) {
	b.logger.Info("provisioning service instance",
		"instance_id", instanceID,
		"service_id", details.ServiceID,
		"plan_id", details.PlanID,
		"async_allowed", asyncAllowed)

	// Verify service and plan exist
	svc := b.catalog.GetService(details.ServiceID)
	if svc == nil {
		return domain.ProvisionedServiceSpec{}, apiresponses.NewFailureResponse(
			fmt.Errorf("service %s not found", details.ServiceID),
			400, "invalid-service-id")
	}
	plan := b.catalog.GetPlanForService(details.ServiceID, details.PlanID)
	if plan == nil {
		return domain.ProvisionedServiceSpec{}, apiresponses.NewFailureResponse(
			fmt.Errorf("plan %s not found", details.PlanID),
			400, "invalid-plan-id")
	}

	// Check for duplicate instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.ProvisionedServiceSpec{}, errors.RedactError(err)
	}

	if existing != nil {
		// Check if it's a compatible duplicate
		compatible, err := b.isCompatibleDuplicate(ctx, existing, details)
		if err != nil {
			return domain.ProvisionedServiceSpec{}, errors.RedactError(err)
		}
		if compatible {
			b.logger.Info("returning existing compatible instance", "instance_id", instanceID)
			return domain.ProvisionedServiceSpec{
				AlreadyExists: true,
			}, nil
		}
		return domain.ProvisionedServiceSpec{}, apiresponses.ErrInstanceAlreadyExists
	}

	// Build metadata from context
	labels, annotations := b.buildMetadata(ctx, instanceID, details.RawContext)

	// Parse parameters
	var params map[string]interface{}
	if len(details.RawParameters) > 0 {
		if err := json.Unmarshal(details.RawParameters, &params); err != nil {
			return domain.ProvisionedServiceSpec{}, apiresponses.NewFailureResponse(
				fmt.Errorf("invalid parameters: %w", err),
				400, "invalid-parameters")
		}
	}

	// Create service instance in CF
	result, err := b.cfClient.CreateServiceInstance(ctx, &cfclient.ServiceInstanceParams{
		Name:            instanceID, // Use OSB instance ID as CF instance name
		ServicePlanGUID: details.PlanID,
		SpaceGUID:       b.defaultSpaceGUID,
		Parameters:      params,
		Labels:          labels,
		Annotations:     annotations,
	})
	if err != nil {
		b.logger.Error("failed to create service instance", "error", err)
		return domain.ProvisionedServiceSpec{}, errors.RedactError(err)
	}

	// Handle async operation
	if result.IsAsync {
		if !asyncAllowed {
			return domain.ProvisionedServiceSpec{}, apiresponses.ErrAsyncRequired
		}

		return asyncProvisionSpec(instanceID, result.JobGUID), nil
	}

	// Sync completion
	var dashboardURL string
	if result.ServiceInstance.DashboardURL != nil {
		dashboardURL = *result.ServiceInstance.DashboardURL
	}

	return domain.ProvisionedServiceSpec{
		IsAsync:      false,
		DashboardURL: dashboardURL,
	}, nil
}

func asyncProvisionSpec(instanceID, jobGUID string) domain.ProvisionedServiceSpec {
	return domain.ProvisionedServiceSpec{
		IsAsync:       true,
		OperationData: NewProvisionOperation(instanceID, jobGUID).Encode(),
	}
}

// Deprovision deletes a service instance.
func (b *Broker) Deprovision(ctx context.Context, instanceID string, details domain.DeprovisionDetails, asyncAllowed bool) (domain.DeprovisionServiceSpec, error) {
	b.logger.Info("deprovisioning service instance",
		"instance_id", instanceID,
		"service_id", details.ServiceID,
		"plan_id", details.PlanID,
		"async_allowed", asyncAllowed)

	// Find the backing instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.DeprovisionServiceSpec{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.DeprovisionServiceSpec{}, apiresponses.ErrInstanceDoesNotExist
	}

	// Delete all service keys first
	if err := b.cfClient.DeleteAllServiceBindings(ctx, existing.GUID); err != nil {
		b.logger.Error("failed to delete service bindings", "error", err)
		return domain.DeprovisionServiceSpec{}, errors.RedactError(err)
	}

	// Delete the service instance
	result, err := b.cfClient.DeleteServiceInstance(ctx, existing.GUID)
	if err != nil {
		b.logger.Error("failed to delete service instance", "error", err)
		return domain.DeprovisionServiceSpec{}, errors.RedactError(err)
	}

	// Handle async operation
	if result.IsAsync {
		if !asyncAllowed {
			return domain.DeprovisionServiceSpec{}, apiresponses.ErrAsyncRequired
		}

		opState := &OperationState{
			Type:                       OperationDeprovision,
			BackingCFServiceInstanceID: existing.GUID,
			JobGUID:                    result.JobGUID,
		}

		return domain.DeprovisionServiceSpec{
			IsAsync:       true,
			OperationData: opState.Encode(),
		}, nil
	}

	return domain.DeprovisionServiceSpec{IsAsync: false}, nil
}

// GetInstance fetches a service instance.
func (b *Broker) GetInstance(ctx context.Context, instanceID string, details domain.FetchInstanceDetails) (domain.GetInstanceDetailsSpec, error) {
	b.logger.Debug("getting service instance", "instance_id", instanceID)

	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.GetInstanceDetailsSpec{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.GetInstanceDetailsSpec{}, apiresponses.ErrInstanceDoesNotExist
	}

	// Get parameters
	params, err := b.cfClient.GetServiceInstanceParameters(ctx, existing.GUID)
	if err != nil {
		b.logger.Warn("failed to get instance parameters", "error", err)
		// Continue without parameters
	}

	var dashboardURL string
	if existing.DashboardURL != nil {
		dashboardURL = *existing.DashboardURL
	}

	return domain.GetInstanceDetailsSpec{
		ServiceID:    details.ServiceID,
		PlanID:       details.PlanID,
		DashboardURL: dashboardURL,
		Parameters:   params,
	}, nil
}

// Update modifies an existing service instance.
func (b *Broker) Update(ctx context.Context, instanceID string, details domain.UpdateDetails, asyncAllowed bool) (domain.UpdateServiceSpec, error) {
	b.logger.Info("updating service instance",
		"instance_id", instanceID,
		"service_id", details.ServiceID,
		"plan_id", details.PlanID,
		"async_allowed", asyncAllowed)

	// Find the backing instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.UpdateServiceSpec{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.UpdateServiceSpec{}, apiresponses.ErrInstanceDoesNotExist
	}

	// Build update parameters
	updateParams := &cfclient.UpdateServiceInstanceParams{}

	// Handle plan change
	if details.PlanID != "" && details.PlanID != details.PreviousValues.PlanID {
		updateParams.ServicePlanGUID = &details.PlanID
	}

	// Handle parameter update
	if len(details.RawParameters) > 0 {
		var params map[string]interface{}
		if err := json.Unmarshal(details.RawParameters, &params); err != nil {
			return domain.UpdateServiceSpec{}, apiresponses.NewFailureResponse(
				fmt.Errorf("invalid parameters: %w", err),
				400, "invalid-parameters")
		}
		updateParams.Parameters = params
	}

	// Handle maintenance info update
	if details.MaintenanceInfo != nil {
		updateParams.MaintenanceInfo = &resource.ServiceInstanceMaintenanceInfo{
			Version:     details.MaintenanceInfo.Version,
			Description: details.MaintenanceInfo.Description,
		}
	}

	// Update the instance
	result, err := b.cfClient.UpdateServiceInstance(ctx, existing.GUID, updateParams)
	if err != nil {
		b.logger.Error("failed to update service instance", "error", err)
		return domain.UpdateServiceSpec{}, errors.RedactError(err)
	}

	// Handle async operation
	if result.IsAsync {
		if !asyncAllowed {
			return domain.UpdateServiceSpec{}, apiresponses.ErrAsyncRequired
		}

		opState := &OperationState{
			Type:                       OperationUpdate,
			BackingCFServiceInstanceID: existing.GUID,
			JobGUID:                    result.JobGUID,
		}

		return domain.UpdateServiceSpec{
			IsAsync:       true,
			OperationData: opState.Encode(),
		}, nil
	}

	var dashboardURL string
	if result.ServiceInstance != nil && result.ServiceInstance.DashboardURL != nil {
		dashboardURL = *result.ServiceInstance.DashboardURL
	}

	return domain.UpdateServiceSpec{
		IsAsync:      false,
		DashboardURL: dashboardURL,
	}, nil
}

// LastOperation fetches the last operation state for a service instance.
func (b *Broker) LastOperation(ctx context.Context, instanceID string, details domain.PollDetails) (domain.LastOperation, error) {
	b.logger.Debug("polling last operation",
		"instance_id", instanceID,
		"operation_data", details.OperationData)

	// Parse operation state
	opState, err := DecodeOperationState(details.OperationData)
	if err != nil {
		// Fall back to checking instance state directly
		b.logger.Warn("failed to parse operation state, checking instance directly", "error", err)
		return b.pollInstanceState(ctx, instanceID)
	}

	// CF create, update, delete, bind, and unbind requests all expose a job
	// GUID. Poll it directly, including provisioning before an instance exists.
	if opState.JobGUID != "" {
		job, err := b.cfClient.GetBindingLastOperation(ctx, opState.JobGUID)
		if err != nil {
			return domain.LastOperation{}, errors.RedactError(err)
		}
		return b.mapJobToLastOperation(job), nil
	}

	// For deprovision, check if instance still exists
	if opState.Type == OperationDeprovision {
		existing, err := b.findExistingInstance(ctx, instanceID)
		if err != nil {
			// Error checking - might be temporary
			return domain.LastOperation{
				State:       domain.InProgress,
				Description: "Checking instance status",
			}, nil
		}
		if existing == nil {
			// Instance is gone, deprovision succeeded
			return domain.LastOperation{
				State:       domain.Succeeded,
				Description: "Service instance deleted",
			}, nil
		}
	}

	// Check the backing instance's last operation
	si, err := b.cfClient.GetServiceInstance(ctx, opState.BackingCFServiceInstanceID)
	if err != nil {
		// For deprovision, not found means success
		if opState.Type == OperationDeprovision {
			return domain.LastOperation{
				State:       domain.Succeeded,
				Description: "Service instance deleted",
			}, nil
		}
		return domain.LastOperation{}, errors.RedactError(err)
	}

	return b.mapCFLastOperation(si.LastOperation), nil
}

// Bind creates a service binding (service key).
func (b *Broker) Bind(ctx context.Context, instanceID, bindingID string, details domain.BindDetails, asyncAllowed bool) (domain.Binding, error) {
	b.logger.Info("creating service binding",
		"instance_id", instanceID,
		"binding_id", bindingID,
		"async_allowed", asyncAllowed)

	// Find the backing instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.Binding{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.Binding{}, apiresponses.NewFailureResponse(
			fmt.Errorf("service instance not found"),
			400, "instance-not-found")
	}

	// Parse parameters
	var params map[string]interface{}
	if len(details.RawParameters) > 0 {
		if err := json.Unmarshal(details.RawParameters, &params); err != nil {
			return domain.Binding{}, apiresponses.NewFailureResponse(
				fmt.Errorf("invalid parameters: %w", err),
				400, "invalid-parameters")
		}
	}

	// Check if binding already exists
	existingBinding, err := b.cfClient.GetServiceCredentialBindingByName(ctx, bindingID, existing.GUID)
	if err != nil {
		b.logger.Warn("error checking for existing binding", "error", err)
	}
	if existingBinding != nil {
		// Binding already exists - get credentials and return
		creds, err := b.cfClient.GetServiceBindingDetails(ctx, existingBinding.GUID)
		if err != nil {
			return domain.Binding{}, errors.RedactError(err)
		}
		return domain.Binding{
			AlreadyExists: true,
			Credentials:   creds,
		}, nil
	}

	// Create service key
	result, err := b.cfClient.CreateServiceCredentialBinding(ctx, &cfclient.ServiceBindingParams{
		Name:                bindingID,
		ServiceInstanceGUID: existing.GUID,
		Parameters:          params,
	})
	if err != nil {
		b.logger.Error("failed to create service binding", "error", err)
		return domain.Binding{}, errors.RedactError(err)
	}

	// Handle async operation
	if result.IsAsync {
		if !asyncAllowed {
			return domain.Binding{}, apiresponses.ErrAsyncRequired
		}

		opState := &OperationState{
			Type:                       OperationBind,
			BackingCFServiceInstanceID: existing.GUID,
			JobGUID:                    result.JobGUID,
		}

		return domain.Binding{
			IsAsync:       true,
			OperationData: opState.Encode(),
		}, nil
	}

	// Get credentials for sync completion
	if result.ServiceBinding != nil {
		creds, err := b.cfClient.GetServiceBindingDetails(ctx, result.ServiceBinding.GUID)
		if err != nil {
			b.logger.Error("failed to get binding credentials", "error", err)
			return domain.Binding{}, errors.RedactError(err)
		}

		return domain.Binding{
			Credentials: creds,
		}, nil
	}

	return domain.Binding{}, nil
}

// Unbind deletes a service binding.
func (b *Broker) Unbind(ctx context.Context, instanceID, bindingID string, details domain.UnbindDetails, asyncAllowed bool) (domain.UnbindSpec, error) {
	b.logger.Info("deleting service binding",
		"instance_id", instanceID,
		"binding_id", bindingID,
		"async_allowed", asyncAllowed)

	// Find the backing instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.UnbindSpec{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.UnbindSpec{}, apiresponses.ErrInstanceDoesNotExist
	}

	// Find the binding
	binding, err := b.cfClient.GetServiceCredentialBindingByName(ctx, bindingID, existing.GUID)
	if err != nil {
		return domain.UnbindSpec{}, errors.RedactError(err)
	}

	if binding == nil {
		return domain.UnbindSpec{}, apiresponses.ErrBindingDoesNotExist
	}

	// Delete the binding
	result, err := b.cfClient.DeleteServiceCredentialBinding(ctx, binding.GUID)
	if err != nil {
		b.logger.Error("failed to delete service binding", "error", err)
		return domain.UnbindSpec{}, errors.RedactError(err)
	}

	// Handle async operation
	if result.IsAsync {
		if !asyncAllowed {
			return domain.UnbindSpec{}, apiresponses.ErrAsyncRequired
		}

		opState := &OperationState{
			Type:    OperationUnbind,
			JobGUID: result.JobGUID,
		}

		return domain.UnbindSpec{
			IsAsync:       true,
			OperationData: opState.Encode(),
		}, nil
	}

	return domain.UnbindSpec{IsAsync: false}, nil
}

// GetBinding fetches a service binding.
func (b *Broker) GetBinding(ctx context.Context, instanceID, bindingID string, details domain.FetchBindingDetails) (domain.GetBindingSpec, error) {
	b.logger.Debug("getting service binding",
		"instance_id", instanceID,
		"binding_id", bindingID)

	// Find the backing instance
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.GetBindingSpec{}, errors.RedactError(err)
	}

	if existing == nil {
		return domain.GetBindingSpec{}, apiresponses.ErrInstanceDoesNotExist
	}

	// Find the binding
	binding, err := b.cfClient.GetServiceCredentialBindingByName(ctx, bindingID, existing.GUID)
	if err != nil {
		return domain.GetBindingSpec{}, errors.RedactError(err)
	}

	if binding == nil {
		return domain.GetBindingSpec{}, apiresponses.ErrBindingDoesNotExist
	}

	// Get credentials
	creds, err := b.cfClient.GetServiceBindingDetails(ctx, binding.GUID)
	if err != nil {
		return domain.GetBindingSpec{}, errors.RedactError(err)
	}

	return domain.GetBindingSpec{
		Credentials: creds,
	}, nil
}

// LastBindingOperation fetches the last operation state for a service binding.
func (b *Broker) LastBindingOperation(ctx context.Context, instanceID, bindingID string, details domain.PollDetails) (domain.LastOperation, error) {
	b.logger.Debug("polling last binding operation",
		"instance_id", instanceID,
		"binding_id", bindingID,
		"operation_data", details.OperationData)

	// Parse operation state
	opState, err := DecodeOperationState(details.OperationData)
	if err != nil {
		return domain.LastOperation{}, fmt.Errorf("invalid operation data: %w", err)
	}

	// For async binding operations, check the job status
	if opState.JobGUID != "" {
		job, err := b.cfClient.GetBindingLastOperation(ctx, opState.JobGUID)
		if err != nil {
			return domain.LastOperation{}, errors.RedactError(err)
		}

		return b.mapJobToLastOperation(job), nil
	}

	// Fall back to checking binding existence
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.LastOperation{}, errors.RedactError(err)
	}

	if existing == nil {
		// Instance doesn't exist, binding operation failed
		return domain.LastOperation{
			State:       domain.Failed,
			Description: "Service instance not found",
		}, nil
	}

	binding, err := b.cfClient.GetServiceCredentialBindingByName(ctx, bindingID, existing.GUID)
	if err != nil {
		return domain.LastOperation{}, errors.RedactError(err)
	}

	if opState.Type == OperationUnbind {
		if binding == nil {
			return domain.LastOperation{
				State:       domain.Succeeded,
				Description: "Binding deleted",
			}, nil
		}
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Deleting binding",
		}, nil
	}

	// For bind operation
	if binding != nil {
		return domain.LastOperation{
			State:       domain.Succeeded,
			Description: "Binding created",
		}, nil
	}

	return domain.LastOperation{
		State:       domain.InProgress,
		Description: "Creating binding",
	}, nil
}

// Helper methods

func (b *Broker) findExistingInstance(ctx context.Context, instanceID string) (*struct {
	GUID         string
	DashboardURL *string
}, error) {
	// Look up by label "brokered_service_instance_guid"
	instances, err := b.cfClient.ListServiceInstancesByLabel(ctx, b.defaultSpaceGUID, "brokered_service_instance_guid", instanceID)
	if err != nil {
		return nil, err
	}

	if len(instances) > 0 {
		return &struct {
			GUID         string
			DashboardURL *string
		}{
			GUID:         instances[0].GUID,
			DashboardURL: instances[0].DashboardURL,
		}, nil
	}

	// Fall back to name lookup
	si, err := b.cfClient.GetServiceInstanceByNameInSpace(ctx, instanceID, b.defaultSpaceGUID)
	if err != nil {
		return nil, err
	}

	if si != nil {
		return &struct {
			GUID         string
			DashboardURL *string
		}{
			GUID:         si.GUID,
			DashboardURL: si.DashboardURL,
		}, nil
	}

	return nil, nil
}

func (b *Broker) isCompatibleDuplicate(ctx context.Context, existing *struct {
	GUID         string
	DashboardURL *string
}, details domain.ProvisionDetails) (bool, error) {
	if existing == nil {
		return false, nil
	}

	backing, err := b.cfClient.GetServiceInstance(ctx, existing.GUID)
	if err != nil {
		return false, err
	}
	if backing.Relationships.ServicePlan == nil || backing.Relationships.ServicePlan.Data == nil || backing.Relationships.ServicePlan.Data.GUID != details.PlanID {
		return false, nil
	}

	requested := map[string]interface{}{}
	if len(details.RawParameters) > 0 {
		if err := json.Unmarshal(details.RawParameters, &requested); err != nil {
			return false, apiresponses.NewFailureResponse(fmt.Errorf("invalid parameters: %w", err), 400, "invalid-parameters")
		}
	}

	actual, err := b.cfClient.GetServiceInstanceParameters(ctx, existing.GUID)
	if err != nil {
		return false, err
	}
	if actual == nil {
		actual = map[string]interface{}{}
	}

	return reflect.DeepEqual(requested, actual), nil
}

func (b *Broker) buildMetadata(ctx context.Context, instanceID string, rawContext json.RawMessage) (map[string]*string, map[string]*string) {
	labels := make(map[string]*string)
	annotations := make(map[string]*string)

	// Always set the brokered instance GUID
	instanceGUID := instanceID
	labels["brokered_service_instance_guid"] = &instanceGUID

	// Prefer the identity header installed by brokerapi middleware. OSB context
	// remains the fallback for clients that do not send originating identity.
	if b.metadataService != nil && b.contextParser != nil {
		var parsedCtx *osbcontext.Context
		var err error
		if header, _ := ctx.Value(middlewares.OriginatingIdentityKey).(string); header != "" {
			parsedCtx, err = b.contextParser.ParseOriginatingIdentityContext(header)
		} else if len(rawContext) > 0 {
			parsedCtx, err = b.contextParser.Parse(rawContext)
		}
		if err != nil {
			b.logger.Warn("failed to parse metadata context", "error", err)
		} else if parsedCtx != nil {
			ctxLabels, ctxAnnotations := b.metadataService.Format(parsedCtx)
			for k, v := range ctxLabels {
				labels[k] = v
			}
			for k, v := range ctxAnnotations {
				annotations[k] = v
			}
		}
	}

	return labels, annotations
}

func (b *Broker) pollInstanceState(ctx context.Context, instanceID string) (domain.LastOperation, error) {
	existing, err := b.findExistingInstance(ctx, instanceID)
	if err != nil {
		return domain.LastOperation{}, errors.RedactError(err)
	}

	if existing == nil {
		// Instance doesn't exist - could be deleted or never created
		return domain.LastOperation{
			State:       domain.Succeeded,
			Description: "Service instance deleted",
		}, nil
	}

	si, err := b.cfClient.GetServiceInstance(ctx, existing.GUID)
	if err != nil {
		return domain.LastOperation{}, errors.RedactError(err)
	}

	return b.mapCFLastOperation(si.LastOperation), nil
}

func (b *Broker) mapCFLastOperation(lastOp interface{}) domain.LastOperation {
	if lastOp == nil {
		return domain.LastOperation{
			State:       domain.Succeeded,
			Description: "Operation complete",
		}
	}

	// Type assertion for LastOperation
	type cfLastOp struct {
		Type        string `json:"type"`
		State       string `json:"state"`
		Description string `json:"description"`
	}

	// Handle different possible types
	switch op := lastOp.(type) {
	case resource.LastOperation:
		return b.mapCFState(op.State, op.Description)
	case *resource.LastOperation:
		if op == nil {
			return domain.LastOperation{State: domain.Succeeded, Description: "Operation complete"}
		}
		return b.mapCFState(op.State, op.Description)
	case *cfLastOp:
		return b.mapCFState(op.State, op.Description)
	case map[string]interface{}:
		state, _ := op["state"].(string)
		desc, _ := op["description"].(string)
		return b.mapCFState(state, desc)
	default:
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Checking operation status",
		}
	}
}

func (b *Broker) mapCFState(state, description string) domain.LastOperation {
	switch state {
	case "succeeded":
		return domain.LastOperation{
			State:       domain.Succeeded,
			Description: description,
		}
	case "failed":
		return domain.LastOperation{
			State:       domain.Failed,
			Description: description,
		}
	case "in progress", "initial":
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: description,
		}
	default:
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: description,
		}
	}
}

func (b *Broker) mapJobToLastOperation(job interface{}) domain.LastOperation {
	if job == nil {
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Processing",
		}
	}

	// Handle Job resource type
	type cfJob struct {
		State string `json:"state"`
	}

	switch j := job.(type) {
	case resource.Job:
		return b.mapJobState(string(j.State))
	case *resource.Job:
		if j == nil {
			return domain.LastOperation{State: domain.InProgress, Description: "Processing"}
		}
		return b.mapJobState(string(j.State))
	case *cfJob:
		return b.mapJobState(j.State)
	case map[string]interface{}:
		state, _ := j["state"].(string)
		return b.mapJobState(state)
	default:
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Processing",
		}
	}
}

func (b *Broker) mapJobState(state string) domain.LastOperation {
	switch state {
	case "COMPLETE":
		return domain.LastOperation{
			State:       domain.Succeeded,
			Description: "Operation complete",
		}
	case "FAILED":
		return domain.LastOperation{
			State:       domain.Failed,
			Description: "Operation failed",
		}
	case "PROCESSING", "POLLING":
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Processing",
		}
	default:
		return domain.LastOperation{
			State:       domain.InProgress,
			Description: "Processing",
		}
	}
}
