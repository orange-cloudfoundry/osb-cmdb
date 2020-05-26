
Propagate metadata as custom params https://github.com/orange-cloudfoundry/cf-ops-automation-broker/issues/47#issuecomment-630726012
* [x] Add new flag to support opt-out
   * [x] Add associated unit test
* [x] Service instance params gets fill with a new custom param
* [ ] Acceptance test to assert param 
   * Using GSI endpoint 
      * Pb: `x-osb-cmdb` custom param would be exposed to app developers if returned in GSI endpoint
         * An an opt-in `hideMetadataCustomParamInGetServiceInstanceEndpoint` to return the custom param in GSI, and only activate this endpoint in the custom param AT
   * [ ] implement GSI
      * [x] implement GSI on interceptor
         * Pb: GSI call has no context unlike CSI to distinguish backing from brokered service instance using context.
         * Only accept call if previously provisionned in create call
      * [x] implement GSI on CmdbServiceInstance
         * [x] check javaclient support GSIP: org.cloudfoundry.client.v2.serviceinstances.ServiceInstances.getParameters()
         * Pb: SCOSB does not yet leverage 2.16 spaceId and serviceDefinitionId hints see https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/287 necessary to lookup in service instance by name in backend space
            * workaround: 
               * [x] Lookup backend service instance guid in metadata using brokered service instance guid
               * [x] Fetch backend service instance using V2 api to fetch dashboard
               * [x] Fetch backend service instance params using V2 api to fetch params
                  * [x] filter custom param when `hideMetadataCustomParamInGetServiceInstanceEndpoint` is set 
      * [x] add asserts on create: 
         * [x] configure catalog with instance retreivable
         * [x] call CF GSIP on backing si, and compare returned params to requested params, check no extra 
         * [x] assert invalid service guid is rejected
      * [ ] add asserts on update: copy plan update test into params update test
   * [x] Add new AT with `hideMetadataCustomParamInGetServiceInstanceEndpoint` opt-out and assert `x-osb-cmdb` custom param is returned
         


Polish before 1.0 release
* [x] Manage git repo history. 
   * 0.x are based on scab code base
   * `redesign-cmdb` branch rewritten spike history in a single squashed commit, and added incremental redesign and cleanup
   * Goals
      * make repo smaller
      * don't pollute git history that much with unrelated scab work
      * keep ability to compare SCAB AT/CT/gradle as they evolve/spin-off 
   * Alternatives:
      * **Squash `redesign-cmdb` branch into a single initial commit, update master to it, and leave the `redesign-cmdb` for further instropection/history as necessary**
      * Selectively squash `redesign-cmdb` branch to isolate scab work
   * [x] Squash `redesign-cmdb` branch into a single initial commit
      * Mention commit with history with URL to `redesign-cmdb` commit in github
   * [ ] Squash and edit/change the root commit
      

Clean up after 1.0 release
* [ ] update CI to differentiate SCAB contribs from Osb-cmdb contribs ?
* [ ] rename configuration to not pull anymore SCAB config prefix
   * Goal: look independent of scab for community contribs    
   * Rationales for delaying post 1.0
      * Impacts AT/CT/UT config. Would conflict SCAB exchanges there
   * [ ] update documentation
   * [ ] update CI
   * [ ] update paas-template manifest.yml file


* Improve packaging & release process
   * [ ] collect a publish code coverage report
      * Look at SCAB CI for leveraging `codeCoverageReport` task
   * [ ] Refine concourse pipeline to honor `skip-ci` git commit keyword   

* [ ] Harden acceptance tests
   * [ ] Fail fast on set up errors such as broker registration errors to avoid misleading error traces
   * [ ] refine cleans up of backing service using cf-java-client purge
      * Upon ERROR logs in recent logs, tardown assert fails, preventing clean up. This may be useful to inspect problems afterwards
   * [x] Disable wire trace logs are enabled in backend services for now   

* [ ] Refactor AT:
    * [ ] Fix broker clean up
       * [x] check cf-java-client support purge service offering (in cfclient) + purge service instance
       * [ ] modify call to use cfclient.deleteService(purge=true) for each service 
    * [ ] Rename Test class


* [ ] ~~optimize cf api calls:~~ delayed
         > START  Get Organization, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025530176414}
         > GET    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025675803922}
         > 200    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1 (20 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025695542107}
         > GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/private_domains?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025781465159}
         > GET    /v2/shared_domains?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025826601868}
         > 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/private_domains?page=1 (46 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025827872013}
         > GET    /v2/quota_definitions/66f4ff66-02e3-4541-a571-2b1c1a078715, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025840209217}
         > GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/space_quota_definitions?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025841744455}
         > GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025843418112}
         > 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/space_quota_definitions?page=1 (15 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025856717929}
         > 200    /v2/quota_definitions/66f4ff66-02e3-4541-a571-2b1c1a078715 (31 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025871754954}
         > 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?page=1 (39 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054025883241097}
         > 200    /v2/shared_domains?page=1 (204 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026030582880}
         > FINISH Get Organization (onComplete/517 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026047828351}
         > GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?q=name:app-service-create-instance-with-service-keys&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026069215465}
         > 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?q=name:app-service-create-instance-with-service-keys&page=1 (21 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026090640736}
         > START  Create Service Instance, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026095442524}
         > GET    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026125318859}
         > 200    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1 (19 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026145038591}
         > GET    /v2/spaces?q=name:app-service-create-instance-with-service-keys&q=organization_guid:14af188e-b07f-4041-9488-d97bacfcb49c&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026174237501}
         > 200    /v2/spaces?q=name:app-service-create-instance-with-service-keys&q=organization_guid:14af188e-b07f-4041-9488-d97bacfcb49c&page=1 (35 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026210076566}
         > GET    /v2/spaces/96e4bdb6-bd69-42ba-820d-aaa787edf274/services?q=label:app-service-create-instance-with-service-keys&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026360011530}
         > 200    /v2/spaces/96e4bdb6-bd69-42ba-820d-aaa787edf274/services?q=label:app-service-create-instance-with-service-keys&page=1 (34 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026393507811}
         > GET    /v2/service_plans?q=service_guid:adb1977c-732c-4a74-b6e5-ee5966010aed&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026438371818}
         > 200    /v2/service_plans?q=service_guid:adb1977c-732c-4a74-b6e5-ee5966010aed&page=1 (19 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026457758750}
         > POST   /v2/service_instances?accepts_incomplete=true, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026524599424}
         > PUT "/v2/service_instances/ba4556d6-298a-43ed-9df0-0a0b141ce265?accepts_incomplete=true", parameters={masked}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587054026670619848}
   * [ ] duplicate `START  Get Organization` on CSI
   * [x] Check whether current component tests already mock the chatty calls: they do
      * CreateInstanceWithSpacePerServiceInstanceTargetComponentTest
      * spring-cloud-app-broker-integration-tests/src/test/java/org/springframework/cloud/appbroker/integration/fixtures/CloudControllerStubFixture.java 
      
      >	public void stubCommonCloudControllerRequests() {
      >		stubGetPlatformInfo();
      >		stubFindTestOrg();
      >		stubFindTestSpace();
      >		stubFindDomains();
      >	}
   * [ ] chatty verbose calls to organization and space
      * getSpaceScopedOperations() and createSpace() are the faulty
         * try to get a Operations without looking up the space, if erroring, then try create the space ?
      * [ ] find a faster way to interate than AT which waits for a long push
         * a spring test which spring loads context and then invoke OsbCmdbServiceInstance protected methods
            * make AbstractOsbCmdbService a collaborator, and directly test it without interacting with OsbCmdbServiceInstance 
               * Reuse `DynamicServiceAutoConfigurationAcceptanceTest`
               * Not: won't work for testing cmdb use-cases such as CSI since prereq brokers will be missing. 


* [ ] Harden/uniformize handling of long service instance guid:
   ```  
   String backingServiceInstanceName =
   			ServiceInstanceNameHelper.truncateNameToCfMaxSize(request.getServiceInstanceId());
   ```
  * Remove name truncation, and just flow the upstream CF CC API error to the caller ?
  * Refactor to systematically/consistently truncate name everywhere 

* [ ] Set up unit test, mocking CF java client
   * [ ] Extract collaborator to deal with CloudFoundryOperations mock lifecycle (similar to CloudFoundryOperationsUtils)

* [ ] Refactor race condition support ?
   * [ ] extract concurrent exception handler in its collaborator object to unit test it


* [ ] reduce pipeline feedback time by tuning gradle fork policy
   * [ ] double avail procs had no visible changes: `maxParallelForks = (2* Runtime.runtime.availableProcessors()) - 1 ?: 1`
* [ ] automate triggering of smoke tests when concourse acceptance tests pass


* [ ] implement OSB GSI and GSB 
   * [ ] Refine race condition tests to handle conflicting params
      * [ ] Create
      * [ ] Update
      * [ ] Binding

* [ ] assert params are properly returned in AT
   * Using an Interceptor which stores received SIP/SBP and returns them into service binding   
   * Using GSIP and GSBP
      * [x] investigate whether CC API V2 returns existing params
         ```
           cf curl /v2/service_instances/0db0c6f3-92fb-47b2-8ab1-527c08c22c9c/parameters
           {
              "description": "This service does not support fetching service instance parameters.",
              "error_code": "CF-ServiceFetchInstanceParametersNotSupported",
              "code": 120004
           }
         ```
      * [ ] implement GSB
         * [ ] implement GSB on interceptor
         * [ ] implement GSB on CmdbServiceInstance
            * [x] check javaclient support GSBP: org.cloudfoundry.client.v2.servicebindings.ServiceBindingsV2.getParameters()
         * add asserts on bind: 
            * [ ] call CF GSBP on backing si, and compare returned params to requested params

    
* [ ] Refactor AT with multi broker support 
    * [ ] Wait for support in cf-java-client. https://github.com/cloudfoundry/cf-java-client/issues/1025
    * [ ] Fail fast on org.springframework.cloud.appbroker.acceptance.CloudFoundryAcceptanceTest.initializeBroker()
        > org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerNameTaken(270002): The service broker name is taken
        * [ ] Replace `blockingSubscribe(Mono<? super T> publisher)` with Mono.block() ?                                                                                                                
    * [ ] Modify AT to deploy two distinct brokers in the same CF app manifest, with distinct routes
    * [ ] Modify AT to still deploy single cf app, exposing two brokers
       * with distinct path prefix
       * with distinct route
       * backing broker 
          * is controlled with spring profile / env var injected
 



* [ ] Diagnose and fix suspected flaky test:  com.orange.oss.osbcmdb.CloudFoundryAppDeployerAutoConfigurationTest
```
java.lang.AssertionError: 
Expecting:
 <Unstarted application context org.springframework.boot.test.context.assertj.AssertableApplicationContext[startupFailure=org.springframework.beans.factory.BeanCreationException]>
to have a single bean of type:
 <com.orange.oss.osbcmdb.CloudFoundryTargetProperties>:
but context failed to start:
 org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'cloudFoundryClient': Invocation of init method failed; nested exception is reactor.core.Exceptions$ErrorCallbackNotImplemented: java.net.UnknownHostException: api.example.local
```


* [ ] **Set up component test, mocking CF API** to get faster feedback than AT
   * [ ] set up SCAB component test infra and cmdb test cases
      * [x] ~~Small step to get cmdb code status wih component tests: Add gradle dependency to cmdb~~
         * Fails with both => squash it and try direct copy in osb-cmdb codebase 
         ``` 
         	implementation project(":osb-cmdb")
         	testImplementation project(":osb-cmdb")
         ```
      * [ ] **Copy component tests in osb-cmdb module as a subpackage**
         * [x] Create new package `com.orange.oss.osbcmdb.com.orange.oss.osbcmdb.integration`
         * [x] Move cmdb classes from scab into it and make it compile. Pb
           > Class com.orange.oss.osbcmdb.com.orange.oss.osbcmdb.integration.cmdb.AsyncCreateInstanceFailureStillAssignsMetadataComponentTest 
           > uses package-private class org.springframework.cloud.appbroker.com.orange.oss.osbcmdb.integration.WiremockComponentTest
             * [x] transiently make the class public                                                                 
         * [x] Copy all `com.orange.oss.osbcmdb.integration-tests` module into `osb-cmdb` module
         * [x] Add external dependencies to cmdb package 
         * [ ] Handle wiremock state, in gradle config `// force a new fork for every test to eliminate issues with wiremock state`
           * [ ] fork every junit tests in ocb-cmdb: slow
           * [ ] identity/understand wiremock state pb 
           * [ ] revert and keep two distinct modules with distinct config
           * [ ] craft gradle syntax to only fork integration tests 
           
      * [ ] **Adapt `WiremockComponentTest` to load OsbCmdbApplication with some tunings**
            * [ ] Debug test failures resulting from SCAB rebase:
               * [x] CreateInstanceFailureWithOnlyABackingServiceAndMetadataTransformerComponentTest
                  * [ ] logback.yml is common to all test packages => spring security logs become polution
                     * [ ] transiently remove spring security verbose logs
                     * [ ] control logback level with profiles: https://www.baeldung.com/spring-boot-testing-log-level#1-profile-based-logging-settings
                  *  [ ] fixture fails to wiremock CC service instance stub to reach timeout and trigger failure

   * [ ] assert handling of forged last operation request in a component test

Q: how to simulate an async service error ?
* simplest: 1st response `last_operation` returns an error
* statefull responses: http://wiremock.org/docs/stateful-behaviour/ 

```
20-04-2020 15:22:41.391 [cloudfoundry-client-epoll-4] DEBUG cloudfoundry-client.request.request - GET    /v2/spaces/TEST-SPACE-GUID/service_instances?q=name:instance-id&page=1&return_user_provided_service_instances=true
20-04-2020 15:22:41.407 [cloudfoundry-client-epoll-4] DEBUG cloudfoundry-client.response.response - 200    /v2/spaces/TEST-SPACE-GUID/service_instances?q=name:instance-id&page=1&return_user_provided_service_instances=true (15 ms)
```

```
$ cf curl /v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c/service_instances
{
   "total_results": 1,
   "total_pages": 1,
   "prev_url": null,
   "next_url": null,
   "resources": [
      {
         "metadata": {
            "guid": "00cefea6-b1fb-4d2a-8185-61230e526bbb",
            "url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb",
            "created_at": "2020-04-20T13:38:58Z",
            "updated_at": "2020-04-20T13:38:58Z"
         },
         "entity": {
            "name": "gberche",
            "credentials": {},
            "service_plan_guid": "640e14b7-4677-4909-a97b-2b63f9276e1b",
            "space_guid": "ff120500-0550-4ce5-abb6-7ad52243c70c",
            "gateway_data": null,
            "dashboard_url": "https://p-mysql.redacted-domain.org/manage/instances/00cefea6-b1fb-4d2a-8185-61230e526bbb",
            "type": "managed_service_instance",
            "last_operation": {
               "type": "create",
               "state": "succeeded",
               "description": "",
               "updated_at": "2020-04-20T13:38:58Z",
               "created_at": "2020-04-20T13:38:58Z"
            },
            "tags": [],
            "maintenance_info": {},
            "service_guid": "f8fdfaa9-fdc1-4b68-8791-e55a1419a360",
            "space_url": "/v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c",
            "service_plan_url": "/v2/service_plans/640e14b7-4677-4909-a97b-2b63f9276e1b",
            "service_bindings_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/service_bindings",
            "service_keys_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/service_keys",
            "routes_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/routes",
            "service_url": "/v2/services/f8fdfaa9-fdc1-4b68-8791-e55a1419a360",
            "shared_from_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/shared_from",
            "shared_to_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/shared_to",
            "service_instance_parameters_url": "/v2/service_instances/00cefea6-b1fb-4d2a-8185-61230e526bbb/parameters"
         }
      }
   ]
}

```

vs ./osb-cmdb/src/test/resources/responses/cloudcontroller/list-space-service_instances.json

```
{
	"total_results": 1,
	"total_pages": 1,
	"prev_url": null,
	"next_url": null,
	"resources": [
		{
			"metadata": {
				"guid": "@guid",
				"url": "/v2/service_instances/@guid",
				"created_at": "2018-07-19T22:29:41Z",
				"updated_at": "2018-07-19T22:29:41Z"
			},
			"entity": {
				"name": "@name",
				"credentials": {},
				"space_guid": "@space-guid",
				"service_guid": "@service-guid",
				"service_plan_guid": "@plan-guid",
				"type": "managed_service_instance",
				"syslog_drain_url": "",
				"route_service_url": "",
				"space_url": "/v2/spaces/@space-guid",
				"service_bindings_url": "/v2/service_instances/@guid/service_bindings",
				"service_keys_url": "/v2/service_instances/@guid/service_keys",
				"service_url": "/v2/services/@service-guid",
				"service_plan_url": "/v2/service_plans/@plan-guid",
				"routes_url": "/v2/service_instances/@guid/routes"
			}
		}
	]
}

```

Q: how to simulate and assert proper async service error support ?
* simplest:  
   * [ ] responses to `last_operation` returns immediately an error
   * [ ] response to "spaces/id/serviceinstance" to return entity with last-operation-state=error
   * improve OsbCmdbServiceInstance to also look at returned ServiceInstance.status to fail synchronously when faster than configured timeout  ?
   * improve OsbCmdbServiceInstance to also look at returned ServiceInstance.status to fail synchronously when faster than configured timeout  ?
* statefull responses: http://wiremock.org/docs/stateful-behaviour/ 



                        
            * In hope that some existing tests osb-cmdb SCAB-based can work without much changes:
                ```
                ├── CreateBindingWithServiceKeyComponentTest.java: PASS
                ├── CreateInstanceFailureWithOnlyABackingServiceAndMetadataTransformerComponentTest.java: FAIL
                ├── CreateInstanceWithOnlyABackingServiceAndMetadataTransformerComponentTest.java
                ├── CreateInstanceWithParametersOnlyABackingServiceComponentTest.java
                ├── DeleteBindingWithServiceKeyComponentTest.java
                ├── DynamicServiceAutoConfigurationComponentTest.java: PASS
                ├── fixtures
                │   ├── AbstractServiceInstanceWorkflow.java
                │   ├── ExtendedCloudControllerStubFixture.java
                │   ├── ServiceKeyCreateServiceBindingWorkflow.java
                │   └── ServiceKeyDeleteServiceBindingWorkflow.java
                └── StaticOsbCatalogTest.java
                ```
         * [ ] **disable dynamic catalog with property**
         * [ ] **handle default osb-cmdb auth**
            * select a permissive spring security config to talk to osb api
               * using spring profile
            * modify org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture.serviceBrokerSpecification() to use basic auth
      * [ ] Make sure `CreateBindingWithServiceKeyComponentTest` pass within cmdb code base
      * [ ] Document component test (move 3 lines from `DONE-cmdb-native.md` in a README.md or package-info.java)
      
      
   * [ ] Initiate CmdbCreateServiceInstanceComponentTest from CreateInstanceWithSpacePerServiceInstanceTargetComponentTest
      * [ ] ~~configure scab-integration-tests to depend on osb-cmdb project~~
      * [ ] adapt WiremockComponentTest to use OsbCmdbApplication and inject osb-cmdb props: catalog off + admin user
      * [ ] **assert dashboard properly returned**
   * [ ] async backing service with timeout
   * [ ] async backing service without timeout
   * [ ] K8S dupls
   * [ ] Collect wire traces from CF API AT traces ?
   * [ ] check reasons for a distinct gradle module: can this be merged ?


