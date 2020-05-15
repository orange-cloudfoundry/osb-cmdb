# Implementation steps

* [~] Set up acceptance test
   * [x] Make POC compile
      * [x] Make OsbCmdbServiceInstance compile
      * [x] Make OsbCmdbServiceBinding compile
         * [x] Extract superclass with default space behavior
   * [x] Wire autoconfiguration in OsbCmdbApplication. For now reuse scab autoconfiguration and just override ServiceInstanceService
   * [x] Cherry pick service key acceptance test from service-key-support branch & associated files
      * alternatives
      * [ ] ~~create a `service-key-support-flattened-Aprl-5`~~
      * ~~using git reset~~
         * [ ] git reset --keep `service-key-support`: brings in all files from service-key support in working dir
            * from d4da3f73
         * [ ] git reset --soft : reset index
      * [x] using git checkout https://jasonrudolph.com/blog/2009/02/25/git-tip-how-to-merge-specific-files-from-another-branch/
         * acceptance-tests
         * autoconfiguration
            * [ ] move dynamoc catalog in osb-cmdb package ?
   * [ ] ~~Adapt AT infrastructure to deploy osb-cmdb jar broker + sample NoOp broker~~ Too costly for now
      * osb-cmdb jar broker: using -Dtests.broker-app-path=osb-cmdb/build/libs/osb-cmdb-0.10.0-SNAPSHOT.jar
         * [ ] set up properties to turn dynamic catalog off
         * step 1: OsbCmdbServiceInstance + OsbCmdbServiceBinding + CFClient beans. Workflows and AppDeployer beans are instanciated but ignored.
         * step 2: Only OsbCmdbServiceInstance + OsbCmdbServiceBinding + CFClient beans
      * sample NoOp broker: new NoOpServiceInstance + existing NoOpServiceBinding
         * set up a new module ?
            * configure it to produce spring boot jar
         * use existing acceptance-test/main
      * [ ] modify org.springframework.cloud.appbroker.acceptance.CloudFoundryAcceptanceTest.initializeBroker() to
         * have distinct catalog configurations between the two brokers
         * deploy twice
      * eventually: duplicate AT to not depend on SCAB anymore ?
   * [~] Adapt OsbCmdb to support some OSB requests returning Noop
      * [ ] Conditioned by a spring profile acceptance test
      * [ ] Only triggering for some service definitions `app-service-*`
         * Q: does SC-OSB supports multiple ServiceInstance beans ? How is its autoconfig looking ?
            * Single mandatory ServiceInstance is required. See spring-cloud-open-service-broker/spring-cloud-open-service-broker-autoconfigure/src/main/java/org/springframework/cloud/servicebroker/autoconfigure/web/reactive/ServiceBrokerWebFluxAutoConfiguration.java
            * Optional ServiceBinding, see spring-cloud-open-service-broker/spring-cloud-open-service-broker-autoconfigure/src/main/java/org/springframework/cloud/servicebroker/autoconfigure/web/ServiceBrokerAutoConfiguration.java
         * A decorator pattern with a new interface duplicating ServiceInstanceService and ServiceBindingService
            * A ServiceInstanceRouterImpl which dispatch between
         * A branch in production code
            * private ServiceInstanceInterceptor osbInterceptor
* [ ] Run AT
   * [x] adapt env vars to cmdb expectation
   * [x] regenerate cmdb bootjar using gradle
   * [x] turn on cf-java-client traces
      logging.level.cloudfoundry-client.operations: debug
      logging.level.cloudfoundry-client.request: debug
      logging.level.cloudfoundry-client.response: debug
      logging.level.cloudfoundry-client.wire: trace
    * [ ] fix bugs
       * [x] too heavy CF API calls
       * [x] logic errors in ServiceInstance
       * [x] logic errors in ServiceInstanceInterceptor
       * [x] Infinite loop: brokered service instance creation ask for brokered service creation until exhausting CC API threads.
          * In the common osb-cmdb case, brokered service definition is same as backend service definition name: no suffix is added
             * however, differences are that
                * two distinct brokers are actually deployed, enabling selective publication of service plans
                * brokered service where not published in backend orgs, avoiding confusion with multiple service brokers
                * AT where using CF CLI which specify service broker to instanciate
          * Alternative fixes
             * [ ] Test different set up than prod: brokered service definition name being different than backing service definition name
                   * [ ] Restore BackingService spec code and use it in OsbCmdbServiceInstance: corresponds to different case than osb-cmdb prod
                      * [ ] Modify CreateInstanceWithBackingServiceKeysAcceptanceTest catalog + requested service definitions
                   * [ ] Configure an artificial suffix in OsbCmdbServiceInstance to convert BrokeredService name into BackendService name
             * [~] Test same set up than prod: brokered service name == backend service name
                * [x] **Modify ServiceInstanceInterceptor to only accept OSB calls in backing spaces**,
                   * [x] CSI and USI using using CF context
                   * DSI and GLO don't have backing space as context
                      * [x] Maintain state and correlate provisionned service instance id(s)
                   * [x] replace interface defaults
                   * [x] Inject the default space name
                   * [x] Modify AT to lookup backing service with same name as brokered service, but in backing space
                * [ ] Fix AT infrastructure to reduce visibility when publishing brokered and backing service plans to the appropriate orgs
                   * [ ]
                * [ ] ~~Specify broker in CSI calls~~
                   * [ ] Modify AT to deploy two distinct brokers
                   * [ ] Check recent support in cf-java-client. https://github.com/cloudfoundry/cf-java-client/issues/1025
                   * [ ] Contribute support in cf-java-client
       * [x] Json serialization issue
          > java.lang.RuntimeException: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
          > No serializer found for class com.orange.oss.osbcmdb.serviceinstance.OsbCmdbServiceInstance$CmdbOperationState and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
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

       

* [~] Run AT in CI
   * [x] Adapt JVM arg -Dtests.broker-app-path=/home/guillaume/code/osb-cmdb-spike/osb-cmdb/build/libs/osb-cmdb-0.10.0-SNAPSHOT.jar
      * [x] fly hijack to know if current dir absolute path is constant
      * [x] modify task to replace $PWD with current var
   * [ ] Adapt clean up script to also clean up backing (spacePerServiceDefinition) spaces ?
   * [x] Modify pipeline to run on `redesign-cmdb` branch
* [ ] Fix CI https://circleci.com/gh/orange-cloudfoundry/osb-cmdb-spike/282
   * [x] Add tag to select which test to run
   * [ ] **Fix cmdb UT**
   
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
   
   * [ ] Delete unneeded scab test, once we get sufficient inspiration    
* [ ] Refine AT coverage
    * [ ] **dashboard url**
    * [ ] update service instance plan
    * [ ] dynamic catalog
    * [ ] service instance params ?
    * [ ] service binding params ?
    * [ ] async backing service
    * [ ] metadata
* [ ] Refactor AT:
    * [x] Extract variables to make explicit backend service name
    * [ ] Fix broker clean up
       * [x] DSI bug: missing subscribe to mono through block()
       * [x] check cf-java-client support purge service offering (in cfclient) + purge service instance
       * [ ] modify call to use cfclient.deleteService(purge=true) for each service 
    * [x] Remove app-broker unnecessary properties passed as env vars
    * [ ] Rename Test class
* [ ] Refactor AT with multi broker support 
    * [ ] Fail fast on org.springframework.cloud.appbroker.acceptance.CloudFoundryAcceptanceTest.initializeBroker()
        > org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerNameTaken(270002): The service broker name is taken
        * [ ] Replace `blockingSubscribe(Mono<? super T> publisher)` with Mono.block() ?                                                                                                                
    * [ ] Modify AT to deploy two distinct brokers
    * [ ] Check recent support in cf-java-client. https://github.com/cloudfoundry/cf-java-client/issues/1025
    
* [ ] Collect wire traces from CF API
* [ ] **Set up component test, mocking CF API** to get faster feedback than AT
   * [ ] Initiate CmdbCreateServiceInstanceComponentTest from CreateInstanceWithSpacePerServiceInstanceTargetComponentTest
      * [ ] configure scab-integration-tests to depend on osb-cmdb project
      * [ ] adapt WiremockComponentTest to use OsbCmdbApplication and inject osb-cmdb props: catalog off + admin user
      * [ ] **assert dashboard properly returned**
   * [ ] async backing service with timeout
   * [ ] async backing service without timeout
   * [ ] K8S dupls
* [ ] Collect Cf java client exception
   * [ ] Submit cf-java client issue to have exceptions be documented and tested
* [ ] Set up unit test, mocking CF java client
   * [ ] Extract collaborator to deal with CloudFoundryOperations mock lifecycle (similar to CloudFoundryOperationsUtils)

* [ ] Refactor

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

        
* [x] set up shorter feedback loop than current full CI (15 min per commit)
   * [x] add new tag (eg "k8s") on tests being worked on
   * [ ] filter on `cmdb & k8s`: stashed 
      * [ ] prototype filtering on AND or multiple tags
         * [ ] junit supports & syntax https://junit.org/junit5/docs/current/user-guide/#running-tests `smoke & feature-a`
         * [ ] modify SCAB build to accept multiple tags
            * [ ] adapt pipeline to renamed property
            * [ ] support quotes and & in pipeline arg ?
   * [x] filter only on `k8s`
      * [x] run single test locally
   * [x] transiently only run a subset of tests being worked on
   * [ ] increase build concurrency: apparently 3 in parallel now
      * [ ] attempted hardcoded 6
      
   
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
   * [x] Impl delete error handling (in particular concurrent deletes)
      * Error use-cases
         * concurrent service key deletion (sync)
         * concurrent service provisionning: a delete is in progress
      * Recovery: lookup service instance and return status depending on its state 
   * [x] Test delete https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#response-9
      * [x] Refine DSI sync success test  
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 410 GONE
      * [x] New interceptor StalledAsyncDelete
      * [x] Successful Async Delete test: covered in CreateDeleteAsyncInstanceWithBackingServiceKeysAcceptanceTest 
         * [x] async DSI which gets accepted: check same dupl receives right status  
            * [x] 202 Accepted as backing service deprovision is still in progress
      * [x] New Concurrent Async Delete test that does 
         * [x] async DSI which gets stalled  
         * [x] OSB unprovision dupl same SI: check same dupl receives right status  
            * [x] 202 Accepted as backing service deprovision is still in progress
   * [x] Test update https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#response-5
      * [x] Implement update error recovery
         * Check si, if an update operation is in progress, then return 202 accepted, 
         * otherwise 500 bad request if update request was previously accepted
         * otherwise 400 bad request otherwise
         * OSB api v2.16 will be supporting error details such as "usable", see https://github.com/openservicebrokerapi/servicebroker/pull/661
            * But not yet implemented in CF API v3 http://v3-apidocs.cloudfoundry.org/version/3.83.0/index.html#create-a-service-instance
            * Nor in SC-OSB
      * [x] Refine USI sync success test: sync update will trigger a new backing update and not enter error recovery branch, just perform the update twise   
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 200 Ok as backing service was completed
      * [x] Check UpdateAsyncInstanceWithBackingServiceAcceptanceTest does 
         * [x] USI  
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 202 accepted, and then 200
      * [x] New concurrent stalled Update async test (ConcurrentAsyncUpdateInstanceWithBackingServiceAcceptanceTest) that does 
         * [x] New interceptor StalledAsyncUpdate
         * [ ] USI  
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 202 Accepted as backing service is still in progress
         * [x] OSB update with invalid plan 
            * [x] 400 Bad request

   * [x] Test bind https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#response-3
      * [x] Refine CSI sync success test  
         * [x] OSB provision dupl same SK: check same dupl receives right status  
            * [x] 200 Ok as backing service key was completed
         * [ ] ~~OSB provision dupl different SI~~: wait until SK params support in CF-java-client, and CCAPI V3 
            * [ ] 409 Conflict
      * [ ] No yet async binding sipport in CF API
   * [x] Implement bind fix
      * [x] catch or new method handleException() that is given any received exception + request
         * existingSk = getServiceKey()
         * [x] compare existingSb params -> delayed until GSBP
            * using GSBP CF API, prereq
               * [ ] GSB support in associated brokers
                  * [ ] COAB
                  * [ ] CF-mysql
         * [x] return existingSk to trigger 200 ok.
   * [x] Test unbind https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#response-9
      * [x] New interceptor StalledAsyncUnbind
      * [x] Refine DSI sync success test  
         * [x] OSB provision dupl same SI: check same dupl receives right status  
            * [x] 200 Ok as backing service was completed
      * [ ] No async bind support in CF.
   * [x] Implement unbind fix


* [x] Fix offending error log: Failing test CreateInstanceWithBackingServiceSyncFailureAcceptanceTest with message
   ```
   reactor.core.Exceptions$ReactiveException: java.lang.AssertionError: Expecting no ERROR log entry in broker recent logs, but got:[LogMessage{applicationId=b09d404b-f017-4a2a-ac8d-487870e93238, message=2020-05-11 16:29:31.131 ERROR 14 --- [nio-8080-exec-4] c.o.o.o.s.OsbCmdbServiceInstance         : Unable to lookup existing service with id=86c1a37f-e853-4964-90a0-5317e00539b7 caught java.lang.IllegalArgumentException: Service instance 86c1a37f-e853-4964-90a0-5317e00539b7 does not exist, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1589214571132120642}]
   ``` 
   * Suspecting race condition to get recent logs, resulting in this log entry usually not being collected, and explaining why this has not triggered before
   
* [x] Reduce polling time in some tests to speed up feedback: from 45s to 5s
   
* [x] Harden binding request handling: validate service instance guid is in the proper org, ie a tenant can't bind a si from  another tenant    
   * Lookup the existing service instance
   * Check that org match
* [x] Harden deprovisionning request handling: validate service instance guid is in the proper org, ie a tenant can't bind a si from another tenant    


* [x] Check smoke test status
   * [x] noop plan unpublished 
      * [ ] relaunch coab broker post-deploy
         * [ ] update config to point to paas-templates-private
            * [ ] add support for http proxy in git


  
* [x] add timeout to reactor blocking calls ? Are cf-java-client timeouts sufficient ?
   * [x] Benchmark how SCAB handles timeout. See related https://github.com/spring-cloud/spring-cloud-app-broker/pull/289#pullrequestreview-323604661
      * api-timeout: sets a reactor level timeout to `cf push`, and `cf delete`
       > Propagate a TimeoutException in case no item arrives within the given Duration. 
       > the timeout before the onNext signal from this Mono
      * api-polling-timeout: sets cf-java-client completionTimeout                                                                                                                                                                                                                               
   * [x] Should osb-cmdb respond to last-operation polling indefinitely if case of a stalled async broker ?
      * Without Osb-cmdb, osb-clients would poll backing service OSB endpoints
         * as long as they wish
         * no longer than [service plan's maximum_polling_duration](https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#polling-interval-and-duration) 
         * eventually waiting after [Retry-After HTTP header](https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-1)
           > The response MAY also include the Retry-After HTTP header. This header will indicate how long the Platform SHOULD wait 
           > before polling again and is intended to prevent unnecessary, and premature, calls to the last_operation endpoint. 
           > It is RECOMMENDED that the header include a duration rather than a timestamp.
      * With Osb-cmdb, CF CC_NG CAPI configures a max polling duration after which it would stop polling. 
   * [x] Check default values
      * Polling timeout for async operation is 5 minutes: https://github.com/cloudfoundry/cf-java-client/blob/8ec06b4cdd61dda0f0ba5e4d546651b880735faa/cloudfoundry-operations/src/main/java/org/cloudfoundry/operations/services/_CreateServiceInstanceRequest.java#L36-L39
         * Affected calls: none, call CloudFoundryOperations async calls were replaced by low-level calls 
      * API connect timeout:
         * Not configured in org.springframework.cloud.appbroker.autoconfigure.CloudFoundryAppDeployerAutoConfiguration.connectionContext()
         * This would protect Osb-cmdb threads against CF API not accepting connections.  
         * Default value in io.netty.channel.DefaultChannelConfig.DEFAULT_CONNECT_TIMEOUT is 30s which sounds sensible
      * CF-Java-Client TLS timeouts
         * Default values at https://projectreactor.io/docs/netty/release/api/reactor/netty/tcp/SslProvider.Builder.html sound sensible
      * CAPI service broker client timeout: 60s
         * [broker_client_timeout_seconds](https://bosh.io/jobs/cloud_controller_ng?source=github.com/cloudfoundry/capi-release&version=1.93.0#p%3dcc.broker_client_timeout_seconds)
            > For requests to service brokers, this is the HTTP (open and read) timeout setting. Default 60s
         * [broker_client_max_async_poll_duration_minutes](https://bosh.io/jobs/cloud_controller_ng?source=github.com/cloudfoundry/capi-release&version=1.93.0#p%3dcc.broker_client_max_async_poll_duration_minutes)
            > The max duration the CC will fetch service instance state from a service broker (in minutes). Default is 1 week                                                                                                                                                                                              
   * [x] ~~Implement new timeouts~~ ? No real added-value w.r.t. default timeouts
      * [ ] Add new configuration entry for api-timeout with a default value of "2 mins"
         * Benefit is to protect 
      * [ ] Add reactor level timeout() calls / block(timeout) to sensitive calls + check error recovery
         * [ ] CSI
         * [ ] DSI
         * [ ] USI
         * [ ] CSB
         * [ ] DSB
   * [x] Validate our assumptions of default timeouts in a test
      * [x] Add an interceptor sync blocking indefinitely on create 
      * [x] Configure Test to not sync wait longer than 90s  
      * [x] Assert that CC API 60s timeout indeed properly triggers
      * [x] Clean up message returned to end-user as to redact service broker url to avoid disclosing broker url + backing service instance guid
        >  Resolved [org.springframework.cloud.servicebroker.exception.ServiceBrokerException: CF-HttpClientTimeout(10001): 
        > The request to the service broker timed out: https://test-broker-app-create-instance-with-sync-backing-timeout.redacted-domain/v2/service_instances/8e6fadf5-f735-4dc9-9eed-4ceb2cac350f]
        * [x] Introduce new collaborator CfApiMessageCleaner
        * [x] Unit test it
        * [x] add a wrapper method in AbstractOsbCmdbService: redactAndReThrowException(Exception e)                                                                                                                                            

* [x] Manually test race conditions against K8S openshift

* [x] Refine AT in CI
   * [x] Adapt clean up script to also clean up backing (spacePerServiceDefinition) spaces 
* [x] refine pipeline notifications to include the commit message
   * [x] fix formatting

* [x] Prepare release 1.0.0 version
   * [x] Design whether/how to clean up scab code from osb-cmdb code base
      * Criterias/goals (highest priority first)
         * Build faster
         * Faster IDE because less code loaded/managed
         * Simplify contributions (by other team members and by the community)
            * Avoid google/github search request to index/return scab dead code hosted by osb-cmdb
         * Still be able to be inspired-by/reuse scab component tests, acceptance test, unit tests, reactor syntax, gradle build
            * Compare with the same clone scab remotes
            * Compare/cherry picky upcoming commits in SCAB
         * Remove github public fork relationship
         * Faster git clone
      * Approaches   
         * keep scab code but don't build it in gradle/ide
         * **clean up scab code from develop branch**
            * rename module `spring-cloud-app-broker-integration-tests` into `osb-cmdb-integration-tests` 
         * clean up scab code from git history
   * [x] Collect inspiration pointers in acceptance tests:
      * Junit experimental `@TestMethodOrder` for smaller test case methods: org.springframework.cloud.appbroker.acceptance.UpgradeInstanceAcceptanceTest
      * Use Optional instead of null: UpdateInstanceWithNewServiceAcceptanceTest
         ```
           Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
           		assertThat(backingApplication).hasValueSatisfying(app ->
           			assertThat(app.getRunningInstances()).isEqualTo(1)); 
         ```
      * Health listener used when backing application is updated: org.springframework.cloud.appbroker.acceptance.UpdateInstanceAcceptanceTest
         ```
                //Given a backing application is serving traffic
        		String path = backingApplication.get().getUrls().get(0);
        		healthListener.start(path);
                        
                //When the backing application is updated with zero-down-time
                ...
        
        		// then the backing application was updated with zero downtime
        		healthListener.stop();
        		assertThat(healthListener.getFailures()).isEqualTo(0);
        		assertThat(healthListener.getSuccesses()).isGreaterThan(0); 
         ```
      * ManagementController: a rest controller within AT to test BackingAppManagementService to start/stop/restage/restart backing applications
         * Not mentioned in https://docs.spring.io/spring-cloud-app-broker/docs/current/reference/html5/
         * Likely used by brokers leveraging SCAB wiht custom endpoints to manage backing apps in batch
   * [x] Perform clean up scab code from osb-cmdb code base
      * [x] Update status of component test usage: 
         * all components tests are currently ignored, including Dynamic catalog 
         * components tests have been moved to `osb-cmdb` module along with test fixtures: no more dependency on `spring-cloud-app-broker-integration-tests` 
      * [x] clean up AT:
          * [x] Ease identification of Cmdb AT and speed up compilation steps
             * [x] Delete unneeded scab test, once we get sufficient inspiration    
         * [ ] rename module `spring-cloud-app-broker-acceptance-tests` into `osb-cmdb-acceptance-tests`: delayed 
            * Delayed until SCAB prioritizes spinning off acceptance tests in a 1st class repo
               * In order to ease getting updates in the meantime.
   * [x] Update user documentation 
   * [x] Update release procedure: should the root project version be sufficient ? 
   * [x] Update design documentation 
      * [x] Document Test strategy 
