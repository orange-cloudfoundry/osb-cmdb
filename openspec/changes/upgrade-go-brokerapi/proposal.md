## Why

The Go broker currently depends on the deprecated `github.com/pivotal-cf/brokerapi/v11` module. The maintained BrokerAPI release is `code.cloudfoundry.org/brokerapi/v13 v13.0.25`, which requires Go 1.25, so the dependency and every supported build environment must advance together.

## What Changes

- **BREAKING**: Raise the Go module, local development, CI, and Docker build baseline from Go 1.23 to Go 1.25.
- Replace `github.com/pivotal-cf/brokerapi/v11 v11.0.0` with `code.cloudfoundry.org/brokerapi/v13 v13.0.25`.
- Migrate all production, unit-test, integration-test, and mock imports to the BrokerAPI v13 module path.
- Regenerate module checksums and remove dependency graph entries no longer required after the migration.
- Update documentation to identify Go 1.25 and the Cloud Foundry BrokerAPI v13 module as the supported runtime dependencies.

## Capabilities

### New Capabilities

<!-- None. This change preserves the existing OSB API behavior. -->

### Modified Capabilities

<!-- None. This change does not alter requirements exposed by the broker. -->

## Impact

- `osb-cmdb-go/go.mod` and `go.sum`
- BrokerAPI imports throughout `osb-cmdb-go/`
- Docker builder image and GitHub Actions Go setup
- Developer documentation in `osb-cmdb-go/README.md` and `osb-cmdb-go/MIGRATION.md`
- Local developers and CI runners must provide Go 1.25 or newer
