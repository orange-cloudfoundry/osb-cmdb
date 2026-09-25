## Purpose

Go implementation of OSB-CMDB service broker that proxies Open Service Broker API calls to Cloud Foundry, implementing all 10 OSB API operations with async support, metadata tracking, and multi-tenant isolation.

## ADDED Requirements

### Requirement: Implement OSB Catalog endpoint
The broker SHALL expose a GET /v2/catalog endpoint that returns the service catalog fetched from the backing Cloud Foundry marketplace.

#### Scenario: Successful catalog fetch
- **WHEN** a client sends GET /v2/catalog with valid credentials
- **THEN** the broker returns HTTP 200 with a JSON catalog containing all non-excluded services from the backing CF space

#### Scenario: Unauthenticated catalog request
- **WHEN** a client sends GET /v2/catalog without credentials
- **THEN** the broker returns HTTP 401 Unauthorized

### Requirement: Implement OSB Provision endpoint
The broker SHALL expose a PUT /v2/service_instances/{instance_id} endpoint that creates a backing service instance in Cloud Foundry.

#### Scenario: Synchronous provision success
- **WHEN** a client sends a provision request and the backing broker completes synchronously
- **THEN** the broker returns HTTP 201 Created

#### Scenario: Asynchronous provision
- **WHEN** a client sends a provision request with accepts_incomplete=true and the backing broker requires async
- **THEN** the broker returns HTTP 202 Accepted with an opaque operation field encoding the CF create job GUID, the OSB instance ID, and the operation type

#### Scenario: Duplicate instance with compatible parameters
- **WHEN** a client sends a provision request for an instance_id that already exists with the same plan and parameters
- **THEN** the broker returns HTTP 200 OK

#### Scenario: Duplicate instance with incompatible parameters
- **WHEN** a client sends a provision request for an instance_id that already exists with different plan or parameters
- **THEN** the broker returns HTTP 409 Conflict

### Requirement: Implement OSB Deprovision endpoint
The broker SHALL expose a DELETE /v2/service_instances/{instance_id} endpoint that deletes the backing service instance.

#### Scenario: Successful deprovision
- **WHEN** a client sends a deprovision request for an existing instance
- **THEN** the broker deletes all service keys first, then deletes the backing service instance

#### Scenario: Deprovision non-existent instance
- **WHEN** a client sends a deprovision request for an instance that does not exist
- **THEN** the broker returns HTTP 410 Gone

### Requirement: Implement OSB Update endpoint
The broker SHALL expose a PATCH /v2/service_instances/{instance_id} endpoint that updates the backing service instance.

#### Scenario: Plan change update
- **WHEN** a client sends an update request with a new plan_id
- **THEN** the broker updates the backing service instance to the new plan

#### Scenario: Parameter update
- **WHEN** a client sends an update request with new parameters
- **THEN** the broker updates the backing service instance with the new parameters

#### Scenario: Maintenance info upgrade
- **WHEN** a client sends an update request with updated maintenance_info
- **THEN** the broker forwards the upgrade to the backing service instance

### Requirement: Implement OSB Get Instance endpoint
The broker SHALL expose a GET /v2/service_instances/{instance_id} endpoint that fetches instance details.

#### Scenario: Fetch existing instance
- **WHEN** a client sends a get instance request for an existing instance
- **THEN** the broker returns the instance parameters and dashboard URL

#### Scenario: Fetch non-existent instance
- **WHEN** a client sends a get instance request for an instance that does not exist
- **THEN** the broker returns HTTP 404 Not Found

### Requirement: Implement OSB Last Operation endpoint for instances
The broker SHALL expose a GET /v2/service_instances/{instance_id}/last_operation endpoint that polls async operation status.

#### Scenario: Poll in-progress operation
- **WHEN** a client polls last_operation for an async operation still in progress
- **THEN** the broker returns state "in progress"

#### Scenario: Poll succeeded operation
- **WHEN** a client polls last_operation for a completed operation
- **THEN** the broker returns state "succeeded"

#### Scenario: Poll failed operation
- **WHEN** a client polls last_operation for a failed operation
- **THEN** the broker returns state "failed" with a description

#### Scenario: Poll asynchronous provision job
- **WHEN** a client polls last_operation with operation data containing a CF job GUID
- **THEN** the broker fetches the CF job and maps COMPLETE to "succeeded", FAILED to "failed", and PROCESSING or POLLING to "in progress"

#### Scenario: Poll delete operation for deleted instance
- **WHEN** a client polls last_operation for a delete and the backing instance no longer exists
- **THEN** the broker returns state "succeeded"

### Requirement: Implement OSB Bind endpoint
The broker SHALL expose a PUT /v2/service_instances/{instance_id}/service_bindings/{binding_id} endpoint that creates a service key.

#### Scenario: Synchronous bind success
- **WHEN** a client sends a bind request and the backing broker supports sync bindings
- **THEN** the broker returns HTTP 201 Created with credentials

#### Scenario: Asynchronous bind
- **WHEN** a client sends a bind request with accepts_incomplete=true and the backing broker requires async
- **THEN** the broker returns HTTP 202 Accepted with an operation field containing the job GUID

#### Scenario: Bind to non-existent instance
- **WHEN** a client sends a bind request for an instance that does not exist
- **THEN** the broker returns HTTP 400 Bad Request

### Requirement: Implement OSB Unbind endpoint
The broker SHALL expose a DELETE /v2/service_instances/{instance_id}/service_bindings/{binding_id} endpoint that deletes a service key.

#### Scenario: Successful unbind
- **WHEN** a client sends an unbind request for an existing binding
- **THEN** the broker deletes the backing service key and returns HTTP 200 OK

#### Scenario: Unbind non-existent binding
- **WHEN** a client sends an unbind request for a binding that does not exist
- **THEN** the broker returns HTTP 410 Gone

### Requirement: Implement OSB Get Binding endpoint
The broker SHALL expose a GET /v2/service_instances/{instance_id}/service_bindings/{binding_id} endpoint that fetches binding credentials.

#### Scenario: Fetch existing binding
- **WHEN** a client sends a get binding request for an existing binding
- **THEN** the broker returns the binding credentials

#### Scenario: Fetch non-existent binding
- **WHEN** a client sends a get binding request for a binding that does not exist
- **THEN** the broker returns HTTP 404 Not Found

### Requirement: Implement OSB Last Binding Operation endpoint
The broker SHALL expose a GET /v2/service_instances/{instance_id}/service_bindings/{binding_id}/last_operation endpoint that polls async binding operation status.

#### Scenario: Poll binding operation
- **WHEN** a client polls last_binding_operation for an async binding operation
- **THEN** the broker fetches the CF job status and returns the corresponding OSB state

### Requirement: Redact URLs in error messages
The broker SHALL redact Cloud Foundry API URLs from error messages returned to clients.

#### Scenario: Error with CF URL
- **WHEN** a CF API error contains a URL like https://api.cf.example.com/v3/...
- **THEN** the broker replaces the URL with "redacted-url" before returning the error to the client

### Requirement: Track async operations with operation state
The broker SHALL encode async operation tracking information in the operation field.

#### Scenario: Instance operation state
- **WHEN** an async instance operation is initiated
- **THEN** the operation field contains opaque encoded JSON with the operation type; asynchronous provisioning includes the CF job GUID and OSB instance ID, while operations with a known backing instance include its GUID

#### Scenario: Binding operation state
- **WHEN** an async binding operation is initiated
- **THEN** the operation field contains JSON with job_guid and operation type
