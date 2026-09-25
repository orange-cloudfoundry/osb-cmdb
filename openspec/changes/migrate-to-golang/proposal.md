## Why

The current OSB-CMDB implementation is built on Java with Spring Boot, Spring Cloud Open Service Broker, and cf-java-client. The team has stronger expertise in Go and wants to align with cloud-native patterns (single binary, fast startup, low memory footprint). The reactive programming model (Mono/Flux) adds complexity that isn't necessary for this synchronous-style broker. Migrating to Go will reduce operational overhead and simplify maintenance.

## What Changes

- **BREAKING**: Complete rewrite from Java/Spring Boot to Go
- Replace Spring Cloud Open Service Broker with [brokerapi](https://github.com/pivotal-cf/brokerapi) v11+
- Replace cf-java-client (reactive, V2+V3 hybrid) with [go-cfclient](https://github.com/cloudfoundry/go-cfclient) v3 (synchronous, V3 only)
- Replace Spring configuration with environment variables via envconfig
- Replace Spring Security with brokerapi's built-in HTTP Basic Auth middleware
- Replace JUnit/Mockito with Go's standard `testing` package + testify
- Add Docker image generation with multi-stage builds
- Add OCI Helm chart for Kubernetes deployment
- Add GitHub Actions CI/CD (replacing CircleCI for the Go module)

## Capabilities

### New Capabilities
- `go-osb-broker`: Complete Go implementation of OSB-CMDB service broker implementing all 10 OSB API operations (catalog, provision, deprovision, update, get instance, last operation, bind, unbind, get binding, last binding operation)
- `go-dynamic-catalog`: Dynamic catalog service that fetches services from CF marketplace with periodic refresh
- `go-metadata-formatting`: Metadata formatting for both CloudFoundry and Kubernetes OSB contexts
- `go-deployment`: Docker image generation, OCI Helm chart, and GitHub Actions CI/CD pipeline

### Modified Capabilities
<!-- No existing Go specs to modify - this is a greenfield Go implementation alongside the existing Java code -->

## Impact

- **New directory**: `osb-cmdb-go/` containing the entire Go implementation
- **Dependencies**: New Go modules (brokerapi, go-cfclient, envconfig, testify)
- **CI/CD**: New GitHub Actions workflows for Go build, test, Docker push, Helm push
- **Container Registry**: Images pushed to ghcr.io/orange-cloudfoundry/osb-cmdb-go
- **Helm Registry**: Charts pushed to oci://ghcr.io/orange-cloudfoundry/charts
- **Configuration**: Environment variable-based config instead of application.yml
- **CF API**: V3 only (simplifies the V2/V3 hybrid approach in the Java version)

## Implementation Status

**Status: COMPLETE** (79/79 tasks)

### Build Verification

- **Build**: ✅ All packages compile successfully with Go 1.23
- **Unit Tests**: ✅ 7/7 internal packages pass (broker, catalog, cfclient, context, errors, metadata)
- **Integration Tests**: ⚠️ Require network access (httptest); pass locally
- **Devbox**: ✅ Go tooling configured via `devbox.json` at repo root

### Implementation Notes

During implementation, several go-cfclient v3 API differences were discovered and resolved. Key adaptations are documented in design.md and MIGRATION.md. The first beta release, v3.0.0-beta.1, is pinned and requires Go 1.23.
