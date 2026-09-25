package broker

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
)

// OperationType represents the type of async operation.
type OperationType string

const (
	OperationProvision   OperationType = "provision"
	OperationDeprovision OperationType = "deprovision"
	OperationUpdate      OperationType = "update"
	OperationBind        OperationType = "bind"
	OperationUnbind      OperationType = "unbind"
)

// OperationState tracks async operation details.
// This is encoded into the operation field returned to the platform.
type OperationState struct {
	// Type of operation being performed
	Type OperationType `json:"type"`

	// InstanceID is the OSB instance ID. It identifies an asynchronously
	// provisioning instance before Cloud Foundry has assigned a backing GUID.
	InstanceID string `json:"instance_id,omitempty"`

	// BackingCFServiceInstanceID is the GUID of the backing CF service instance
	BackingCFServiceInstanceID string `json:"backing_cf_service_instance_guid,omitempty"`

	// JobGUID is the CF job GUID for async operations
	JobGUID string `json:"job_guid,omitempty"`

	// BindingGUID is the CF binding GUID for binding operations
	BindingGUID string `json:"binding_guid,omitempty"`
}

// Encode serializes the operation state to a string suitable for the operation field.
func (s *OperationState) Encode() string {
	data, err := json.Marshal(s)
	if err != nil {
		// Should never happen with this simple struct
		return ""
	}
	return base64.StdEncoding.EncodeToString(data)
}

// DecodeOperationState deserializes operation state from the operation field.
func DecodeOperationState(encoded string) (*OperationState, error) {
	if encoded == "" {
		return nil, fmt.Errorf("empty operation data")
	}

	data, err := base64.StdEncoding.DecodeString(encoded)
	if err != nil {
		// Try raw JSON for backwards compatibility
		data = []byte(encoded)
	}

	var state OperationState
	if err := json.Unmarshal(data, &state); err != nil {
		return nil, fmt.Errorf("invalid operation state: %w", err)
	}

	return &state, nil
}

// NewProvisionOperation creates an operation state for provisioning.
func NewProvisionOperation(instanceID, jobGUID string) *OperationState {
	return &OperationState{
		Type:       OperationProvision,
		InstanceID: instanceID,
		JobGUID:    jobGUID,
	}
}

// NewDeprovisionOperation creates an operation state for deprovisioning.
func NewDeprovisionOperation(backingInstanceGUID, jobGUID string) *OperationState {
	return &OperationState{
		Type:                       OperationDeprovision,
		BackingCFServiceInstanceID: backingInstanceGUID,
		JobGUID:                    jobGUID,
	}
}

// NewUpdateOperation creates an operation state for updating.
func NewUpdateOperation(backingInstanceGUID, jobGUID string) *OperationState {
	return &OperationState{
		Type:                       OperationUpdate,
		BackingCFServiceInstanceID: backingInstanceGUID,
		JobGUID:                    jobGUID,
	}
}

// NewBindOperation creates an operation state for binding.
func NewBindOperation(backingInstanceGUID, jobGUID string) *OperationState {
	return &OperationState{
		Type:                       OperationBind,
		BackingCFServiceInstanceID: backingInstanceGUID,
		JobGUID:                    jobGUID,
	}
}

// NewUnbindOperation creates an operation state for unbinding.
func NewUnbindOperation(jobGUID string) *OperationState {
	return &OperationState{
		Type:    OperationUnbind,
		JobGUID: jobGUID,
	}
}
