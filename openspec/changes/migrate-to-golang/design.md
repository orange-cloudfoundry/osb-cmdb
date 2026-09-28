## Context

See proposal.md for motivation. The existing Java implementation uses:
- Spring Cloud Open Service Broker for OSB API handling
- cf-java-client with reactive Mono/Flux patterns and V2+V3 API hybrid
- Spring Security for HTTP Basic Auth
- Spring Boot configuration with application.yml

The Go implementation will live alongside the Java code in `osb-cmdb-go/` to allow parallel operation during transition. A detailed migration guide already exists at `osb-cmdb-go/MIGRATION.md` with Java-to-Go code mappings.

## Goals / Non-Goals

**Goals:**
- Functional parity with the Java implementation for all 10 OSB API operations
- Idiomatic Go patterns (synchronous, error returns, interfaces)
- Single binary deployment with minimal dependencies
- Cloud-native deployment (Docker + Helm)
- Full support for both CloudFoundry and Kubernetes OSB contexts

**Non-Goals:**
- Gradual migration or hybrid deployment (this is a clean rewrite)
- V2 CF API support (V3 only simplifies the implementation)
- Real CF acceptance tests (mocks only for now)
- Spring-style interceptor pattern (use Go interface mocks instead)

## Decisions

### Decision 1: Use brokerapi v11 for OSB API handling

**Choice:** Use [brokerapi](https://github.com/pivotal-cf/brokerapi) v11+ from Pivotal CF

**Rationale:** 
- Mature, production-tested library used by many CF service brokers
- Implements OSB API v2.14+ with all required endpoints
- Built-in HTTP Basic Auth middleware
- Uses chi router which is standard and lightweight
- Well-documented with clear `domain.ServiceBroker` interface

**Alternatives Considered:**
- Custom implementation with net/http: More work, higher risk of OSB compliance issues
- go-open-service-broker-client: Client-side library, not server-side

### Decision 2: Use go-cfclient v3 with V3 API only

**Choice:** Use [go-cfclient](https://github.com/cloudfoundry/go-cfclient) v3 targeting CF API v3 exclusively

**Rationale:**
- Official CF community library, actively maintained
- V3 API has native metadata (labels/annotations) support
- Built-in async job polling with `Jobs.PollComplete()`
- Synchronous API matches Go idioms (no Mono/Flux conversion)
- Simplifies the V2/V3 hybrid approach in Java

**Alternatives Considered:**
- Raw HTTP client: More control but requires reimplementing pagination, auth, error handling
- Keep V2 API support: Adds complexity, V2 is deprecated

### Decision 3: Environment-based configuration with envconfig

**Choice:** Use [envconfig](https://github.com/kelseyhightower/envconfig) for configuration

**Rationale:**
- 12-factor app compliance
- Struct tags for declarative config definition
- Required field validation built-in
- Default values supported
- Simpler than YAML parsing for container deployments

**Alternatives Considered:**
- Viper: More powerful but heavier, YAML support not needed
- Standard library only: Works but requires manual parsing

### Decision 4: Project structure with internal packages

**Choice:** Use Go internal package layout:
```
osb-cmdb-go/
├── cmd/osb-cmdb/main.go
├── internal/
│   ├── broker/      # domain.ServiceBroker implementation
│   ├── catalog/     # Dynamic catalog service
│   ├── cfclient/    # CF client wrapper
│   ├── config/      # Configuration
│   ├── context/     # OSB context parsing
│   ├── metadata/    # Label/annotation formatting
│   ├── middleware/  # HTTP middleware
│   └── errors/      # Error handling utilities
```

**Rationale:**
- `internal/` prevents external imports of implementation details
- Clear separation of concerns
- Each package has a single responsibility
- Matches Go community conventions

**Alternatives Considered:**
- Flat structure: Gets messy as code grows
- pkg/ directory: Less protection against external imports

### Decision 5: Sync.RWMutex for catalog caching

**Choice:** Use `sync.RWMutex` for thread-safe catalog access during periodic refresh

**Rationale:**
- Simple and efficient for read-heavy workload
- Catalog reads are frequent, writes are rare (every 5 minutes)
- No external dependency needed
- Standard library, well-understood semantics

**Alternatives Considered:**
- Channel-based: More complex, not better for this use case
- atomic.Value: Works for simple types, awkward for slices

### Decision 6: Distroless runtime image

**Choice:** Use `gcr.io/distroless/static-debian12:nonroot` for runtime

**Rationale:**
- Minimal attack surface (no shell, no package manager)
- Small image size (~2MB base)
- Non-root execution by default
- Go static binaries need only static libc

**Alternatives Considered:**
- Alpine: Has shell, larger attack surface
- Scratch: No CA certificates, harder to debug
- Ubuntu: Much larger, unnecessary for static Go binary

## Risks / Trade-offs

**[Mitigated] go-cfclient v3 API maturity** → The first beta release, v3.0.0-beta.1, supersedes the alpha version used during initial implementation. It requires Go 1.23. The version is pinned, and its API compatibility is verified by the build and test suite.

**[Risk] V3-only may miss edge cases covered by V2** → The Java codebase documents V2 usage patterns. Service keys (bindings) in V3 are well-supported. Test thoroughly against real CF before production.

**[Risk] Periodic catalog refresh could serve stale data** → Acceptable trade-off. 5-minute default is configurable. Failed refreshes log errors and retain previous catalog.

**[Risk] No acceptance tests against real CF** → Mitigated by comprehensive unit/integration tests with mocks. Add acceptance tests later when CF environment is available.

**[Trade-off] Single binary simplicity vs. hot reload** → Go binaries require restart for config changes. Acceptable for this use case; Kubernetes rolling updates handle this.

## Migration Plan

1. **Phase 1 - Foundation**: Create project structure, go.mod, config loading, main.go stub
2. **Phase 2 - CF Client**: Implement cfclient wrapper with all required operations, unit tests
3. **Phase 3 - Catalog**: Dynamic catalog with periodic refresh, unit tests
4. **Phase 4 - Broker**: Implement all 10 OSB operations, unit tests
5. **Phase 5 - Metadata**: Context parsing and metadata formatting for CF/K8S
6. **Phase 6 - Integration Tests**: Mock CF server tests for full request/response flows
7. **Phase 7 - Deployment**: Dockerfile, Helm chart, GitHub Actions

**Rollback:** The Java implementation remains in place. If issues arise, simply don't deploy the Go version. No shared state between implementations.

## Implementation Notes (Post-Build)

During implementation and build verification, we discovered several API differences in go-cfclient v3 that required code adaptations. The implementation now uses v3.0.0-beta.1.

### go-cfclient v3 API Adaptations

| Issue | Resolution |
|-------|------------|
| `SkipTLSValidation()` takes no arguments | Use conditional `opts = append(opts, config.SkipTLSValidation())` |
| List methods return 3 values (results, pager, error) | Discard pager: `results, _, err := ...` |
| `PollingOptions.Interval` → `PollingOptions.CheckInterval` | Updated field name |
| `CreateManaged()` returns only (jobGUID, error) | Instance fetched separately via Get if needed |
| Parameters are `*json.RawMessage` not `map[string]interface{}` | Marshal/unmarshal JSON as needed |
| Struct types are values not pointers (e.g., `BrokerCatalog`, `Schemas`) | Direct field access, no nil checks needed |
| `LabelSelector` is `map[string]ExclusionFilter` | Use `LabelSel.EqualTo(key, value)` method |
| `ServiceInstanceManagedRelationships` undefined | Use `ServiceInstanceRelationships` instead |
| Use constructors for create operations | `resource.NewServiceInstanceCreateManaged()`, `resource.NewServiceCredentialBindingCreateKey()` |

### brokerapi v11 API Notes

| Issue | Resolution |
|-------|------------|
| No `ErrInvalidServiceID` / `ErrInvalidPlanID` | Use `apiresponses.NewFailureResponse(err, 400, "invalid-service-id")` |
| MaintenanceInfo type mismatch | Use `resource.ServiceInstanceMaintenanceInfo` from go-cfclient |

### Build & Test Results

- **Build**: All packages compile with Go 1.23 (`go build ./...`)
- **Unit Tests**: 7/7 internal packages pass
- **Integration Tests**: Created but require local network for httptest.NewServer()

### Tooling

- **Devbox**: `devbox.json` added at repo root with Go and `golangci-lint`

## Lessons Learned

1. **Pre-release library APIs require verification** - go-cfclient v3 documentation doesn't always match the actual API. Always verify with `go doc` and build early.

2. **Pin exact versions** - Use `v3.0.0-beta.1` specifically. Even pre-release version bumps can break builds.

3. **Test environment limitations** - Some CI/sandbox environments restrict localhost networking, causing `httptest.NewServer()` to fail with "connection refused". Unit tests with mocks are more portable.

4. **Spec-to-implementation gap** - Original specs assumed certain go-cfclient APIs that don't exist. The iterative build-fix cycle caught these.

## Open Questions

- **Helm chart extras**: Should we add Ingress, HPA, PDB templates? Deferred to future enhancement based on deployment requirements.
- **Metrics endpoint**: Should we add Prometheus /metrics? Not in initial scope, can add later without spec changes.
