### Implementation steps

* [x] spring-cloud-open-service-broker support for annotations in profiles documents: see https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/318
    * typed support for typed properties for annotations in https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/9e7fe1532346e00ee27b70162f028ca5c750408c/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L38 is worked around by used of untyped property setters

* [ ] [CF context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object) and [K8S context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#kubernetes-context-object) to propagated to my service broker in the [x-osb-cmdb](https://github.com/orange-cloudfoundry/osb-cmdb#osb-api-extension-x-osb-cmdb-used-with-backing-service-brokers) param
   * Was already propagated implicitly but serialized as java.lang.Map.toString() which make it hard for backing brokers to parse it with standard tooling.
   * Formatting as serialized json string
      * Enables robust parsing by backing brokers, with extra care of Json decoding
      * Consistent with existing x-osb-cmdb param for K8S profile (likely unused)

```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations": "{\"domain.com/org-key1\":\"org-value1\",\"domain.com/org-key2\":\"org-value2\"}",
      "brokered_service_context_space_annotations": "{\"domain.com/space-key1\":\"space-value1\",\"domain.com/space-key2\":\"space-value2\"}",
      "brokered_service_context_instance_annotations": "{\"domain.com/instance-key1\":\"instance-value1\",\"domain.com/instance-key2\":\"instance:-value2\"}"
    },
    "labels": {
      "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
      "brokered_service_context_organization_guid": "c2169b61-9360-4d67-968c-575f3a10edf5",
      "brokered_service_originating_identity_user_id": "0d02117b-aa21-43e2-b35e-8ad6f8223519",
      "brokered_service_context_space_guid": "1a603476-a3a1-4c32-8021-d2a7b9b7c6b4"
    }
  }
}
```

```json
{
  "annotations": {
    "brokered_service_originating_identity_extra": "{\"scopes.authorization.openshift.io\":[\"user:full\"]}",
    "brokered_service_originating_identity_username": "a-user-name",
    "brokered_service_originating_identity_groups": "[\"system:authenticated:oauth\",\"system:authenticated\"]",
    "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1"
  },
  "labels": {
    "brokered_service_instance_guid": "b6a7a748-6fa5-497c-b111-a3a727ec88db",
    "brokered_service_originating_identity_uid": "",
    "brokered_service_context_namespace": "cloudfoundry-service-instances"
  }
}
```




   * Formatting as json structure: 
      * easier and more consistent for backing brokers to parse and process (e.g. CSB) and perform input validation (e.g. COAB)
      * Conformant to JSON specifications which does not place restrictions on key names. See https://datatracker.ietf.org/doc/html/rfc7159#section-4 and https://stackoverflow.com/a/26592221/1484823 while this however present binding to most programming language types since . and / are likely unsupported characters in field names  
      * need to check that existing backing brokers won't break
      * coab maps this as yaml structure which is still possible to process within paas-templates using grep/sed (yq is missing from coa containers)
      

```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations": {
        "domain.com/org-key1": "org-value1",
        "domain.com/org-key2": "org-value2"
      },
      "brokered_service_context_space_annotations": {
        "domain.com/space-key1": "space-value1",
        "domain.com/space-key2": "space-value2"
      },
      "brokered_service_context_instance_annotations": {
        "domain.com/instance-key1": "instance-value1",
        "domain.com/instance-key2": "instance:-value2"
      }
    },
    "labels": {
      "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
      "brokered_service_context_organization_guid": "c2169b61-9360-4d67-968c-575f3a10edf5",
      "brokered_service_originating_identity_user_id": "0d02117b-aa21-43e2-b35e-8ad6f8223519",
      "brokered_service_context_space_guid": "1a603476-a3a1-4c32-8021-d2a7b9b7c6b4"
    }
  }
}
```

to make it consistent, the K8S profile would also be modified into

```json
{
  "annotations": {
    "brokered_service_originating_identity_extra": {
      "scopes.authorization.openshift.io": [
        "user:full"
      ]
    },
    "brokered_service_originating_identity_username": "a-user-name",
    "brokered_service_originating_identity_groups": [
      "system:authenticated:oauth",
      "system:authenticated"
    ],
    "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1"
  },
  "labels": {
    "brokered_service_instance_guid": "b6a7a748-6fa5-497c-b111-a3a727ec88db",
    "brokered_service_originating_identity_uid": "",
    "brokered_service_context_namespace": "cloudfoundry-service-instances"
  }
}
```

This creates a 

* [ ] [CF context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object) and [K8S context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#kubernetes-context-object) to be stored in the CMDB as brokered service instance annotations
