# OSB-CMDB Migration: Java to Go

This document describes the migration of OSB-CMDB from Java (Spring Boot) to Go, including a complete inventory of all OSB API operations, implementation patterns, and Java-to-Go code mappings.

## Table of Contents

- [Overview](#overview)
- [Architecture Comparison](#architecture-comparison)
- [Dependencies & Libraries](#dependencies--libraries)
- [OSB API Operations Inventory](#osb-api-operations-inventory)
- [Java to Go Migration Examples](#java-to-go-migration-examples)
- [Configuration Migration](#configuration-migration)
- [Error Handling Patterns](#error-handling-patterns)
- [Testing Strategy](#testing-strategy)
- [Deployment Artifacts](#deployment-artifacts)
- [References](#references)

---

## Overview

### What is OSB-CMDB?

OSB-CMDB is an Open Service Broker API facade that provides:
- **Inventory management** of service instances and service keys consumed by OSB clients
- **Service broker sharing** among multiple OSB client platforms (Cloud Foundry, Kubernetes)
- Translation of OSB API calls into Cloud Foundry Cloud Controller API calls
- Multi-tenant management with separate authentication per OSB client
- Metadata tracking and traceability from backing services to brokered services

### Migration Goals

| Goal | Description |
|------|-------------|
| **Cloud-native alignment** | Single binary, fast startup, low memory footprint |
| **Team expertise** | Leverage team's Go proficiency |
| **Simplification** | Remove reactive complexity (Mono/Flux → synchronous Go) |
| **Maintainability** | Idiomatic Go patterns, standard library where possible |

### Key Decisions

| Decision | Choice |
|----------|--------|
| Go Version | 1.22 |
| CF API | V3 only (simplifies hybrid V2/V3 approach) |
| OSB Framework | [brokerapi](https://github.com/pivotal-cf/brokerapi) v11+ |
| CF Client | [go-cfclient](https://github.com/cloudfoundry/go-cfclient) v3 |
| Logging | `log/slog` (standard library) |
| Configuration | Environment variables via `envconfig` |
| Testing | Standard Go `testing` + `testify` (mocks, no real CF) |

---

## Architecture Comparison

### Java Architecture (Current)

```
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                     │
├─────────────────────────────────────────────────────────────────┤
│  Spring Cloud Open Service Broker (3.5.x)                       │
│  ├── ServiceInstanceService (OsbCmdbServiceInstance)            │
│  └── ServiceInstanceBindingService (OsbCmdbServiceBinding)      │
├─────────────────────────────────────────────────────────────────┤
│  cf-java-client (5.14.x) - Reactive                             │
│  ├── CloudFoundryOperations (high-level)                        │
│  └── CloudFoundryClient (V2 + V3 APIs)                          │
├─────────────────────────────────────────────────────────────────┤
│  Spring Security (HTTP Basic Auth)                               │
│  Spring Boot Actuator (Health, Metrics)                          │
└─────────────────────────────────────────────────────────────────┘
```

### Go Architecture (Target)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Go Application                            │
├─────────────────────────────────────────────────────────────────┤
│  brokerapi v11+ (domain.ServiceBroker interface)                │
│  └── internal/broker (Broker implementation)                    │
├─────────────────────────────────────────────────────────────────┤
│  go-cfclient v3 (synchronous)                                   │
│  └── internal/cfclient (wrapper with error handling)            │
├─────────────────────────────────────────────────────────────────┤
│  chi router + standard net/http                                  │
│  HTTP Basic Auth middleware                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
osb-cmdb-go/
├── cmd/osb-cmdb/
│   └── main.go                    # Entry point
├── internal/
│   ├── broker/                    # OSB ServiceBroker implementation
│   │   ├── broker.go              # Main broker struct
│   │   ├── provision.go           # Provision operation
│   │   ├── deprovision.go         # Deprovision operation
│   │   ├── update.go              # Update operation
│   │   ├── bind.go                # Bind/Unbind operations
│   │   ├── last_operation.go      # Async polling
│   │   └── operation_state.go     # Operation state encoding
│   ├── catalog/                   # Dynamic catalog service
│   │   ├── service.go             # CatalogService interface
│   │   ├── dynamic.go             # CF marketplace → OSB catalog
│   │   ├── mapper.go              # Resource mapping
│   │   └── refresh.go             # Periodic refresh
│   ├── cfclient/                  # CF client wrapper
│   │   ├── client.go              # Client wrapper
│   │   ├── service_instance.go    # SI operations
│   │   ├── service_binding.go     # Binding operations
│   │   └── space.go               # Space management
│   ├── config/                    # Configuration
│   │   └── config.go              # envconfig-based config
│   ├── context/                   # OSB context parsing
│   │   ├── parser.go              # X-Broker-API-Originating-Identity
│   │   ├── cloudfoundry.go        # CF context
│   │   └── kubernetes.go          # K8S context
│   ├── metadata/                  # Metadata formatting
│   │   ├── formatter.go           # Formatter interface
│   │   ├── cloudfoundry.go        # CF labels/annotations
│   │   └── kubernetes.go          # K8S labels/annotations
│   ├── middleware/                # HTTP middleware
│   │   ├── apiinfo.go             # X-Api-Info-Location validation
│   │   └── logging.go             # Request logging
│   └── errors/                    # Error handling
│       └── redact.go              # URL redaction
├── test/
│   ├── unit/                      # Unit tests
│   ├── integration/               # Integration tests (mock CF)
│   └── mocks/                     # Mock implementations
├── deploy/
│   └── helm/osb-cmdb-go/          # OCI Helm chart
├── .github/workflows/
│   ├── ci.yml                     # Build, test, lint
│   └── release.yml                # Docker + Helm push
├── Dockerfile
├── Makefile
├── go.mod
└── go.sum
```

---

## Dependencies & Libraries

### Java Dependencies (Current)

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 2.6.6 | Application framework |
| Spring Cloud Open Service Broker | 3.5.1 | OSB API implementation |
| cf-java-client | 5.14.0 | Cloud Foundry API client |
| Spring Security | (via Boot) | Authentication |
| Spring WebFlux | (via Boot) | Reactive HTTP |
| Jackson | 2.13.2 | JSON serialization |

### Go Dependencies (Target)

| Dependency | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| [brokerapi](https://github.com/pivotal-cf/brokerapi) | v11.0.16+ | OSB API framework | [pkg.go.dev](https://pkg.go.dev/github.com/pivotal-cf/brokerapi/v11) |
| [go-cfclient](https://github.com/cloudfoundry/go-cfclient) | v3.0.0-beta.1 | CF API v3 client | [pkg.go.dev](https://pkg.go.dev/github.com/cloudfoundry/go-cfclient/v3) |
| [envconfig](https://github.com/kelseyhightower/envconfig) | v1.4.0 | Environment config | [pkg.go.dev](https://pkg.go.dev/github.com/kelseyhightower/envconfig) |
| [testify](https://github.com/stretchr/testify) | v1.9.0 | Testing utilities | [pkg.go.dev](https://pkg.go.dev/github.com/stretchr/testify) |
| [chi](https://github.com/go-chi/chi) | v5.0.12 | HTTP router | [pkg.go.dev](https://pkg.go.dev/github.com/go-chi/chi/v5) |

### go.mod

```go
module github.com/orange-cloudfoundry/osb-cmdb-go

go 1.22

require (
    github.com/pivotal-cf/brokerapi/v11 v11.0.16
    github.com/cloudfoundry/go-cfclient/v3 v3.0.0-beta.1
    github.com/kelseyhightower/envconfig v1.4.0
    github.com/stretchr/testify v1.9.0
)
```

---

## OSB API Operations Inventory

OSB-CMDB implements [Open Service Broker API v2.14](https://github.com/openservicebrokerapi/servicebroker/blob/v2.14/spec.md).

### Operations Summary

| # | Operation | OSB Endpoint | HTTP Method | Java Implementation | Go Implementation |
|---|-----------|--------------|-------------|---------------------|-------------------|
| 1 | **Get Catalog** | `/v2/catalog` | GET | `DynamicCatalogServiceImpl.fetchServiceDefinitions()` | `Broker.Services()` |
| 2 | **Provision** | `/v2/service_instances/{id}` | PUT | `OsbCmdbServiceInstance.createServiceInstance()` | `Broker.Provision()` |
| 3 | **Get Instance** | `/v2/service_instances/{id}` | GET | `OsbCmdbServiceInstance.getServiceInstance()` | `Broker.GetInstance()` |
| 4 | **Update** | `/v2/service_instances/{id}` | PATCH | `OsbCmdbServiceInstance.updateServiceInstance()` | `Broker.Update()` |
| 5 | **Deprovision** | `/v2/service_instances/{id}` | DELETE | `OsbCmdbServiceInstance.deleteServiceInstance()` | `Broker.Deprovision()` |
| 6 | **Last Operation (Instance)** | `/v2/service_instances/{id}/last_operation` | GET | `OsbCmdbServiceInstance.getLastOperation()` | `Broker.LastOperation()` |
| 7 | **Bind** | `/v2/service_instances/{id}/service_bindings/{bid}` | PUT | `OsbCmdbServiceBinding.createServiceInstanceBinding()` | `Broker.Bind()` |
| 8 | **Get Binding** | `/v2/service_instances/{id}/service_bindings/{bid}` | GET | `OsbCmdbServiceBinding.getServiceInstanceBinding()` | `Broker.GetBinding()` |
| 9 | **Unbind** | `/v2/service_instances/{id}/service_bindings/{bid}` | DELETE | `OsbCmdbServiceBinding.deleteServiceInstanceBinding()` | `Broker.Unbind()` |
| 10 | **Last Operation (Binding)** | `/v2/service_instances/{id}/service_bindings/{bid}/last_operation` | GET | `OsbCmdbServiceBinding.getLastOperation()` | `Broker.LastBindingOperation()` |

### Detailed Operation Specifications

#### 1. Get Catalog

| Aspect | Description |
|--------|-------------|
| **Purpose** | Fetch service catalog dynamically from CF marketplace |
| **Java Location** | `DynamicCatalogServiceImpl.java:49-68` |
| **Key Logic** | 1. Target CF org/space<br>2. List services in space<br>3. Fetch plans per service<br>4. Filter excluded services<br>5. Map to OSB ServiceDefinition |
| **CF APIs Used** | V2: `ListSpaceServicesRequest`, `ListServicePlansRequest` |
| **Go Notes** | Use V3 API: `ServiceOfferings.List()`, `ServicePlans.List()` |

#### 2. Provision Service Instance

| Aspect | Description |
|--------|-------------|
| **Purpose** | Create backing service instance in CF |
| **Java Location** | `OsbCmdbServiceInstance.java:211-300` |
| **Key Logic** | 1. Validate maintenance info<br>2. Get backing plan ID<br>3. Format metadata<br>4. Reject duplicate GUID<br>5. Create SI via CF API<br>6. Handle sync/async response<br>7. Update metadata labels |
| **Async State** | `CmdbOperationState{backingCfServiceInstanceGuid, operationType: CREATE}` |
| **Error Handling** | Concurrent duplicate → 200/202/409, Not found → 404 |

#### 3. Get Service Instance

| Aspect | Description |
|--------|-------------|
| **Purpose** | Fetch service instance details |
| **Java Location** | `OsbCmdbServiceInstance.java:126-209` |
| **Key Logic** | 1. Lookup by brokered GUID label<br>2. Verify tenant org<br>3. Fetch parameters<br>4. Hide x-osb-cmdb param if configured |
| **CF APIs Used** | V3: `ListServiceInstancesRequest` (label selector), V2: `GetServiceInstanceParametersRequest` |

#### 4. Update Service Instance

| Aspect | Description |
|--------|-------------|
| **Purpose** | Update plan, parameters, or maintenance info |
| **Java Location** | `OsbCmdbServiceInstance.java:483-603` |
| **Key Logic** | 1. Validate plan ID<br>2. Validate maintenance info<br>3. Check for no-op upgrade<br>4. Update backing SI<br>5. Always update metadata |
| **Async State** | `CmdbOperationState{backingCfServiceInstanceGuid, operationType: UPDATE}` |

#### 5. Deprovision Service Instance

| Aspect | Description |
|--------|-------------|
| **Purpose** | Delete backing service instance |
| **Java Location** | `OsbCmdbServiceInstance.java:315-399` |
| **Key Logic** | 1. Lookup backing SI<br>2. Delete all service keys first<br>3. Delete backing SI<br>4. Handle sync/async response |
| **Async State** | `CmdbOperationState{backingCfServiceInstanceGuid, operationType: DELETE}` |

#### 6. Last Operation (Instance)

| Aspect | Description |
|--------|-------------|
| **Purpose** | Poll async operation status |
| **Java Location** | `OsbCmdbServiceInstance.java:401-481` |
| **Key Logic** | 1. Parse operation JSON<br>2. Fetch backing SI by GUID<br>3. If not found: DELETE→succeeded, CREATE/UPDATE→failed<br>4. Convert CF state to OSB state |
| **State Mapping** | `succeeded`→SUCCEEDED, `failed`→FAILED, `in progress`→IN_PROGRESS |

#### 7. Bind (Create Service Binding)

| Aspect | Description |
|--------|-------------|
| **Purpose** | Create service key for backing SI |
| **Java Location** | `OsbCmdbServiceBinding.java:67-156` |
| **Key Logic** | 1. Validate backing SI exists<br>2. Try sync creation (V2 API)<br>3. If async required (error 10001): use V3 API<br>4. Return credentials or 202 |
| **Async State** | `CmdbOperationState{asyncJobId, operationType: CREATE}` |

#### 8. Get Binding

| Aspect | Description |
|--------|-------------|
| **Purpose** | Fetch binding credentials |
| **Java Location** | `OsbCmdbServiceBinding.java:200-238` |
| **Key Logic** | 1. List bindings by name (V3)<br>2. Get binding details/credentials |
| **CF APIs Used** | V3: `ListServiceBindingsRequest`, `GetServiceBindingDetailsRequest` |

#### 9. Unbind (Delete Service Binding)

| Aspect | Description |
|--------|-------------|
| **Purpose** | Delete service key |
| **Java Location** | `OsbCmdbServiceBinding.java:260-331` |
| **Key Logic** | 1. Lookup backing SI<br>2. Try sync delete<br>3. If async required: use V3 API |
| **Async State** | `CmdbOperationState{asyncJobId, operationType: DELETE}` |

#### 10. Last Operation (Binding)

| Aspect | Description |
|--------|-------------|
| **Purpose** | Poll async binding operation |
| **Java Location** | `OsbCmdbServiceBinding.java:171-198` |
| **Key Logic** | 1. Parse operation JSON<br>2. Get CF job status (V3)<br>3. Convert job state to OSB state |
| **State Mapping** | `COMPLETE`→SUCCEEDED, `FAILED`→FAILED, `POLLING/PROCESSING`→IN_PROGRESS |

---

## Java to Go Migration Examples

### 1. Provision Service Instance

<details>
<summary><b>Java Implementation (OsbCmdbServiceInstance.java:211-300)</b></summary>

```java
@Override
public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
    if (osbInterceptor != null && osbInterceptor.accept(request)) {
        return osbInterceptor.createServiceInstance(request);
    }
    
    maintenanceInfoFormatterService.validateAnyCreateRequest(request);
    
    String backingServiceName = request.getServiceDefinition().getName();
    CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
    String backingServicePlanId = lookUpBackingPlanId(spacedTargetedOperations, backingServiceName, 
        request.getPlan().getName());
    
    MetaData formattedMetaData = createServiceMetadataFormatterService.formatAsMetadata(request);
    rejectDuplicateServiceInstanceGuid(request);
    
    try {
        org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceResponse response = 
            client.serviceInstances()
                .create(org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest.builder()
                    .name(truncatedServiceInstanceName(request.getServiceInstanceId()))
                    .servicePlanId(backingServicePlanId)
                    .spaceId(getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations))
                    .parameters(formatParams(request, formattedMetaData))
                    .acceptsIncomplete(true)
                    .build())
                .block();
        
        LastOperation lastOperation = response.getEntity().getLastOperation();
        if ("in progress".equals(lastOperation.getState())) {
            return Mono.just(CreateServiceInstanceResponse.builder()
                .async(true)
                .operation(toJson(new CmdbOperationState(response.getMetadata().getId(), OsbOperation.CREATE)))
                .build());
        }
        // ... handle sync success
    } catch (Exception e) {
        return handleCreateException(e, request, spacedTargetedOperations);
    } finally {
        updateBackingServiceInstanceMetadata(request, formattedMetaData);
    }
}
```
</details>

<details>
<summary><b>Go Implementation (internal/broker/provision.go)</b></summary>

```go
func (b *Broker) Provision(
    ctx context.Context,
    instanceID string,
    details domain.ProvisionDetails,
    asyncAllowed bool,
) (domain.ProvisionedServiceSpec, error) {
    
    log := slog.With("instanceID", instanceID, "serviceID", details.ServiceID)
    log.Info("provisioning service instance")
    
    // Validate maintenance info
    if err := b.maintenanceInfo.ValidateCreateRequest(details); err != nil {
        return domain.ProvisionedServiceSpec{}, err
    }
    
    // Get backing service info
    serviceDef, err := b.catalog.GetService(ctx, details.ServiceID)
    if err != nil {
        return domain.ProvisionedServiceSpec{}, err
    }
    
    plan, err := b.catalog.GetPlan(ctx, details.ServiceID, details.PlanID)
    if err != nil {
        return domain.ProvisionedServiceSpec{}, err
    }
    
    // Format metadata from OSB context
    metadata := b.metadataFormatter.Format(ctx, details.RawContext, instanceID)
    
    // Check for duplicate
    if err := b.rejectDuplicateInstance(ctx, instanceID); err != nil {
        return domain.ProvisionedServiceSpec{}, err
    }
    
    // Get backing space
    spaceGUID, err := b.cf.GetOrCreateSpace(ctx, serviceDef.Name)
    if err != nil {
        return domain.ProvisionedServiceSpec{}, err
    }
    
    // Create backing service instance (CF V3 API)
    backingInstance, jobGUID, err := b.cf.CreateServiceInstance(ctx, &cfclient.CreateServiceInstanceRequest{
        Name:            truncateName(instanceID, 50),
        SpaceGUID:       spaceGUID,
        ServicePlanGUID: plan.ID,
        Parameters:      b.formatParams(details.RawParameters, metadata),
        Labels:          metadata.Labels,
        Annotations:     metadata.Annotations,
    })
    if err != nil {
        return b.handleCreateError(ctx, err, instanceID, details)
    }
    
    // Check if async
    if jobGUID != "" {
        opState := OperationState{
            BackingServiceGUID: backingInstance.GUID,
            Operation:          OperationCreate,
        }
        return domain.ProvisionedServiceSpec{
            IsAsync:       true,
            OperationData: opState.Encode(),
        }, nil
    }
    
    return domain.ProvisionedServiceSpec{IsAsync: false}, nil
}
```
</details>

### 2. Bind (Create Service Binding)

<details>
<summary><b>Java Implementation (OsbCmdbServiceBinding.java:67-156)</b></summary>

```java
@Override
public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
    CreateServiceInstanceBindingRequest request) {
    
    if (osbInterceptor != null && osbInterceptor.accept(request)) {
        return osbInterceptor.createServiceInstanceBinding(request);
    }
    
    CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(
        request.getServiceDefinition().getName());
    ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());
    
    if (existingSi == null) {
        throw new ServiceBrokerInvalidParametersException("instance_id does not match service_id");
    }
    
    // Try sync creation first (V2 API)
    try {
        CreateServiceKeyResponse response = client.serviceKeys()
            .create(CreateServiceKeyRequest.builder()
                .serviceInstanceId(existingSi.getId())
                .parameters(request.getParameters())
                .name(request.getBindingId())
                .build())
            .block();
        
        return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
            .credentials(response.getEntity().getCredentials())
            .async(false)
            .build());
    } catch (Exception e) {
        if (!isExceptionReportingAsyncRequired(e)) {
            throw redactExceptionAndWrapAsServiceBrokerException(e);
        }
    }
    
    // Async creation (V3 API)
    CreateServiceBindingResponse response = client.serviceBindingsV3()
        .create(CreateServiceBindingRequest.builder()
            .relationships(ServiceBindingRelationships.builder()
                .serviceInstance(ToOneRelationship.builder()
                    .data(Relationship.builder().id(existingSi.getId()).build())
                    .build())
                .build())
            .type(ServiceBindingType.KEY)
            .parameters(request.getParameters())
            .name(request.getBindingId())
            .build())
        .block();
    
    return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
        .async(true)
        .operation(toJson(new CmdbOperationState(response.getJobId().get(), OsbOperation.CREATE)))
        .build());
}
```
</details>

<details>
<summary><b>Go Implementation (internal/broker/bind.go)</b></summary>

```go
func (b *Broker) Bind(
    ctx context.Context,
    instanceID, bindingID string,
    details domain.BindDetails,
    asyncAllowed bool,
) (domain.Binding, error) {
    
    log := slog.With("instanceID", instanceID, "bindingID", bindingID)
    log.Info("creating service binding")
    
    // Get backing service instance
    serviceDef, err := b.catalog.GetService(ctx, details.ServiceID)
    if err != nil {
        return domain.Binding{}, err
    }
    
    backingInstance, err := b.cf.GetServiceInstanceByName(ctx, serviceDef.Name, truncateName(instanceID, 50))
    if err != nil {
        return domain.Binding{}, apiresponses.NewFailureResponse(
            fmt.Errorf("backing service instance not found"),
            http.StatusBadRequest,
            "bind-instance-not-found",
        )
    }
    
    // Create service credential binding (V3 API)
    binding, jobGUID, err := b.cf.CreateServiceCredentialBinding(ctx, &cfclient.CreateBindingRequest{
        Name:                bindingID,
        ServiceInstanceGUID: backingInstance.GUID,
        Type:                "key",
        Parameters:          details.RawParameters,
    })
    if err != nil {
        return domain.Binding{}, errors.RedactError(err)
    }
    
    // Async binding
    if jobGUID != "" {
        if !asyncAllowed {
            return domain.Binding{}, apiresponses.ErrAsyncRequired
        }
        opState := OperationState{
            JobGUID:   jobGUID,
            Operation: OperationCreate,
        }
        return domain.Binding{
            IsAsync:       true,
            OperationData: opState.Encode(),
        }, nil
    }
    
    // Sync binding - get credentials
    creds, err := b.cf.GetServiceBindingDetails(ctx, binding.GUID)
    if err != nil {
        return domain.Binding{}, errors.RedactError(err)
    }
    
    return domain.Binding{
        IsAsync:     false,
        Credentials: creds,
    }, nil
}
```
</details>

### 3. Dynamic Catalog

<details>
<summary><b>Java Implementation (DynamicCatalogServiceImpl.java:49-68)</b></summary>

```java
@Override
public List<ServiceDefinition> fetchServiceDefinitions() {
    final Mono<String> finalSpaceId = getOperationsForOrgAndSpace()
        .cast(DefaultCloudFoundryOperations.class)
        .flatMap(DefaultCloudFoundryOperations::getSpaceId);
    
    return Mono.zip(this.cloudFoundryClient, finalSpaceId)
        .flatMapMany(function((cloudFoundryClient, spaceId) -> 
            requestListServices(cloudFoundryClient, spaceId)
                .map(resource -> Tuples.of(cloudFoundryClient, resource))))
        .flatMap(function((cloudFoundryClient, resource) -> Mono.zip(
            Mono.just(resource),
            getServicePlans(cloudFoundryClient, ResourceUtils.getId(resource)))))
        .filter(t -> filterServiceEntity(t.getT1().getEntity()))
        .map(function((resource, servicePlans) -> 
            serviceDefinitionMapper.toServiceDefinition(resource, servicePlans)))
        .collectList()
        .block();
}
```
</details>

<details>
<summary><b>Go Implementation (internal/catalog/dynamic.go)</b></summary>

```go
type DynamicCatalog struct {
    cf            *cfclient.Client
    config        *config.Config
    mapper        *ServiceMapper
    mu            sync.RWMutex
    services      []domain.Service
    lastRefresh   time.Time
}

func (c *DynamicCatalog) Refresh(ctx context.Context) error {
    log := slog.With("space", c.config.DefaultSpace, "org", c.config.DefaultOrg)
    log.Info("refreshing catalog from CF marketplace")
    
    // Get space GUID
    spaceGUID, err := c.cf.GetSpaceGUID(ctx, c.config.DefaultOrg, c.config.DefaultSpace)
    if err != nil {
        return fmt.Errorf("failed to get space: %w", err)
    }
    
    // List all service offerings in the space
    offerings, err := c.cf.ServiceOfferings.ListAll(ctx, &client.ServiceOfferingListOptions{
        SpaceGUIDs: client.Filter{Values: []string{spaceGUID}},
    })
    if err != nil {
        return fmt.Errorf("failed to list services: %w", err)
    }
    
    var services []domain.Service
    for _, offering := range offerings {
        if c.shouldExclude(offering) {
            continue
        }
        
        plans, err := c.cf.ServicePlans.ListAll(ctx, &client.ServicePlanListOptions{
            ServiceOfferingGUIDs: client.Filter{Values: []string{offering.GUID}},
        })
        if err != nil {
            log.Warn("failed to get plans", "service", offering.Name, "error", err)
            continue
        }
        
        services = append(services, c.mapper.MapService(offering, plans))
    }
    
    c.mu.Lock()
    c.services = services
    c.lastRefresh = time.Now()
    c.mu.Unlock()
    
    log.Info("catalog refreshed", "serviceCount", len(services))
    return nil
}

func (c *DynamicCatalog) StartPeriodicRefresh(ctx context.Context) {
    ticker := time.NewTicker(c.config.CatalogRefreshInterval)
    defer ticker.Stop()
    
    for {
        select {
        case <-ctx.Done():
            return
        case <-ticker.C:
            if err := c.Refresh(ctx); err != nil {
                slog.Error("catalog refresh failed", "error", err)
            }
        }
    }
}
```
</details>

### 4. Metadata Formatter (CF Context)

<details>
<summary><b>Java Implementation (CfMetadataFormatter.java:23-39)</b></summary>

```java
@Override
protected void setLabelsAndAnnotations(Map<String, Object> properties, 
    Map<String, String> annotations, Map<String, String> labels, String prefix) {
    
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
        String key = entry.getKey();
        key = restoreOriginalOsbContextKeyNames(key);
        String prefixedKey = "brokered_service_" + prefix + "_" + key;
        Object entryValue = entry.getValue();
        String value = serializeNonStringValueToJson(key, entryValue);
        
        if (isContextKeyImmutableToQualifyAsALabel(key)) {
            labels.put(prefixedKey, value);
        } else {
            annotations.put(prefixedKey, value);
        }
    }
    labels.putAll(extractPrecedentedAnnotations(properties));
}

private boolean isContextKeyImmutableToQualifyAsALabel(String key) {
    return key.contains("_guid") || key.contains("_id");
}
```
</details>

<details>
<summary><b>Go Implementation (internal/metadata/cloudfoundry.go)</b></summary>

```go
type CloudFoundryFormatter struct{}

func (f *CloudFoundryFormatter) Format(ctx context.Context, rawContext json.RawMessage, instanceID string) *Metadata {
    var cfContext CloudFoundryContext
    if err := json.Unmarshal(rawContext, &cfContext); err != nil {
        slog.Warn("failed to parse CF context", "error", err)
        return &Metadata{}
    }
    
    labels := make(map[string]*string)
    annotations := make(map[string]*string)
    
    // Always set the brokered service instance GUID as a label
    labels[LabelBrokeredServiceInstanceGUID] = ptr(instanceID)
    
    properties := map[string]any{
        "organization_guid": cfContext.OrganizationGUID,
        "space_guid":        cfContext.SpaceGUID,
        "user_id":           cfContext.UserID,
        "organization_name": cfContext.OrganizationName,
        "space_name":        cfContext.SpaceName,
        "instance_name":     cfContext.InstanceName,
    }
    
    for key, value := range properties {
        if value == nil || value == "" {
            continue
        }
        
        prefixedKey := fmt.Sprintf("brokered_service_context_%s", key)
        strValue := toString(value)
        
        if isImmutableKey(key) {
            labels[prefixedKey] = &strValue
        } else {
            annotations[prefixedKey] = &strValue
        }
    }
    
    f.extractOrangeAnnotations(cfContext.InstanceAnnotations, labels)
    
    return &Metadata{Labels: labels, Annotations: annotations}
}

func isImmutableKey(key string) bool {
    return strings.Contains(key, "_guid") || strings.Contains(key, "_id")
}
```
</details>

### 5. Operation State (Async Tracking)

<details>
<summary><b>Java Implementation</b></summary>

```java
// OsbCmdbServiceInstance.java (inner class)
static class CmdbOperationState {
    private String backingCfServiceInstanceGuid;
    private OsbOperation operationType;
    
    @JsonCreator
    public CmdbOperationState(
        @JsonProperty("backing_cf_service_instance_guid") String backingCfServiceInstanceGuid,
        @JsonProperty("operation") OsbOperation operationType) {
        this.backingCfServiceInstanceGuid = backingCfServiceInstanceGuid;
        this.operationType = operationType;
    }
    // getters...
}

enum OsbOperation { CREATE, UPDATE, DELETE }

private String toJson(CmdbOperationState state) {
    try {
        return OBJECT_MAPPER.writeValueAsString(state);
    } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
    }
}
```
</details>

<details>
<summary><b>Go Implementation (internal/broker/operation_state.go)</b></summary>

```go
type Operation string

const (
    OperationCreate Operation = "CREATE"
    OperationUpdate Operation = "UPDATE"
    OperationDelete Operation = "DELETE"
)

type OperationState struct {
    BackingServiceGUID string    `json:"backing_cf_service_instance_guid,omitempty"`
    JobGUID            string    `json:"job_guid,omitempty"`
    Operation          Operation `json:"operation"`
}

func (s *OperationState) Encode() string {
    data, _ := json.Marshal(s)
    return string(data)
}

func DecodeOperationState(data string) (*OperationState, error) {
    var state OperationState
    if err := json.Unmarshal([]byte(data), &state); err != nil {
        return nil, fmt.Errorf("invalid operation state: %w", err)
    }
    return &state, nil
}
```
</details>

---

## Configuration Migration

### Java Configuration (application.yml)

```yaml
spring:
  cloud:
    appbroker:
      deployer:
        cloudfoundry:
          api-host: api.cf.example.com
          username: admin
          password: secret
          default-org: system
          default-space: osb-cmdb
  security:
    user:
      name: broker
      password: broker-secret

osbcmdb:
  admin:
    user: admin
    password: admin-secret
  dynamic-catalog:
    enabled: true
    suffix: "-cmdb"
    excludeBrokerNamesRegex: ".*-test"
```

### Go Configuration (Environment Variables)

```go
// internal/config/config.go
type Config struct {
    // Cloud Foundry connection
    CFAPIHost      string `envconfig:"CF_API_HOST" required:"true"`
    CFUsername     string `envconfig:"CF_USERNAME"`
    CFPassword     string `envconfig:"CF_PASSWORD"`
    CFClientID     string `envconfig:"CF_CLIENT_ID"`
    CFClientSecret string `envconfig:"CF_CLIENT_SECRET"`
    CFSkipSSL      bool   `envconfig:"CF_SKIP_SSL_VALIDATION" default:"false"`
    
    // Target org/space for backing services
    DefaultOrg   string `envconfig:"CF_DEFAULT_ORG" required:"true"`
    DefaultSpace string `envconfig:"CF_DEFAULT_SPACE" required:"true"`
    
    // Broker authentication
    BrokerUsername string `envconfig:"BROKER_USERNAME" required:"true"`
    BrokerPassword string `envconfig:"BROKER_PASSWORD" required:"true"`
    
    // Admin authentication
    AdminUsername string `envconfig:"ADMIN_USERNAME"`
    AdminPassword string `envconfig:"ADMIN_PASSWORD"`
    
    // Catalog settings
    CatalogRefreshInterval time.Duration `envconfig:"CATALOG_REFRESH_INTERVAL" default:"5m"`
    ServiceSuffix          string        `envconfig:"SERVICE_SUFFIX"`
    ExcludedServices       []string      `envconfig:"EXCLUDED_SERVICES"`
    
    // Server
    Port int `envconfig:"PORT" default:"8080"`
}
```

### Environment Variable Mapping

| Java Property | Go Environment Variable |
|--------------|------------------------|
| `spring.cloud.appbroker.deployer.cloudfoundry.api-host` | `CF_API_HOST` |
| `spring.cloud.appbroker.deployer.cloudfoundry.username` | `CF_USERNAME` |
| `spring.cloud.appbroker.deployer.cloudfoundry.password` | `CF_PASSWORD` |
| `spring.cloud.appbroker.deployer.cloudfoundry.default-org` | `CF_DEFAULT_ORG` |
| `spring.cloud.appbroker.deployer.cloudfoundry.default-space` | `CF_DEFAULT_SPACE` |
| `spring.security.user.name` | `BROKER_USERNAME` |
| `spring.security.user.password` | `BROKER_PASSWORD` |
| `osbcmdb.admin.user` | `ADMIN_USERNAME` |
| `osbcmdb.admin.password` | `ADMIN_PASSWORD` |

---

## Error Handling Patterns

### Java Exception Mapping

| Java Exception | HTTP Status | OSB Error Code |
|---------------|-------------|----------------|
| `ServiceInstanceDoesNotExistException` | 404/410 | - |
| `ServiceInstanceExistsException` | 409 | - |
| `ServiceBrokerInvalidParametersException` | 400 | - |
| `ServiceBrokerConcurrencyException` | 422 | `ConcurrencyError` |
| `ServiceBrokerMaintenanceInfoConflictException` | 422 | `MaintenanceInfoConflict` |
| `ServiceBrokerException` | 500 | - |

### Go Error Mapping

```go
// internal/errors/redact.go
var urlRegex = regexp.MustCompile(`(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*`)

func RedactURLs(message string) string {
    return urlRegex.ReplaceAllString(message, "redacted-url")
}

func WrapAsOSBError(err error) error {
    redacted := RedactURLs(err.Error())
    return apiresponses.NewFailureResponse(
        errors.New(redacted),
        http.StatusInternalServerError,
        "cf-api-error",
    )
}

// Use brokerapi predefined errors
var (
    ErrInstanceAlreadyExists    = apiresponses.ErrInstanceAlreadyExists    // 409
    ErrInstanceDoesNotExist     = apiresponses.ErrInstanceDoesNotExist     // 410
    ErrConcurrentInstanceAccess = apiresponses.ErrConcurrentInstanceAccess // 422
    ErrMaintenanceInfoConflict  = apiresponses.ErrMaintenanceInfoConflict  // 422
)
```

### CF Error Detection

```go
func IsAsyncRequiredError(err error) bool {
    var cfErr *resource.CloudFoundryError
    if errors.As(err, &cfErr) {
        return cfErr.Code == 10001 // CF-AsyncRequired
    }
    return false
}
```

---

## Testing Strategy

### Java Test Categories

| Category | Location | Framework |
|----------|----------|-----------|
| Unit Tests | `osb-cmdb/src/test/java/.../` | JUnit 5 + Mockito |
| Component Tests | `osb-cmdb/src/test/java/.../integration/` | Spring Boot Test + WireMock |
| Acceptance Tests | `spring-cloud-app-broker-acceptance-tests/` | JUnit 5 + Real CF |

### Go Test Strategy

| Category | Location | Framework | Notes |
|----------|----------|-----------|-------|
| Unit Tests | `internal/*/..._test.go` | `testing` + `testify` | Table-driven tests |
| Integration Tests | `test/integration/` | `testing` + `httptest` | Mock CF server |
| Mocks | `test/mocks/` | `testify/mock` | Interface mocks |

### Go Test Example

```go
// internal/broker/provision_test.go
func TestBroker_Provision(t *testing.T) {
    tests := []struct {
        name           string
        instanceID     string
        details        domain.ProvisionDetails
        asyncAllowed   bool
        cfMockSetup    func(*mocks.CFClient)
        expectedResult domain.ProvisionedServiceSpec
        expectedError  error
    }{
        {
            name:       "sync provision success",
            instanceID: "test-instance-1",
            details: domain.ProvisionDetails{
                ServiceID: "service-1",
                PlanID:    "plan-1",
            },
            asyncAllowed: true,
            cfMockSetup: func(m *mocks.CFClient) {
                m.On("CreateServiceInstance", mock.Anything, mock.Anything).
                    Return(&resource.ServiceInstance{GUID: "backing-guid"}, "", nil)
            },
            expectedResult: domain.ProvisionedServiceSpec{IsAsync: false},
        },
        {
            name:       "async provision",
            instanceID: "test-instance-2",
            details: domain.ProvisionDetails{
                ServiceID: "service-1",
                PlanID:    "plan-1",
            },
            asyncAllowed: true,
            cfMockSetup: func(m *mocks.CFClient) {
                m.On("CreateServiceInstance", mock.Anything, mock.Anything).
                    Return(&resource.ServiceInstance{GUID: "backing-guid"}, "job-123", nil)
            },
            expectedResult: domain.ProvisionedServiceSpec{
                IsAsync:       true,
                OperationData: `{"backing_cf_service_instance_guid":"backing-guid","operation":"CREATE"}`,
            },
        },
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            cfMock := new(mocks.CFClient)
            tt.cfMockSetup(cfMock)
            
            broker := NewBroker(cfMock, nil, nil)
            result, err := broker.Provision(context.Background(), tt.instanceID, tt.details, tt.asyncAllowed)
            
            if tt.expectedError != nil {
                assert.ErrorIs(t, err, tt.expectedError)
            } else {
                assert.NoError(t, err)
                assert.Equal(t, tt.expectedResult.IsAsync, result.IsAsync)
            }
        })
    }
}
```

---

## Deployment Artifacts

### Dockerfile

```dockerfile
# Build stage
FROM golang:1.22-alpine AS builder
WORKDIR /app
ARG VERSION=dev
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build \
    -ldflags="-s -w -X main.version=${VERSION}" \
    -o osb-cmdb ./cmd/osb-cmdb

# Runtime stage
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=builder /app/osb-cmdb /
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/osb-cmdb"]
```

### Helm Chart (values.yaml)

```yaml
replicaCount: 1

image:
  repository: ghcr.io/orange-cloudfoundry/osb-cmdb-go
  pullPolicy: IfNotPresent
  tag: ""

cf:
  apiHost: ""
  username: ""
  password: ""
  defaultOrg: ""
  defaultSpace: ""

broker:
  username: "broker"
  password: ""

catalog:
  refreshInterval: "5m"
  serviceSuffix: ""
```

### GitHub Actions (release.yml)

```yaml
name: Release

on:
  push:
    branches: [main]
    paths: ['osb-cmdb-go/**']
    tags: ['v*']

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          context: osb-cmdb-go
          push: true
          tags: ghcr.io/orange-cloudfoundry/osb-cmdb-go:${{ github.ref_name }}
```

---

## References

### Open Service Broker API

- **Specification**: https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md
- **API Version**: 2.14+
- **Profile (CF/K8S contexts)**: https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md

### Go Libraries

| Library | Documentation | Repository |
|---------|---------------|------------|
| brokerapi | [pkg.go.dev](https://pkg.go.dev/github.com/pivotal-cf/brokerapi/v11) | [GitHub](https://github.com/pivotal-cf/brokerapi) |
| go-cfclient | [pkg.go.dev](https://pkg.go.dev/github.com/cloudfoundry/go-cfclient/v3) | [GitHub](https://github.com/cloudfoundry/go-cfclient) |
| envconfig | [pkg.go.dev](https://pkg.go.dev/github.com/kelseyhightower/envconfig) | [GitHub](https://github.com/kelseyhightower/envconfig) |
| testify | [pkg.go.dev](https://pkg.go.dev/github.com/stretchr/testify) | [GitHub](https://github.com/stretchr/testify) |
| chi | [pkg.go.dev](https://pkg.go.dev/github.com/go-chi/chi/v5) | [GitHub](https://github.com/go-chi/chi) |
| slog | [pkg.go.dev](https://pkg.go.dev/log/slog) | (standard library) |

### Cloud Foundry

- **CF API v3 Documentation**: https://v3-apidocs.cloudfoundry.org/
- **CF API v2 Documentation**: https://apidocs.cloudfoundry.org/ (deprecated)
- **go-cfclient Examples**: https://github.com/cloudfoundry/go-cfclient/tree/main/examples

### Java (Original Implementation)

- **Spring Cloud Open Service Broker**: https://spring.io/projects/spring-cloud-open-service-broker
- **cf-java-client**: https://github.com/cloudfoundry/cf-java-client

### Container & Kubernetes

- **OCI Helm Charts**: https://helm.sh/docs/topics/registries/
- **Distroless Images**: https://github.com/GoogleContainerTools/distroless
- **GitHub Container Registry**: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry

---

## Appendix: Pattern Migration Summary

| Java Pattern | Go Equivalent |
|-------------|---------------|
| `Mono<T>.block()` | Direct function return |
| `@Autowired` | Constructor injection |
| `@ConfigurationProperties` | `envconfig` struct tags |
| `reactor.util.Logger` | `log/slog` |
| Jackson `ObjectMapper` | `encoding/json` |
| Checked exceptions | `error` return value |
| `ServiceInstanceService` interface | `domain.ServiceBroker` interface |
| Spring Security | brokerapi middleware |
| Spring Boot Actuator | Custom `/health`, `/ready` endpoints |
| WireMock | `httptest.Server` |
| JUnit 5 + Mockito | `testing` + `testify` |
