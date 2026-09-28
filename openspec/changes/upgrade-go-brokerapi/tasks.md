## 1. Dependency and Source Migration

- [x] 1.1 Change the Go directive in `osb-cmdb-go/go.mod` to Go 1.25.0.
- [x] 1.2 Replace the direct BrokerAPI dependency with `code.cloudfoundry.org/brokerapi/v13 v13.0.25`.
- [x] 1.3 Replace all production-code imports of `github.com/pivotal-cf/brokerapi/v11` with the BrokerAPI v13 module path.
- [x] 1.4 Replace all unit-test, integration-test, and mock imports with the BrokerAPI v13 module path.
- [x] 1.5 Run `go mod tidy` with Go 1.25 and commit the resulting module graph and checksums.

## 2. Build Environment and Documentation

- [x] 2.1 Update the Docker builder image to `golang:1.25-alpine`.
- [x] 2.2 Update every GitHub Actions Go setup step to Go 1.25.
- [x] 2.3 Update README prerequisites and migration documentation to reference Go 1.25 and `code.cloudfoundry.org/brokerapi/v13`.
- [x] 2.4 Update any remaining repository references to the old BrokerAPI module path or Go 1.23 baseline.

## 3. Compatibility Verification

- [x] 3.1 Run `gofmt` on changed Go files.
- [x] 3.2 Run `go build ./...` using Go 1.25.
- [x] 3.3 Run `go vet ./...` using Go 1.25.
- [x] 3.4 Run `go test ./...` using Go 1.25; document sandbox-only loopback failures separately from code failures if they recur.
- [x] 3.5 Run `golangci-lint run ./...`.
- [x] 3.6 Build the Docker image and verify the Go 1.25 builder stage resolves dependencies successfully.
- [x] 3.7 Run `openspec validate upgrade-go-brokerapi --strict`.

## Verification Notes

- `go build ./...`, `go vet ./...`, and `golangci-lint run ./...` pass with the installed Go 1.27 toolchain, which satisfies the Go 1.25 module baseline.
- `go test ./...` passes for all non-networked packages. `test/integration` cannot run in this sandbox because every `httptest` loopback connection to `127.0.0.1` is refused; the same limitation existed before this upgrade and must be verified in a network-enabled environment.
- Docker build verification passed with the `golang:1.25-alpine` builder image.
