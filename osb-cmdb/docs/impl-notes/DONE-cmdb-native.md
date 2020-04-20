* [x] Fix CI https://circleci.com/gh/orange-cloudfoundry/osb-cmdb-spike/282
   * [x] **Fix cmdb UT**
        >   ApplicationConfigurationIntegrationTest > paas_templates_overrides_default_cmdb_config_in_application_default_yml_Overrides_application_yml() FAILED
        >       java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:132
        >           Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException at ConstructorResolver.java:798
        >               Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException at DefaultListableBeanFactory.java:1700
        >   FAILED test: com.orange.oss.osbcmdb.OsbCmdbServiceInstanceTest > createServiceInstanceWithTarget()
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
