* [ ] assert that cmdb and backing service traces don't contain ERROR level traces such as the following
```
-04-23 07:38:42.279 ERROR 7 --- [nio-8080-exec-6] c.o.o.o.s.OsbCmdbServiceInstance         : Unexpected si state after delete delete full si is ServiceInstance{applications=[], id=c9c8595e-1f39-4406-810f-5a8f5edb6a56, name=d94467fd-d8ac-4b36-9863-10c85578c695, plan=standard, service=app-service-delete-instance-with-async-backing-failure, type=managed_service_instance, dashboardUrl=null, description=A service that deploys a backing app, documentationUrl=null, lastOperation=delete, message=, startedAt=2020-04-23T07:38:41Z, status=in progress, tags=[], updatedAt=2020-04-23T07:38:41Z}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587627522280180256}
23-04-2020 07:38:44.996 ?[35m[cloudfoundry-client-epoll-4]?[0;39m ?[39mDEBUG?[0;39m o.s.c.a.a.f.cf.CloudFoundryService.lambda$logRecentAppLogs$13 - LogMessage{applicationId=cb6e02a2-6533-4ad4-a165-874e858f798f, message=2020
```
   * [ ] verify this works by injecting a fault
   * [ ] remove the fault injection

* [ ] handle isAsync accepted in create
   * [x] add polling if necessary: not necessary test class uses CF Operations which does the polling
      * [ ] Pb: the test fails to find the backing service instance, whereas the brokered service instance has returned from async polling, and saw the backing service
   * [x] new interceptor
* [ ] handle isAsync accepted in update
   * [x] new interceptor
   * [x] new test class
* [ ] add timeout to reactor blocking calls ? 
   * Are cf-java-client timeouts sufficient ?
   * Check default values
   * [ ]  

* [x] assert dashboard is properly returned
   * [x] base interceptor returns a dashboard url
   * [ ] fix code to make test pass
* [ ] assert params are properly returned in AT
   * [ ] implement GSI
   * add asserts on create
   * add asserts on update: copy plan update test into params update test
* [ ] assert metadata is properly assigned in AT ?
* [ ] Handle race conditions (including for K8S dups)      



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


* [ ] reduce pipeline feedback time by tuning gradle fork policy
* [ ] automate triggering of smoke tests when concourse acceptance tests pass

* [ ] assert handling of forged last operation request in a component test

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

Q: how is cf-javaclient accepting partially incomplete response ?
  * wrong response returned ?
  * **response without last operation** 

Somehow, this gets ignored and no retry is attempted despite org.cloudfoundry.util.LastOperationUtils.waitForCompletion():

```
    public static Mono<Void> waitForCompletion(Duration completionTimeout, Supplier<Mono<LastOperation>> lastOperationSupplier) {
        return lastOperationSupplier.get()
            .map(LastOperation::getState)
            .filter(state -> !IN_PROGRESS.equals(state))
            .repeatWhenEmpty(DelayUtils.exponentialBackOff(Duration.ofSeconds(1), Duration.ofSeconds(15), completionTimeout))
            .onErrorResume(t -> t instanceof ClientV2Exception && ((ClientV2Exception) t).getStatusCode() == 404, t -> Mono.empty())
            .then();
    } 
```
=> modify usage of response to use `status` field.
=> rather modify to use client low level API and avoid unnecessary polling of last operation in create.

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

* [ ] Collect Cf java client exception
   * [ ] Submit cf-java client issue to have exceptions be documented and tested
   * [ ] Submit cf-java client issue to have purge option to OperationsService (in addition to cfclient low level) with async service deletion polling
* [ ] Set up unit test, mocking CF java client
   * [ ] Extract collaborator to deal with CloudFoundryOperations mock lifecycle (similar to CloudFoundryOperationsUtils)

* [ ] Refine AT in CI
   * [ ] Adapt clean up script to also clean up backing (spacePerServiceDefinition) spaces ?
   
* [ ] Refine AT coverage
    * [ ] **dashboard url**
    * [ ] update service instance plan
    * [ ] dynamic catalog
    * [ ] service instance params ?
    * [ ] service binding params ?
    * [ ] async backing service
    * [ ] metadata
* [ ] Refactor AT:
    * [ ] Delete unneeded scab test, once we get sufficient inspiration    
    * [ ] Fix broker clean up
       * [x] check cf-java-client support purge service offering (in cfclient) + purge service instance
       * [ ] modify call to use cfclient.deleteService(purge=true) for each service 
    * [ ] Rename Test class

     
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


* [x] refine pipeline notifications to include the commit message
   * [x] fix formatting
   
