## Context

The Go implementation currently pins `github.com/pivotal-cf/brokerapi/v11 v11.0.0` and declares Go 1.23. BrokerAPI has moved to the `code.cloudfoundry.org/brokerapi/v13` module path. The target release, v13.0.25, declares `go 1.25.0` in its module metadata.

This is a dependency and toolchain migration. It must preserve the existing Open Service Broker endpoints, authentication, asynchronous operation tracking, and catalog behavior.

## Goals / Non-Goals

**Goals:**
- Use the maintained BrokerAPI v13.0.25 release and module path.
- Align every build environment with Go 1.25.
- Preserve existing broker behavior while proving source and runtime compatibility through the established verification suite.
- Keep the dependency diff limited to what the Go module resolver requires.

**Non-Goals:**
- Change the OSB API version, routes, or request/response behavior.
- Upgrade go-cfclient or unrelated direct dependencies.
- Add an application-level compatibility shim for the old BrokerAPI module path.
- Change deployment runtime images beyond the builder toolchain requirement.

## Decisions

### Decision 1: Adopt the official Cloud Foundry module path

**Choice:** Replace `github.com/pivotal-cf/brokerapi/v11` with `code.cloudfoundry.org/brokerapi/v13 v13.0.25` directly in `go.mod` and all imports.

**Rationale:** BrokerAPI v13 is published under the Cloud Foundry module path. A `replace` directive would obscure the real dependency, retain obsolete imports, and make downstream dependency resolution less predictable.

### Decision 2: Upgrade the entire supported toolchain baseline to Go 1.25

**Choice:** Declare `go 1.25.0`, build Docker images with `golang:1.25-alpine`, and use Go 1.25 in GitHub Actions jobs.

**Rationale:** BrokerAPI v13.0.25 explicitly requires Go 1.25.0. Supporting an older declared baseline would produce an invalid and untestable dependency contract.

### Decision 3: Treat compilation and regression tests as the compatibility gate

**Choice:** Keep existing broker implementation patterns unless the compiler or test suite identifies an actual v13 incompatibility.

**Rationale:** The v13 package retains the `brokerapi.New`, `domain.ServiceBroker`, `domain/apiresponses`, and middleware package surfaces used by this project. Minimal mechanical migration reduces regression risk while tests validate that assumption.

### Decision 4: Let Go recalculate the dependency graph

**Choice:** Run `go mod tidy` after the direct module and import migration, then commit the resulting `go.mod` and `go.sum` updates.

**Rationale:** BrokerAPI v13 has a different dependency graph. Hand-editing checksums risks stale or missing indirect requirements.

## Risks / Trade-offs

**[Risk] Toolchain availability**: Developers and CI images pinned to Go 1.23 will no longer build the module. The Dockerfile, CI configuration, and documentation are updated as part of the atomic upgrade.

**[Risk] Major-version API differences**: BrokerAPI v13 could expose compile-time differences not visible in the high-level interface. Build, vet, unit/integration tests, and Docker build verification are required before completion.

**[Trade-off] New transitive dependencies**: `go mod tidy` may materially update transitive checksums. This is accepted because the graph is owned by the selected BrokerAPI release.

## Migration Plan

1. Update the Go directive and direct BrokerAPI dependency.
2. Replace imports in application code, tests, integration tests, and mocks.
3. Run `go mod tidy` with Go 1.25.
4. Update Docker, CI, and documentation version references.
5. Run formatting, build, test, vet, lint, and Docker build validation.

**Rollback:** Revert the complete change, including the direct dependency and toolchain updates. Do not combine BrokerAPI v13 with the Go 1.23 build baseline.
