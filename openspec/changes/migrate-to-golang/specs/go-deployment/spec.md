## Purpose

Deployment artifacts for the Go OSB-CMDB broker including Docker image generation, OCI Helm chart, and GitHub Actions CI/CD pipeline.

## ADDED Requirements

### Requirement: Multi-stage Docker build
The Dockerfile SHALL use multi-stage builds to produce a minimal runtime image.

#### Scenario: Build stage
- **WHEN** the Docker image is built
- **THEN** the build stage uses golang:1.22-alpine and compiles with CGO_ENABLED=0

#### Scenario: Runtime stage
- **WHEN** the Docker image is built
- **THEN** the runtime stage uses distroless/static-debian12:nonroot for minimal attack surface

#### Scenario: Non-root execution
- **WHEN** the container runs
- **THEN** it executes as the nonroot user, not root

### Requirement: Version injection at build time
The Docker build SHALL inject the version into the binary.

#### Scenario: Version from git tag
- **WHEN** building with VERSION=v1.0.0
- **THEN** the binary reports version v1.0.0

### Requirement: Helm chart with basic resources
The Helm chart SHALL include Deployment, Service, ConfigMap, Secret, and ServiceAccount.

#### Scenario: Deploy with Helm
- **WHEN** helm install is run with required values
- **THEN** a Deployment, Service, ConfigMap, Secret, and ServiceAccount are created

#### Scenario: Configure CF connection via values
- **WHEN** cf.apiHost, cf.username, cf.password, cf.defaultOrg, cf.defaultSpace are set
- **THEN** the Deployment environment variables are populated accordingly

#### Scenario: Use existing secret
- **WHEN** existingSecret is set in values
- **THEN** the Deployment references that secret instead of the chart-created one

### Requirement: OCI Helm chart distribution
The Helm chart SHALL be packaged and pushed as an OCI artifact.

#### Scenario: Push chart to GHCR
- **WHEN** a release is triggered
- **THEN** the chart is pushed to oci://ghcr.io/orange-cloudfoundry/charts/osb-cmdb-go

### Requirement: Health and readiness probes
The Helm chart SHALL configure liveness and readiness probes.

#### Scenario: Liveness probe
- **WHEN** the container is running
- **THEN** Kubernetes probes GET /health on the http port

#### Scenario: Readiness probe
- **WHEN** the container is running
- **THEN** Kubernetes probes GET /ready on the http port

### Requirement: GitHub Actions CI workflow
The CI workflow SHALL build, test, and lint on PRs and pushes to main.

#### Scenario: PR validation
- **WHEN** a PR is opened targeting main with changes in osb-cmdb-go/
- **THEN** the workflow runs go build, go test, and golangci-lint

#### Scenario: Main branch push
- **WHEN** code is pushed to main with changes in osb-cmdb-go/
- **THEN** the workflow runs the same validation steps

### Requirement: GitHub Actions Release workflow
The Release workflow SHALL build and push Docker images and Helm charts.

#### Scenario: Tag push
- **WHEN** a tag like v1.0.0 is pushed
- **THEN** Docker image is pushed with tags :1.0.0, :1.0, and the Helm chart is pushed

#### Scenario: Main branch push
- **WHEN** code is pushed to main
- **THEN** Docker image is pushed with tag :main and :sha-<commit>

### Requirement: Container registry authentication
The Release workflow SHALL authenticate to GitHub Container Registry.

#### Scenario: GHCR login
- **WHEN** the release workflow runs
- **THEN** it logs in to ghcr.io using the GITHUB_TOKEN
