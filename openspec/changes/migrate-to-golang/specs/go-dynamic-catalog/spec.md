## Purpose

Dynamic catalog service that fetches service offerings from the Cloud Foundry marketplace and converts them to OSB catalog format, with periodic background refresh and configurable filtering.

## ADDED Requirements

### Requirement: Fetch catalog from CF marketplace
The catalog service SHALL fetch service offerings from the configured CF org/space and convert them to OSB ServiceDefinition format.

#### Scenario: Successful catalog fetch
- **WHEN** the catalog service refreshes
- **THEN** it lists all service offerings in the backing space and their associated plans

#### Scenario: Map CF service to OSB service
- **WHEN** a CF service offering is fetched
- **THEN** the catalog maps id, name, description, tags, bindable, plans, and metadata to OSB format

#### Scenario: Map CF plan to OSB plan
- **WHEN** a CF service plan is fetched
- **THEN** the catalog maps id, name, description, free, bindable, schemas, and maintenance_info to OSB format

### Requirement: Support service name suffix
The catalog service SHALL support adding a configurable suffix to service names.

#### Scenario: Suffix configured
- **WHEN** SERVICE_SUFFIX is set to "-cmdb"
- **THEN** all service names in the catalog have "-cmdb" appended

#### Scenario: No suffix configured
- **WHEN** SERVICE_SUFFIX is empty or not set
- **THEN** service names are returned unchanged

### Requirement: Support service exclusion
The catalog service SHALL support excluding services by name pattern.

#### Scenario: Service matches exclusion pattern
- **WHEN** EXCLUDED_SERVICES contains "test-*" and a service named "test-broker" exists
- **THEN** that service is not included in the catalog

#### Scenario: Service does not match exclusion
- **WHEN** a service name does not match any exclusion pattern
- **THEN** that service is included in the catalog

### Requirement: Periodic catalog refresh
The catalog service SHALL refresh the catalog periodically in the background.

#### Scenario: Refresh interval elapsed
- **WHEN** CATALOG_REFRESH_INTERVAL (default 5m) elapses
- **THEN** the catalog service fetches the latest services from CF

#### Scenario: Refresh failure
- **WHEN** a catalog refresh fails due to CF API error
- **THEN** the catalog service logs the error and retains the previous catalog

#### Scenario: Concurrent catalog access during refresh
- **WHEN** a client requests the catalog while a refresh is in progress
- **THEN** the client receives the current cached catalog without blocking

### Requirement: Thread-safe catalog access
The catalog service SHALL provide thread-safe access to the cached catalog.

#### Scenario: Multiple concurrent readers
- **WHEN** multiple goroutines read the catalog simultaneously
- **THEN** all readers receive consistent catalog data without race conditions
