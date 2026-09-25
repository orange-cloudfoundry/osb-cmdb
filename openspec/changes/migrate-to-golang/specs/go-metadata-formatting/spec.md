## Purpose

Metadata formatting service that extracts context from OSB requests and formats it as Cloud Foundry labels and annotations for tracking brokered service instances.

## ADDED Requirements

### Requirement: Parse OSB originating identity header
The metadata formatter SHALL parse the X-Broker-API-Originating-Identity header to extract platform context.

#### Scenario: CloudFoundry identity
- **WHEN** the header is "cloudfoundry base64({...})"
- **THEN** the formatter extracts CF context including user_id, organization_guid, space_guid

#### Scenario: Kubernetes identity
- **WHEN** the header is "kubernetes base64({...})"
- **THEN** the formatter extracts K8S context including uid, username, groups, namespace

#### Scenario: Missing identity header
- **WHEN** the X-Broker-API-Originating-Identity header is absent
- **THEN** the formatter returns empty metadata without error

### Requirement: Format CloudFoundry context as labels
The metadata formatter SHALL format CF context properties as CF V3 labels and annotations.

#### Scenario: GUID properties become labels
- **WHEN** a context property contains "_guid" or "_id" in its name
- **THEN** the property is formatted as a label with prefix "brokered_service_context_"

#### Scenario: Mutable properties become annotations
- **WHEN** a context property does not contain "_guid" or "_id"
- **THEN** the property is formatted as an annotation with prefix "brokered_service_context_"

#### Scenario: Brokered instance GUID label
- **WHEN** formatting metadata for a service instance
- **THEN** a label "brokered_service_instance_guid" is set to the OSB instance_id

### Requirement: Format Kubernetes context as labels
The metadata formatter SHALL format K8S context properties as CF V3 labels and annotations.

#### Scenario: K8S UID becomes label
- **WHEN** formatting K8S context with uid
- **THEN** the uid is stored as a label

#### Scenario: K8S namespace becomes annotation
- **WHEN** formatting K8S context with namespace
- **THEN** the namespace is stored as an annotation

### Requirement: Extract whitelisted Orange annotations
The metadata formatter SHALL extract orange.com/ prefixed annotations and promote them to labels.

#### Scenario: Orange annotation present
- **WHEN** instance_annotations contains "orange.com/team": "platform"
- **THEN** a label "brokered_service_context_orange_team" is set to "platform"

#### Scenario: Non-Orange annotation ignored
- **WHEN** instance_annotations contains annotations without orange.com/ prefix
- **THEN** those annotations are not promoted to labels

### Requirement: Select formatter based on platform
The metadata service SHALL select the appropriate formatter based on the originating identity platform.

#### Scenario: CloudFoundry platform
- **WHEN** the originating identity platform is "cloudfoundry"
- **THEN** the CloudFoundry formatter is used

#### Scenario: Kubernetes platform
- **WHEN** the originating identity platform is "kubernetes"
- **THEN** the Kubernetes formatter is used

#### Scenario: Unknown platform
- **WHEN** the originating identity platform is unrecognized
- **THEN** a default formatter with minimal metadata is used
