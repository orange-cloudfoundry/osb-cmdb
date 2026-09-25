## 1. Dependency and Source Migration

- [ ] 1.1 Change the Go directive in `osb-cmdb-go/go.mod` to Go 1.25.0.
- [ ] 1.2 Replace the direct BrokerAPI dependency with `code.cloudfoundry.org/brokerapi/v13 v13.0.25`.
- [ ] 1.3 Replace all production-code imports of `github.com/pivotal-cf/brokerapi/v11` with the BrokerAPI v13 module path.
- [ ] 1.4 Replace all unit-test, integration-test, and mock imports with the BrokerAPI v13 module path.
- [ ] 1.5 Run `go mod tidy` with Go 1.25 and commit the resulting module graph and checksums.

## 2. Build Environment and Documentation

- [ ] 2.1 Update the Docker builder image to `golang:1.25-alpine`.
- [ ] 2.2 Update every GitHub Actions Go setup step to Go 1.25.
- [ ] 2.3 Update README prerequisites and migration documentation to reference Go 1.25 and `code.cloudfoundry.org/brokerapi/v13`.
- [ ] 2.4 Update any remaining repository references to the old BrokerAPI module path or Go 1.23 baseline.

## 3. Compatibility Verification

- [ ] 3.1 Run `gofmt` on changed Go files.
- [ ] 3.2 Run `go build ./...` using Go 1.25.
- [ ] 3.3 Run `go vet ./...` using Go 1.25.
- [ ] 3.4 Run `go test ./...` using Go 1.25; document sandbox-only loopback failures separately from code failures if they recur.
- [ ] 3.5 Run `golangci-lint run ./...`.
- [ ] 3.6 Build the Docker image and verify the Go 1.25 builder stage resolves dependencies successfully.
- [ ] 3.7 Run `openspec validate upgrade-go-brokerapi --strict`.
