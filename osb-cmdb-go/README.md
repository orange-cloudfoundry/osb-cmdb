# OSB-CMDB Go

A Go implementation of the OSB-CMDB (Open Service Broker - Cloud Meta-Data Broker) that proxies Open Service Broker API calls to Cloud Foundry.

## Overview

OSB-CMDB acts as a service broker that:
- Exposes services from a Cloud Foundry marketplace to other platforms (CF or Kubernetes)
- Tracks brokered service instances with metadata labels and annotations
- Supports all 10 OSB API operations including async provisioning and binding

## Features

- **Dynamic Catalog**: Fetches services from CF marketplace with periodic refresh
- **Metadata Tracking**: Propagates OSB context as CF labels/annotations
- **Async Support**: Full support for async provision, update, deprovision, bind, unbind
- **Multi-Platform**: Supports both CloudFoundry and Kubernetes OSB contexts
- **Configurable Filtering**: Exclude services or add name suffixes
- **Secure**: URL redaction in error messages, non-root container

## Quick Start

### Using Docker

```bash
docker run -d \
  -p 8080:8080 \
  -e BROKER_USERNAME=admin \
  -e BROKER_PASSWORD=secret \
  -e CF_API_HOST=https://api.cf.example.com \
  -e CF_USERNAME=cf-admin \
  -e CF_PASSWORD=cf-password \
  -e CF_DEFAULT_ORG=services-org \
  -e CF_DEFAULT_SPACE=backing-space \
  ghcr.io/orange-cloudfoundry/osb-cmdb-go:latest
```

### Using Helm

```bash
helm install osb-cmdb oci://ghcr.io/orange-cloudfoundry/charts/osb-cmdb-go \
  --set broker.password=secret \
  --set cf.apiHost=https://api.cf.example.com \
  --set cf.username=cf-admin \
  --set cf.password=cf-password \
  --set cf.defaultOrg=services-org \
  --set cf.defaultSpace=backing-space
```

## Configuration

All configuration is via environment variables:

### Broker Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `PORT` | HTTP server port | No | `8080` |
| `BROKER_USERNAME` | Basic auth username | Yes | - |
| `BROKER_PASSWORD` | Basic auth password | Yes | - |

### Cloud Foundry Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `CF_API_HOST` | CF API endpoint URL | Yes | - |
| `CF_USERNAME` | CF username | Yes | - |
| `CF_PASSWORD` | CF password | Yes | - |
| `CF_CLIENT_ID` | OAuth client ID (optional) | No | - |
| `CF_CLIENT_SECRET` | OAuth client secret (optional) | No | - |
| `CF_DEFAULT_ORG` | Default org for backing services | Yes | - |
| `CF_DEFAULT_SPACE` | Default space for backing services | Yes | - |
| `CF_SKIP_SSL_VALIDATION` | Skip TLS verification | No | `false` |

### Catalog Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `CATALOG_REFRESH_INTERVAL` | Catalog refresh interval | No | `5m` |
| `SERVICE_SUFFIX` | Suffix to add to service names | No | - |
| `EXCLUDED_SERVICES` | Comma-separated list of service patterns to exclude | No | - |

### Feature Flags

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `PROPAGATE_METADATA` | Propagate OSB context as CF metadata | No | `true` |
| `LOG_LEVEL` | Log level (debug, info, warn, error) | No | `info` |

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v2/catalog` | GET | Get service catalog |
| `/v2/service_instances/{id}` | PUT | Provision service instance |
| `/v2/service_instances/{id}` | PATCH | Update service instance |
| `/v2/service_instances/{id}` | DELETE | Deprovision service instance |
| `/v2/service_instances/{id}` | GET | Get service instance |
| `/v2/service_instances/{id}/last_operation` | GET | Poll last operation |
| `/v2/service_instances/{id}/service_bindings/{bid}` | PUT | Create binding |
| `/v2/service_instances/{id}/service_bindings/{bid}` | DELETE | Delete binding |
| `/v2/service_instances/{id}/service_bindings/{bid}` | GET | Get binding |
| `/v2/service_instances/{id}/service_bindings/{bid}/last_operation` | GET | Poll binding operation |
| `/health` | GET | Liveness probe |
| `/ready` | GET | Readiness probe |

## Development

### Prerequisites

- Go 1.25+
- Make

### Building

```bash
cd osb-cmdb-go
make build
```

### Testing

```bash
make test
```

### Linting

```bash
make lint
```

### Running Locally

```bash
export BROKER_USERNAME=admin
export BROKER_PASSWORD=secret
export CF_API_HOST=https://api.cf.example.com
export CF_USERNAME=admin
export CF_PASSWORD=password
export CF_DEFAULT_ORG=my-org
export CF_DEFAULT_SPACE=my-space

make run
```

## Architecture

```
osb-cmdb-go/
├── cmd/osb-cmdb/           # Main entry point
├── internal/
│   ├── broker/             # OSB broker implementation
│   ├── catalog/            # Dynamic catalog service
│   ├── cfclient/           # CF client wrapper
│   ├── config/             # Configuration
│   ├── context/            # OSB context parsing
│   ├── errors/             # Error handling
│   ├── metadata/           # Metadata formatting
│   └── middleware/         # HTTP middleware
├── test/
│   ├── mocks/              # Mock implementations
│   └── integration/        # Integration tests
└── deploy/
    └── helm/               # Helm chart
```

## License

Apache License 2.0
