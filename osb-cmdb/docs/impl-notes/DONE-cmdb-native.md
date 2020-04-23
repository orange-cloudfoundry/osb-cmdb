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

