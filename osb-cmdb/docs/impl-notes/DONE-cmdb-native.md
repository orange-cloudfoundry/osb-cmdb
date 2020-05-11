* [x] Fix CI https://circleci.com/gh/orange-cloudfoundry/osb-cmdb-spike/282
   * [x] **Fix cmdb UT**
        >   ApplicationConfigurationIntegrationTest > paas_templates_overrides_default_cmdb_config_in_application_default_yml_Overrides_application_yml() FAILED
        >       java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:132
        >           Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException at ConstructorResolver.java:798
        >               Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException at DefaultListableBeanFactory.java:1700
        >   FAILED test: com.orange.oss.osbcmdb.serviceinstance.OsbCmdbServiceInstanceTest > createServiceInstanceWithTarget()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > unAuthenticatedActuactorHealth_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > unAuthenticatedSensitiveActuactorEndPoints_shouldFailWith401()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > authenticatedPostOsbRequest_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > basicAuthAuthenticatedAdmin_to_ActuactorInfo_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > basicAuthAuthenticatedOsbUser_to_ActuactorInfo_shouldSucceedWith401()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > basicAuthAuthenticatedOsbRequest_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > adminAuthenticatedSensitiveActuactorEndPoints_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > authenticatedOsbRequest_shouldSucceedWith200()
        >   FAILED test: com.orange.oss.osbcmdb.SecurityConfigTest > unauthenticatedOsbRequest_shouldFailWith401()
      * [x] modify circleci build to only run test in cmdb subproject

* [x] Reintroduce metadata support
   * Change prototype to not depend anymore on BackingService: just need a `Map<String, String> labels` and `Map<String, String> annotations`
      * create new metadata package
       * Introduce a new Metadata class
       * Replace BackingService with Metadata

 > BaseBackingServicesMetadataTransformationService
 > protected Mono<List<BackingService>> setMetadata(List<BackingService> backingServices,
 > 	ServiceBrokerRequest request, String serviceInstanceId,
 > 	Context context) {


* [x] Understand/refresh and document how scab integration tests work
   * `WiremockComponentTest` starts the spring boot app from integration tests and configures it to talk wiremock server launched in jvm.
      * In scab context, autoconfiguration classes are present in the classpath and thus automatically detected
      * Requires the wiremock resources to be present in "classpath:/responses/"
      * No auth is performed 
   * [x] Test `DynamicCatalogComponentTest`: just checks that static v2/catalog from application.yml is served to junit
      * [x] Fix changes to recorded mocks since rebase
      * [x] Rename and comment          
   * [x] Test `DynamicServiceAutoConfigurationComponentTest`
      * Pb: wiremock port conflicts `java.io.IOException: Failed to bind to /0.0.0.0:8080`
         * another scab rebase regression ? 
         * multiple wiremock instances started that conflict
         * compare with cmdb-master: WireMockServer fixture changed:
           > 	@PostConstruct
           >  	public void startWiremock() {
         * check circle ci history on rebase osb-cmdb master: `cmdb-master-rebased-from-scab`
      * [x] Fixed `ExtendedCloudControllerStubFixture` with now missing body id replacement


  * [x] increase cf-java client traces to wire debug in integration tests and in production
     * does not take effect. Why ?
        * [x] turn logback debug mode https://www.baeldung.com/logback#3-troubleshooting-configuration
```
16:04:37,484 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [com.orange.oss.osbcmdb.metadata] to DEBUG
16:04:37,484 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [cloudfoundry-client] to DEBUG
16:04:37,484 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [cloudfoundry-client.operations] to DEBUG
16:04:37,485 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [cloudfoundry-client.request] to DEBUG
16:04:37,486 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [cloudfoundry-client.response] to DEBUG
16:04:37,486 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [cloudfoundry-client.wire] to TRACE
```

Only getting request missing wire traces

```
20-04-2020 16:05:24.338 [cloudfoundry-client-epoll-4] DEBUG cloudfoundry-client.request.request - GET    /v2/spaces/TEST-SPACE-GUID/service_instances?q=name:instance-id&page=1&return_user_provided_service_instances=true
20-04-2020 16:05:24.377 [cloudfoundry-client-epoll-4] DEBUG cloudfoundry-client.response.response - 200    /v2/spaces/TEST-SPACE-GUID/service_instances?q=name:instance-id&page=1&return_user_provided_service_instances=true (37 ms)
```
        * [ ] typo in logback.xml ?
        * [x] overriden somewhere ?
           * production logback ?
```
16:10:35,726 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Found resource [logback.xml] at [file:/home/guillaume/code/osb-cmdb-spike/osb-cmdb/build/resources/test/logback.xml]
```
           * **WiremockComponentTest** properties !!
        * [ ] something interfering ?
           * [ ] missing http client lib in the classpath supporting wire traces ?
        * [ ] log output redirected ?
        * How to debug/fix ?
           * [x] step into with debugger: confirm the wrong level
              * [x] grep in all of the project the log category
           * [ ] Read cf-java-client-doc
           * [ ] Makesure wire traces work in acceptance tests
           
Pb: cf-java client logs gzip encoded content which can't be read
   * [ ] turn gzip off in cf-java-client
      * [x] ask for help
         * [x] stackoverflow does not seem active
         * [x] Prefer GH issue. https://github.com/cloudfoundry/cf-java-client/issues/1043
   * [x] turn gzip off in wiremock.
      * https://github.com/tomakehurst/wiremock/commit/3b46b0bcef963e675d7ea32a8bb968625c206486
   * [ ] log at the wiremock side instead
   

Pb: cf-java client org.cloudfoundry.operations.services.DefaultServices.createInstance(CreateServiceInstanceRequest) seems to ignore the last operation status, and thus backing service instances failure is silently ignored

* [x] Reproduce in an acceptance test
   * [x] Inject an interceptor impl which always fails
      * Pb: despites @ConditionalOnMissingBean acceptanceTestFailedAsyncBackingServiceInstanceInterceptor is still created
```
   2020-04-21T09:35:42.97+0200 [APP/PROC/WEB/0] OUT    OsbCmdbBrokerConfiguration#acceptanceTestBackingServiceInstanceInterceptor matched:
   2020-04-21T09:35:42.97+0200 [APP/PROC/WEB/0] OUT       - @ConditionalOnMissingBean (types: com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor; SearchStrategy: all) did not find any beans (OnBeanConditio


   2020-04-21T09:35:42.99+0200 [APP/PROC/WEB/0] OUT Parameter 3 of method osbCmdbServiceInstance in com.orange.oss.osbcmdb.OsbCmdbBrokerConfiguration required a single bean, but 2 were found:
   2020-04-21T09:35:42.99+0200 [APP/PROC/WEB/0] OUT     - acceptanceTestBackingServiceInstanceInterceptor: defined by method 'acceptanceTestBackingServiceInstanceInterceptor' in class path resource [com/orange/oss/osbcmdb/OsbCmdbBrokerConfiguration.class]
   2020-04-21T09:35:42.99+0200 [APP/PROC/WEB/0] OUT     - acceptanceTestFailedAsyncBackingServiceInstanceInterceptor: defined by method 'acceptanceTestFailedAsyncBackingServiceInstanceInterceptor' in class path resource [com/orange/oss/osbcmdb/OsbCmdbBrokerConfiguration.class]
   2020-04-21T09:35:42.99+0200 [APP/PROC/WEB/0] OUT Action:
   2020-04-21T09:35:42.99+0200 [APP/PROC/WEB/0] OUT Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier to identify the bean that should be consumed

```      
      * [x] Reproduce in a unit test to iterate faster than in AT
      * [ ] AT is running a stale version: missing a springboot jar task before !!
         * [ ] Find a way for acceptance tests to always run gradle bootjar before
          
   * [x] Create a new acceptance test class
      * Pb: cf java client getServiceInstance() is also mangling the lastOperation state, and only returns the last operation operation
      * Solution: use `status` field which complements `lastOperation` field.
         * [ ] ~~submit one issue to cf-java-client ?~~ Rather use accepteable workaround     
         * possible workarounds: 
            * use low-level CF-java-client v2/v3 instead of high level CfOperations
               * Pb: requires duplicating some private methods in cf-java-client to replicate same reactive code 
            * [x] use  service instance status  
* [x] Fix OsbCmdbService Instance: **check service instance status in last operation before returning async completion status**
   * [x] fix sync backing service failure handling in CSI
   * [x] fix async backing service failure handling
      * [x] Still need to fetch the proper backing service plan id. Passing the brokered service instance id obviously fails :-(
      * [x] Refine acceptance test to poll service instance status, and then check its status
   
 
* [x] reduce risk by getting feedback from smoke tests
   * [x] fix circle ci build preventing last commits from being included into the tarball
      * [x] exclude scab tests from integration tests
         * [x] add @Tag("scab") to scab test
         * [x] add -PexcludeTag to circle ci arg
         * [x] add -PexcludeTag to concourse  arg
   * [x] diagnose/fix missing matching backing service
      * [x] interceptor not excluded outside acceptance tests ?

* [x] fix sync backing service failure handling in USI
   * [x] Set up acceptance test
      * What kind of update to to ?
         * Update plan
         * Update params
         * **Update noop**: simplest for failure test
   * [x] Fix OsbServiceInstanceService


* [x] Fix concourse ci so that we get result of osb-cmdb unit tests. Currently in osb-cmdb we only get test matching profile acceptance tests (`-DincludeTags=AcceptanceTest`)
   * [x] Try removing `-DincludeTags=AcceptanceTest` and see if we still have conflicts among test cases and requested args, and need more gradle launches passes


* [ ] Refactor interceptor profile management to be safer to class renamings
   * [ ] ~~Configuration class uses class.getName()~~: annotation need a constant value, and can't make method calls
   ```
	public static String profileFor(Class aClass) {
		return ACCEPTANCE_TESTS_AND + aClass.getSimpleName();
	}
	
	@Bean
	@Profile(profileFor(ASyncFailedCreateBackingSpaceInstanceInterceptor.class))
   ```
   * [ ] Acceptance test uses class.getName()
      * [ ] Add a dependency to osb-cmdb main
      
      
* [x] study backward compat impact of backing service key name change. 
   * Likely break production code
   * Likely break out acceptance tests
   * [x] Space name is service definition name unlimited
   * [x] Backing service name is service definition truncated
   * [x] Service Key Name is straight brokered service instance id 
   * Fix it
      * [x] backport service instance name factory
* [ ] cleanup 
```
.name(ServiceInstanceNameHelper.truncateNameToCfMaxSize(brokeredServiceInstanceId)) 
```

* [x] fix async backing service failure handling in USI
   * [x] Set up acceptance test

* [x ] fix sync backing service failure handling in DSI
   * [x] Set up acceptance test: 
      * [x] backing service delete fails
      * [x] test cleans up brokered service using cf-java-client purge 
      * [x] test cleans up backing service using cf-java-client purge 
   * [x] Fix OsbServiceInstanceService


* [x] fix async backing service failure handling in DSI
   * [x] Set up acceptance test
      * [ ] Diagnose last test status: should we poll the backing service status ? Is this a race condition that can happen in other async tests and we need to restore/generalize polling inprogress status ? 

* [x] assert dashboard is properly returned
   * [x] base interceptor returns a dashboard url
   * [x] fix code to make test pass


* [x] assert that cmdb and backing service traces don't contain ERROR level traces such as the following
```
-04-23 07:38:42.279 ERROR 7 --- [nio-8080-exec-6] c.o.o.o.s.OsbCmdbServiceInstance         : Unexpected si state after delete delete full si is ServiceInstance{applications=[], id=c9c8595e-1f39-4406-810f-5a8f5edb6a56, name=d94467fd-d8ac-4b36-9863-10c85578c695, plan=standard, service=app-service-delete-instance-with-async-backing-failure, type=managed_service_instance, dashboardUrl=null, description=A service that deploys a backing app, documentationUrl=null, lastOperation=delete, message=, startedAt=2020-04-23T07:38:41Z, status=in progress, tags=[], updatedAt=2020-04-23T07:38:41Z}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587627522280180256}
23-04-2020 07:38:44.996 ?[35m[cloudfoundry-client-epoll-4]?[0;39m ?[39mDEBUG?[0;39m o.s.c.a.a.f.cf.CloudFoundryService.lambda$logRecentAppLogs$13 - LogMessage{applicationId=cb6e02a2-6533-4ad4-a165-874e858f798f, message=2020
```
   * [x] verify this works by injecting a fault
   * [x] remove the fault injection


* [x] handle isAsync accepted in create
   * [x] add polling if necessary: not necessary test class uses CF Operations which does the polling
      * [x] Pb: the test fails to find the backing service instance, whereas the brokered service instance has returned from async polling, and saw the backing service
         * the CFJC traces show that brokered service is indeed returning async provision ack
         * but client Test is making get requets on backing service 2s before 
            * brokered service returns last operation completion
      * hypothesis
         * Test createServiceInstance is not properly waiting for async status and returns early
            * Pb: a GSI confirms the stats "created" on brokered service
         * Race condition in CF which commits transactions after returning REST responses.
            * + some clock lag (2s) between Test client traces and broker traces
            * => confirm hypothesis with only CC timestamp in response headers
            * workaround: poll backing service instance for 20s before failing on absence of backing service
      * [x] **error logic in test: was looking at the wrong space: Parent class was not looking up the service name in overriden method, but directly in constant**  
   * [x] new interceptor
* [x] handle isAsync accepted in update
   * [x] new interceptor
   * [x] new test class
      * [x] Pb: the test fails to find the backing service instance, whereas the brokered service instance has returned from async polling, and saw the backing service
      

* [x] Refactor/clean AT
   * [x] remove duplication in constants, and only keep suffix as defined per test
      ```
     	private static final String SI_NAME = "si-delete-service-async-fail";
     	private static final String SUFFIX = "delete-instance-with-async-backing-failure";
     	private static final String BROKERED_SERVICE_NAME = "app-service-" + SUFFIX;
      ```
     * [x] encapsulate SI_NAME into brokeredServiceInstanceName() and use suffix as an impl
        * [x] refine clean up script for si leaks: 
           * backing services are purged with brokers,
           * brokered services are deleted without prefix filters
        * [x] add space clean up, which increases coverage

* [x] assert metadata is properly assigned in AT
   * [x] add asserts
   * [x] manual test

* [x] fix AT regression in commit 11385b3dac25e40c1e06477de51d5abfd2f56d9c
   * looks like setup isn't called anymore
      * [x] added missing Junit annotation
   * backing service/app name collision, implies weird race condition errors in CC
      * CF DB error/unstabilities
      
* [x] fix race condition tests
   * [x] configure small timeout wait to StalledCreate interceptor in the ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest (currently 5 mins by default in cf-java-client)     
      ```
      org.cloudfoundry.util.DelayTimeoutException
      	at org.cloudfoundry.util.DelayUtils.lambda$getDelay$8(DelayUtils.java:103) 
      ```
   * [x] don't fail on CJC timeout waiting for end of inprogress to StalledCreate interceptor
   * [x]Diagnose and fix 500 status error in sync create
      * [x] fix missing tearDown() method execution, preventing recentLogs from being dumped
      * Observed once
         ```
        No existing instance in the inventory, the exception is likely not related to concurrent or conflicting duplicate, rethrowing it 
         ```
      * Missing such trace in build 102.
         * recent log truncated ?
         * [x] check build clean up properly purges stalled service instances
      * [x] run test in debugger
         * [x] configure :bootJar gradle task before executing test
         * [x] update and execute `cleanUpAfterTestFailure.bash`
         * [x] manually run `cf logs  test-broker-app-concurrent-create-instance-with-service-keys | tee traces.txt &` to ease trace display
      * [x] fix invalid space id used to lookup instance
      * [x] fix invalid status 409 instead of 200: comparing brokered and backing service id, and service plan ids, instead of names 
         ```
        service definition mismatch with:f793e2cc-4fd9-4732-86a1-cdd4ae2aa8d6 
         ``` 
         * [x] check same error is not present in primary success branch: get_last_operation only checks backing service instance name equals brokered service instance id
            * currently does not enforce consistency in service definition and service plan.
               * could there be a forged injection there ? hard to imagine, and anyhow we don't use these unvalidated input data, so we're safe 
        
      * [x] Optimize concurrency error recovery calls: pass in CFOperations if available
        ```      
        c.o.o.o.s.OsbCmdbServiceInstance         : Inspecting exception caught org.springframework.cloud.servicebroker.exception.ServiceBrokerException: org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceInstanceNameTaken(60002): The service instance name is taken: 793fef2e-66ac-4315-89f9-915899f50f47 for possible concurrent dupl while handling request ServiceBrokerRequest{platformInstanceId='null', apiInfoLocation='null', originatingIdentity=null', requestIdentity=null}AsyncServiceBrokerRequest{asyncAccepted=false}AsyncParameterizedServiceInstanceRequest{parameters={}, context=null}CreateServiceInstanceRequest{serviceDefinitionId='7f8ae079-064f-4a65-9a1c-4aa05db46422', planId='c5c4170f-3449-4891-9d28-93f9979bcf25', organizationGuid='org-id', spaceGuid='space-id', serviceInstanceId='793fef2e-66ac-4315-89f9-915899f50f47'} , messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958668059287}
        cloudfoundry-client.operations           : START  Get Organization, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958668062224}
        cloudfoundry-client.request              : GET    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958695536076}
        cloudfoundry-client.response             : 200    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1 (32 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958727573470}
        cloudfoundry-client.request              : GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/private_domains?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958791966580}
        cloudfoundry-client.request              : GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958793299790}
        cloudfoundry-client.request              : GET    /v2/shared_domains?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958794157444}
        cloudfoundry-client.request              : GET    /v2/quota_definitions/66f4ff66-02e3-4541-a571-2b1c1a078715, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958795060875}
        cloudfoundry-client.request              : GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/space_quota_definitions?page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958796002405}
        cloudfoundry-client.response             : 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/private_domains?page=1 (35 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958827191455}
        cloudfoundry-client.response             : 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/space_quota_definitions?page=1 (55 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958850783197}
        cloudfoundry-client.response             : 200    /v2/quota_definitions/66f4ff66-02e3-4541-a571-2b1c1a078715 (57 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958853013743}
        cloudfoundry-client.response             : 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?page=1 (71 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958864581577}
        cloudfoundry-client.response             : 200    /v2/shared_domains?page=1 (96 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958891053003}
        cloudfoundry-client.operations           : FINISH Get Organization (onComplete/228 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958894653488}
        cloudfoundry-client.request              : GET    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?q=name:bsn-create-instance-with-service-keys&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958908748662}
        cloudfoundry-client.response             : 200    /v2/organizations/14af188e-b07f-4041-9488-d97bacfcb49c/spaces?q=name:bsn-create-instance-with-service-keys&page=1 (21 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958929925120}
        cloudfoundry-client.request              : GET    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958952589382}
        cloudfoundry-client.response             : 200    /v2/organizations?q=name:osb-cmdb-services-acceptance-tests&page=1 (24 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958977223882}
        cloudfoundry-client.request              : GET    /v2/spaces?q=name:bsn-create-instance-with-service-keys&q=organization_guid:14af188e-b07f-4041-9488-d97bacfcb49c&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766958996441573}
        cloudfoundry-client.response             : 200    /v2/spaces?q=name:bsn-create-instance-with-service-keys&q=organization_guid:14af188e-b07f-4041-9488-d97bacfcb49c&page=1 (21 ms), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766959017675688}
        cloudfoundry-client.request              : GET    /v2/spaces/0dbabfa1-3ed9-46c6-8fbb-e0c0c62605a7/services?q=label:bsn-create-instance-with-service-keys&page=1, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587766959037185146}


* [ ] Handle race conditions (including for K8S dups)      
   * [x] Implement race/conflict handling in create
      * [x] new method handleException() that is given any received exception + request
         * existingSi = getServiceInstance()
         * [ ] compare existingSi params
            * using GSIP CF API, prereq
               * [ ] GSI support in associated brokers
                  * [ ] COAB
                  * [ ] CF-mysql
         * [x] compare plans + service definition to request
         * throw appropriate ServiceBrokerException
            * 202 ACCEPTED: ServiceBrokerCreateOperationInProgressException
            * 409 CONFLICT: ServiceInstanceExistsException
         * return existingSi to trigger 201 accepted or 200
      * [ ] ~~alternative: systematically lookup for an existing service instance by name in target space~~
         * cons:
            * add extra load on normal CF client for seld K8S dupl client 
            * would still require to handle CF concurrent exceptions, like create space or create service instance 

   * [X] Test create https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#response-3
      * [x] New interceptor StalledAsyncCreate
      * [x] Refine CSI sync success test (i.e. existing instance)
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 200 Ok as backing service was completed
            * [x] 409 Conflict
               * [x] for different plans
               * [x] for different service definition id
               * [ ] for different params: on hold until GSIP
      * [x] New Create test that does 
         * [x] CSI  
         * [x] OSB provision dupl same SI
            * [x] Adapt an `OpenServiceBrokerApiFixture` in AT
               * [x] Inject CC API host/port/skip ssl in class
               * [x] use them in  serviceBrokerSpecification()
            * [x] check same dupl receives right status  
               * [x] 202 Accepted as backing service is still in progress
         * [x] OSB provision dupl different SI: check different dupl receives right status
            * [x] 409 Conflict
               * [x] for different plans
               * [x] for different params. Test implemented but ignored for now as not yet implemented (waiting for GSIP prioritized support)
               * [x] for different service definition id.
                  * We need a 2nd brokered service definition handled by osb-cmdb
                  * When passing the SCAB backing app service id
                     * interceptor ignores the request (as a side effect of missing OSB context our osb client fixture)
                     * a backing space and backing service instance is created and accepted => returns 202 instead of 409
                        * in other words, we don't detect recycling SI guid by osb clients. 
                           * CSI would allow multiple backing service for the same brokered SI guid
                           * DSI only deletes the backing service matching the specified service definition id in DSIReq  
                  * would be able to detect recycled SI guids by looking up an existing backing service matching brokered service instance guid in all spaces, using metadata query
                     * in the CSI before creating the instance
                        * without special care, this would override concurrent instance error handling:
                           * throws a new exception (similar to CF that could have thrown it)
                           * then error handling looks up a backing service instance to further qualify the conflict
                              * however it does not find a backing service instance in the conflicting space
                        * we therefore need to 
                           * [x] lookup the associated space 
                           * [x] compare its name to the expected service definition name
                           * [x] compare its parent org to the current org, to avoid incorrectly rejecting dupl ids among tenants
                           * [x] verify 409 asserted in the test
         * [x] Rework exceptions handling
            * [x] Use a specific exception class for exceptions that are thrown by our code and does not need further inspection: OsbCmdbInternalErrorException
            * [ ] Rename handleException() into better naming ?
                * inspectInspectionIntoOsbResponse() 
         * [ ] Diagnose and handle test failure
         ```
        Service broker parameters are invalid: missing operation field
        
        GET "/v2/service_instances/43bcb7d0-2515-4d24-9e5c-4ee4c928f7f4/last_operation?plan_id=3a56d4f6-8775-4b0d-86c0-c4ec74d770de&service_id=78f94c53-5516-4f98-ab3c-b29fef7de5a7", parameters={masked}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1588842614618603232}

        Accept: isServiceGuidPreviousProvisionnedByUs=false for serviceInstanceId=43bcb7d0-2515-4d24-9e5c-4ee4c928f7f4 and request=ServiceBrokerRequest{platformInstanceId='null', apiInfoLocation='api.redacted-domain.org/v2/info', originatingIdentity=null', requestIdentity=f3bb2924-8bf5-4468-ac91-124202c942b3}GetLastServiceOperationRequest{serviceInstanceId='43bcb7d0-2515-4d24-9e5c-4ee4c928f7f4', serviceDefinitionId='78f94c53-5516-4f98-ab3c-b29fef7de5a7', planId='3a56d4f6-8775-4b0d-86c0-c4ec74d770de', operation='null'}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1588842614663803509}
        
         ```
           * [x] Check OSBClientFixture not passing state: not the case       
           * [x] CC API receives empty last operation from concurrent call, and passes it around. Confirmed 
              * Pb: not using CC API for simulating concurrent calls in ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest, but OSB API fixture
              * Potentially with cleanup not being properly done and some instances remain.
                 * No explicit clean up between early AT test phase and full phase.         
              * [x] Fix it: pass state also in handleError()
                 * [x] check ServiceBrokerCreateOperationInProgressException(operation) is indeed for OSB operation. Reported https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/284
           * [x] Check missing last operation in some CC API facing CSI calls       
         * [ ] Diagnose and handle test failure
         ```
        org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerRequestRejected(10001): Service broker error: Service definition does not exist: id=78f94c53-5516-4f98-ab3c-b29fef7de5a7
        07-05-2020 09:10:21.894 ?[35m[cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.operations.lambda$log$2 - FINISH Create Service Instance (onError/845 ms)
        07-05-2020 09:10:21.907 ?[35m[Test worker]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.operations.lambda$log$1 - START  Get Service Instance 
        
        
         o.s.c.a.a.CloudFoundryAcceptanceTest.lambda$blockingSubscribe$16 - error subscribing to publisher
        org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerNameTaken(270002): The service broker name is taken
        	at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$0(ErrorPayloadMappers.java:47)
        	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
        Assembly trace from producer [reactor.core.publisher.MonoFlatMap] :
        	reactor.core.publisher.Mono.flatMap(Mono.java:2734)
         ```
           * missing clean up
              * [x] check missing clean up in test ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest 
              * [ ] check systematic clean up upon successfull test: 
              ```
             	private Mono<Void> cleanup(String orgId, String spaceId) {
             		return
             			cloudFoundryService.deleteServiceBroker(serviceBrokerName())
             			.then(cloudFoundryService.deleteApp(testBrokerAppName()))
             			.then(cloudFoundryService.removeAppBrokerClientFromOrgAndSpace(brokerClientId(), orgId, spaceId))
             			.onErrorResume(e -> Mono.empty());
             	}
              ```
                * [ ] Lookup all service plans associated to service brokers
                * [ ] Lookup all service instance associated to service brokers
                

* [x] Diagnose test failures: suspecting CF instability or concourse worker overload
   * [x] high concourse load some 15 mins ago (28 and 48 on 2 workers)
      * [x] retrigger test: successfull within 10 mins

``` 
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 30000ms in 'source(MonoCreate)' (and no fallback has been configured)
	at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.handleTimeout(FluxTimeout.java:289)
```

