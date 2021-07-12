### Product requirements

* P1: backward compatibility is preserved for existing Orange backing brokers
* P1 orange corporate metadatas (e.g. app code) get propagated to backing brokers with precedence for CF profile: org/space/instance
* P1: paas-templates coab model have default values including existing instances (i.e. without relying on coab-vars.yml to be updated for old instances)
* P2 orange corporate metadatas get indexed in osb-cmdb to be queryable
* P3 backing broker input validation remain simple for common/useful fields, including orange corporate metadata
* P3 x-osb-cmdb param syntax is consistent / makes sense to backing service brokers
* ~~P4 x-osb-cmdb param content have default values for all possible keys in cf and k8s profiles to avoid conditional field inclusion in brokers such as coab~~

### Implementation steps and design alternatives

* [x] spring-cloud-open-service-broker support for annotations in profiles documents: see https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/318
    * typed support for typed properties for annotations in https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/9e7fe1532346e00ee27b70162f028ca5c750408c/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L38 is worked around by used of untyped property setters

* [ ] [CF context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object) and [K8S context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#kubernetes-context-object) to propagated to my service broker in the [x-osb-cmdb](https://github.com/orange-cloudfoundry/osb-cmdb#osb-api-extension-x-osb-cmdb-used-with-backing-service-brokers) param

#### Current: Formatting annotations as java.util.Map.toString

   * Was already propagated implicitly but serialized as java.lang.Map.toString() which make it hard for backing brokers to parse it with standard tooling.

#### Option 1: Formatting as serialized json string

* +: Enables robust parsing by backing brokers, with extra care of Json decoding
* +: Consistent with existing x-osb-cmdb param for K8S profile (likely unused)
* +: Avoids extra work in osb-cmdb to have different logic between x-osb-cmdb param and master-depl-cf labels/annotations   
* -: requires Json deserialization support in backing service brokers to use the annotations: CSB, COAB, Logarythm
  * -: complex for coab to perform required user-facing input validation: requires coab to specifically parse keys depending on their name/json format and deserialize them 
* -: backing brokers still have to implement orange.com filtering + precedence impl + key name preprocessing
* -: orange annotations are not indexed in osb-cmdb to query them

```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations": "{\"domain.com/org-key1\":\"org-value1\",\"orange.com/overrideable-key\":\"org-value2\"}",
      "brokered_service_context_space_annotations": "{\"domain.com/space-key1\":\"space-value1\",\"orange.com/overrideable-key\":\"space-value2\"}",
      "brokered_service_context_instance_annotations": "{\"domain.com/instance-key1\":\"instance-value1\",\"orange.com/overrideable-key\":\"instance:-value2\"}"
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

For K8S

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


#### Option 1b: Formatting as serialized json string + with org/space/instance precedence and key name standardization:

* +: Enables robust parsing by backing brokers, with extra care of Json decoding
* +: Consistent with existing x-osb-cmdb param for K8S profile (likely unused)
* +: Avoids extra work in osb-cmdb to have different logic between x-osb-cmdb param and master-depl-cf labels/annotations   
* +: backing brokers can just use relevant keys without implementing precedence/normalization 
* +: orange annotations are indexed in osb-cmdb to query them

```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations": "{\"domain.com/org-key1\":\"org-value1\",\"orange.com/overrideable-key\":\"org-value2\"}",
      "brokered_service_context_space_annotations": "{\"domain.com/space-key1\":\"space-value1\",\"orange.com/overrideable-key\":\"space-value2\"}",
      "brokered_service_context_instance_annotations": "{\"domain.com/instance-key1\":\"instance-value1\",\"orange.com/overrideable-key\":\"instance:-value2\"}"
    },
    "labels": {
      "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
      "brokered_service_context_organization_guid": "c2169b61-9360-4d67-968c-575f3a10edf5",
      "brokered_service_originating_identity_user_id": "0d02117b-aa21-43e2-b35e-8ad6f8223519",
      "brokered_service_context_space_guid": "1a603476-a3a1-4c32-8021-d2a7b9b7c6b4",

      "brokered_service_context_orange_overrideable-key": "instance:-value2"
    }
  }
}
```

For K8S

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

Impl steps:
* [ ] Revert Json structure commit
* [ ] Refine test case to cover orange.com overriden keys use-case
* [ ] Implement overriden orange.com keys use-case
* [ ] Possibly add configuration flags to configure FQDN and white listed keys 
* [ ] Possibly perform input validation on annotations values to have friendly user-facing errors on invalid characters in annotations 


#### option 2: **Formatting as json structure**:
     
* +: easier and more consistent for backing brokers to parse and process (e.g. CSB) and perform input validation (e.g. COAB) without Json deserialization
* +: Conformant to JSON specifications which does not place restrictions on key names. See https://datatracker.ietf.org/doc/html/rfc7159#section-4 and https://stackoverflow.com/a/26592221/1484823 while this however present binding to most programming language types since . and / are likely unsupported characters in field names
* -: `annotations` structure lacks consistency: context is partly flattened, but context.annotations are not
* -: (hard to distinguish original OSB fields from Osb-cmdb fields)
* -: still duplicated work among backing brokers to select hierarchical annotations (org/space/instance)  
* -: orange annotations are not indexed in osb-cmdb to query them
* OK: need to check that existing backing brokers won't break. 2/2 confirmed. 
* OK: coab maps this as yaml structure which was prototyped within paas-templates using grep/sed (yq is missing from coa containers)
* -: harder impl for osb-cmdb: different logic for x-osb-cmdb param and master-depl/cf
   * annotations set by CF on service instances (in the osb-cmdb) only support key/cf won't support Json structure => need to preserve existing code for this
   * x-osb-cmdb-param can use slightly different codebase to format context into the variable. 
   * prototyped multiple approaches to get feedback from the code   
      * 1- modify com.orange.oss.osbcmdb.metadata.MetaData to include both structured objects (same as currently) and new json serialized strings in a new distinct member: challenging on Jackson serialization where two distinct fields need to serialize with same name, without conflicting, and optionally be empty. 
      * 2- introduce new com.orange.oss.osbcmdb.metadata.StructuredMetaData with structured objects : duplicates most of the current code
      * 3- relax strong binding from com.orange.oss.osbcmdb.metadata.MetaData.annotations and pass a boolean flag down the call chain to select between serialized string or structured json.
      * 4- distinct FormatterService instance for serialized string or structured json (i.e. storing decision as a field instead of parameter call): reaches the upper limit for fields in OsbCmdbServiceInstance
   * [x] Option 3) seems the best balance so far

* [ ] Refine acceptance tests ?
* [ ] Introduce `brokered_service_context_orange_overrideable-key` ?

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
        "orange.com/overrideable-key": "org-value2"
      },
      "brokered_service_context_space_annotations": {
        "domain.com/space-key1": "space-value1",
        "orange.com/overrideable-key": "space-value2"
      },
      "brokered_service_context_instance_annotations": {
        "domain.com/instance-key1": "instance-value1",
        "orange.com/overrideable-key": "instance:-value2"
      }
    },
    "labels": {
      "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
      "brokered_service_context_organization_guid": "c2169b61-9360-4d67-968c-575f3a10edf5",
      "brokered_service_originating_identity_user_id": "0d02117b-aa21-43e2-b35e-8ad6f8223519",
      "brokered_service_context_space_guid": "1a603476-a3a1-4c32-8021-d2a7b9b7c6b4",
      
      "brokered_service_context_orange_overrideable-key": "instance:-value2"
    }
  }
}
```

to make it consistent, the K8S profile would also be modified into returning the following content for `x-osb-cmdb` param

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

#### Option 3: Formatting param as flattened keys with raw mapping without key name standardization: 

* -: backing brokers still have to implement orange.com filtering + precedence impl + key name preprocessing
* -: orange annotations are not indexed in osb-cmdb to query them


```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations_domain.com/org-key1": "org-value1",
      "brokered_service_context_organization_annotations_orange.com/overrideable-key": "org-value2",
      "brokered_service_context_space_annotations_domain.com/space-key1": "space-value1",
      "brokered_service_context_space_annotations_orange.com/overrideable-key": "space-value2",
      "brokered_service_context_instance_annotations_domain.com/instance-key1": "instance-value1",
      "brokered_service_context_instance_annotations_orange.com/overrideable-key": "instance:-value2"
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

####  Option 3b: Formatting param as flattened keys with org/space/instance precedence and key name standardization:

* -: backing brokers still have to implement orange.com filtering + precedence impl + key name preprocessing
* -: orange annotations are not indexed in osb-cmdb to query them


```json
{
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_orange_annotations_org-key1": "org-value1",
      "brokered_service_context_orange_annotations_space-key1": "space-value1",
      "brokered_service_context_orange_annotations_instance-key1": "instance-value1",
      "brokered_service_context_orange_annotations_overrideable-key": "instance:-value2"
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

#### Option 4: Introducing v2 format in new `x_osb_cmdb_v2` key (while preserving existing `x-osb-cmdb` format)

* -: extra osb-cmdb work and effort w.r.t. option 1 without much benefit 
* +: backing brokers don't have to implement orange.com filtering + precedence impl + key name preprocessing
* +: orange annotations are not indexed in osb-cmdb to query them


```json
{
  "x_osb_cmdb_v2": {
    "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
    "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
    "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
    "brokered_service_originating_identity": {
      "platform": "cloudfoundry",
      "user_id": "0d02117b-aa21-43e2-b35e-8ad6f8223519"
    },
    "brokered_osb_context": {
      "platform": "cloudfoundry",
      "organization_name": "osb-cmdb-brokered-services-org-client-0",
      "space_name": "smoke-tests",
      "instance_name": "osb-cmdb-broker-0-smoketest-1600699922",
      "organization_guid": "c2169b61-9360-4d67-968c-575f3a10edf5",
      "space_guid": "1a603476-a3a1-4c32-8021-d2a7b9b7c6b4",
      "organization_annotations": {
        "domain.com/org-key1": "org-value1",
        "orange.com/overrideable-key": "org-value2"
      },
      "space_annotations": {
        "domain.com/space-key1": "space-value1",
        "orange.com/overrideable-key": "space-value2"
      },
      "instance_annotations": {
        "domain.com/instance-key1": "instance-value1",
        "orange.com/overrideable-key": "instance:-value2"
      }
    },
    "orange_context_annotations": {
      "overrideable-key": "instance:-value2"
    }
  },

  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_context_spaceName": "smoke-tests",
      "brokered_service_context_organizationName": "osb-cmdb-brokered-services-org-client-0",
      "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
      "brokered_service_context_instanceName": "osb-cmdb-broker-0-smoketest-1600699922",
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
      "brokered_service_context_organization_annotations": "{\"domain.com/org-key1\":\"org-value1\",\"orange.com/overrideable-key\":\"org-value2\"}",
      "brokered_service_context_space_annotations": "{\"domain.com/space-key1\":\"space-value1\",\"orange.com/overrideable-key\":\"space-value2\"}",
      "brokered_service_context_instance_annotations": "{\"domain.com/instance-key1\":\"instance-value1\",\"orange.com/overrideable-key\":\"instance:-value2\"}"
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

for k8s

```json
{
  "x_osb_cmdb_v2": {
    "brokered_service_api_info_location": "api.redacted-domain.com/v2/info",
    "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1",
    "brokered_service_instance_guid": "7d9235c5-242d-4b17-ac82-935f121ffd7f",
    "brokered_service_originating_identity": {
      "platform": "kubernetes",
      "value": {
        "username": "duke",
        "uid": "c2dde242-5ce4-11e7-988c-000c2946f14f",
        "groups": [
          "system:authenticated:oauth",
          "system:authenticated"
        ],
        "extra": {
          "scopes.authorization.openshift.io": [
            "user:full"
          ]
        }
      }
    },
    "brokered_osb_context": {
      "platform": "kubernetes",
      "namespace": "development",
      "clusterid": "8263feba-9b8a-23ae-99ed-abcd1234feda"
    },
    "orange_context_annotations": {
      "orange.com/overrideable-key": "instance:-value2"
    }
  },
  "x-osb-cmdb": {
    "annotations": {
      "brokered_service_client_name": "osb-cmdb-backend-services-org-client-1"
      "brokered_service_originating_identity_extra": "{\"scopes.authorization.openshift.io\":[\"user:full\"]}",
      "brokered_service_originating_identity_username": "duke",
      "brokered_service_originating_identity_groups": "[\"system:authenticated:oauth\",\"system:authenticated\"]",
    },
    "labels": {
      "brokered_service_instance_guid": "b6a7a748-6fa5-497c-b111-a3a727ec88db",
      "brokered_service_originating_identity_uid": "c2dde242-5ce4-11e7-988c-000c2946f14f",
      "brokered_service_context_namespace": "cloudfoundry-service-instances"
    }
  }
}
```

This creates a 

* [ ] [CF context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object) and [K8S context annotations](https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#kubernetes-context-object) to be stored in the CMDB as brokered service instance annotations
