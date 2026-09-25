## 1. Project Foundation

- [x] 1.1 Create osb-cmdb-go/ directory structure with cmd/osb-cmdb/ and internal/ packages
- [x] 1.2 Initialize go.mod with module github.com/orange-cloudfoundry/osb-cmdb-go
- [x] 1.3 Add dependencies: brokerapi v11, go-cfclient v3, envconfig, testify
- [x] 1.4 Create internal/config/config.go with Config struct and envconfig tags
- [x] 1.5 Create cmd/osb-cmdb/main.go with config loading and HTTP server setup
- [x] 1.6 Add Makefile with build, test, lint targets

## 2. CF Client Wrapper

- [x] 2.1 Create internal/cfclient/client.go with Client struct wrapping go-cfclient
- [x] 2.2 Implement CreateServiceInstance with V3 API and job handling
- [x] 2.3 Implement DeleteServiceInstance with job handling
- [x] 2.4 Implement UpdateServiceInstance with job handling
- [x] 2.5 Implement GetServiceInstance and GetServiceInstanceByName
- [x] 2.6 Implement ListServiceInstancesByLabel for duplicate detection
- [x] 2.7 Implement CreateServiceCredentialBinding with job handling
- [x] 2.8 Implement DeleteServiceCredentialBinding with job handling
- [x] 2.9 Implement GetServiceBindingDetails for credentials
- [x] 2.10 Implement GetOrCreateSpace for backing space management
- [x] 2.11 Add unit tests for cfclient wrapper

## 3. Dynamic Catalog Service

- [x] 3.1 Create internal/catalog/service.go with CatalogService interface
- [x] 3.2 Create internal/catalog/dynamic.go with DynamicCatalog struct
- [x] 3.3 Implement Refresh() to fetch services from CF marketplace
- [x] 3.4 Create internal/catalog/mapper.go to map CF resources to OSB format
- [x] 3.5 Implement service name suffix support
- [x] 3.6 Implement service exclusion filtering
- [x] 3.7 Implement StartPeriodicRefresh() with configurable interval
- [x] 3.8 Add sync.RWMutex for thread-safe catalog access
- [x] 3.9 Add unit tests for catalog service

## 4. OSB Broker Implementation

- [x] 4.1 Create internal/broker/broker.go implementing domain.ServiceBroker interface
- [x] 4.2 Create internal/broker/operation_state.go with OperationState struct and Encode/Decode
- [x] 4.3 Implement Services() returning catalog
- [x] 4.4 Implement Provision() with sync/async handling and duplicate detection
- [x] 4.5 Implement Deprovision() with service key cleanup
- [x] 4.6 Implement Update() with plan change and maintenance info
- [x] 4.7 Implement GetInstance() with parameter fetching
- [x] 4.8 Implement LastOperation() with CF state to OSB state mapping
- [x] 4.9 Implement Bind() with sync/async handling
- [x] 4.10 Implement Unbind() with sync/async handling
- [x] 4.11 Implement GetBinding() for credential fetching
- [x] 4.12 Implement LastBindingOperation() with job state mapping
- [x] 4.13 Add unit tests for each broker operation

## 5. Context Parsing and Metadata Formatting

- [x] 5.1 Create internal/context/parser.go for X-Broker-API-Originating-Identity parsing
- [x] 5.2 Create internal/context/cloudfoundry.go with CloudFoundryContext struct
- [x] 5.3 Create internal/context/kubernetes.go with KubernetesContext struct
- [x] 5.4 Create internal/metadata/formatter.go with Formatter interface
- [x] 5.5 Create internal/metadata/cloudfoundry.go with CF label/annotation formatting
- [x] 5.6 Implement GUID properties as labels, mutable properties as annotations
- [x] 5.7 Implement orange.com/ annotation extraction and promotion to labels
- [x] 5.8 Create internal/metadata/kubernetes.go with K8S metadata formatting
- [x] 5.9 Add unit tests for context parsing and metadata formatting

## 6. Error Handling and Middleware

- [x] 6.1 Create internal/errors/redact.go with URL redaction regex
- [x] 6.2 Implement RedactError() wrapper for CF errors
- [x] 6.3 Create internal/middleware/apiinfo.go for X-Api-Info-Location validation
- [x] 6.4 Create internal/middleware/logging.go for request logging
- [x] 6.5 Add unit tests for error handling

## 7. Integration Tests

- [x] 7.1 Create test/mocks/ with mock CF client interface
- [x] 7.2 Create test/integration/provision_test.go with mock CF server
- [x] 7.3 Create test/integration/deprovision_test.go
- [x] 7.4 Create test/integration/bind_test.go
- [x] 7.5 Create test/integration/catalog_test.go
- [x] 7.6 Test async operation flows with last_operation polling

## 8. Deployment Artifacts

- [x] 8.1 Create Dockerfile with multi-stage build and distroless runtime
- [x] 8.2 Create .dockerignore
- [x] 8.3 Create deploy/helm/osb-cmdb-go/Chart.yaml
- [x] 8.4 Create deploy/helm/osb-cmdb-go/values.yaml with all config options
- [x] 8.5 Create deploy/helm/osb-cmdb-go/templates/_helpers.tpl
- [x] 8.6 Create deploy/helm/osb-cmdb-go/templates/deployment.yaml
- [x] 8.7 Create deploy/helm/osb-cmdb-go/templates/service.yaml
- [x] 8.8 Create deploy/helm/osb-cmdb-go/templates/configmap.yaml
- [x] 8.9 Create deploy/helm/osb-cmdb-go/templates/secret.yaml
- [x] 8.10 Create deploy/helm/osb-cmdb-go/templates/serviceaccount.yaml
- [x] 8.11 Create deploy/helm/osb-cmdb-go/templates/NOTES.txt
- [x] 8.12 Test Helm chart with helm lint and helm template

## 9. CI/CD Workflows

- [x] 9.1 Create .github/workflows/go-ci.yml for build, test, lint on PRs
- [x] 9.2 Create .github/workflows/go-release.yml for Docker and Helm push
- [x] 9.3 Configure Docker metadata action for semantic version tags
- [x] 9.4 Configure Helm chart packaging and OCI push
- [x] 9.5 Test workflows in a PR (workflows created, will be tested on actual PR)

## 10. Documentation

- [x] 10.1 Create osb-cmdb-go/README.md with usage instructions
- [x] 10.2 Update MIGRATION.md if any implementation details changed
- [x] 10.3 Add configuration reference documentation

## 11. Runtime Wiring and Readiness

- [x] 11.1 Construct the CF client from the loaded application configuration
- [x] 11.2 Resolve the configured default CF space GUID during startup
- [x] 11.3 Create, initially refresh, and start the dynamic catalog service
- [x] 11.4 Construct the metadata formatter, context parser, and OSB broker with the resolved dependencies
- [x] 11.5 Mount the brokerapi handler with configured broker credentials under the application HTTP server
- [x] 11.6 Stop the dynamic catalog cleanly during graceful shutdown
- [x] 11.7 Make the readiness endpoint report startup and CF dependency readiness
- [x] 11.8 Add an executable wiring test that verifies the mounted OSB catalog route

## 12. Async Operation Correctness

- [x] 12.1 Extend operation state with the OSB instance ID for asynchronous provisioning
- [x] 12.2 Encode the CF create job GUID and OSB instance ID without dereferencing an unavailable backing instance during async provision
- [x] 12.3 Poll CF jobs for async provision, update, deprovision, bind, and unbind operations when operation data includes a job GUID
- [x] 12.4 Map concrete go-cfclient resource.Job states to OSB last-operation states
- [x] 12.5 Map concrete go-cfclient resource.LastOperation states to OSB last-operation states
- [x] 12.6 Add regression tests for async provision, including the nil backing-instance result
- [x] 12.7 Add tests for succeeded, failed, and in-progress job and instance operation states

## 13. Broker Semantics and Coverage

- [x] 13.1 Compare requested plan and parameters with the backing instance for duplicate provision idempotency
- [x] 13.2 Return an already-exists response only for matching duplicate provisions and a conflict for incompatible requests
- [x] 13.3 Parse and apply X-Broker-API-Originating-Identity when building CF metadata
- [x] 13.4 Add real-broker tests for provision, deprovision, update, bind, unbind, get, and last-operation behavior
- [x] 13.5 Add real-broker tests for Cloud Foundry, Kubernetes, missing, and unknown originating identities
- [x] 13.6 Make dynamic catalog start and stop lifecycle operations idempotent
- [x] 13.7 Add a Docker image build verification to CI
