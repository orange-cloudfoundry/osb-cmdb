
Metadata impl
 - Implement and test for cf update-service
 - DONE: Refine annotations to store additional annotations (when available to SCOSB, marked as X):
    - X-Broker-Api-Version
    - X-Api-Info-Location (X)
    - User-Agent
    - X-Broker-API-Request-Identity
 - DONE: Handle circle ci test failures:
    - org.springframework.cloud.appbroker.integration.UpdateInstanceWithServicesComponentTest > updateAppWithServices() 
       >  -----------------------------------------------------------------------------------------------------------------------
       >  | Closest stub                                             | Request                                                  |
       >  -----------------------------------------------------------------------------------------------------------------------
       >                                                             |
       >  GET                                                        | GET
       >  /v2/spaces/TEST-SPACE-GUID/security_groups                 | /v2/spaces/TEST-SPACE-GUID/service_instances?q=name:my-db<<<<< URL does not match
       >                                                             | -service&page=1&return_user_provided_service_instances=tr
       >                                                             | ue
       >                                                             |
       >                                                     

       - osb-cmdb side effect of disabled optimization and systematic backing service update ? => disable the test                                                                             
          
    - org.springframework.cloud.appbroker.integration.UpdateInstanceWithServicesParametersComponentTest > updateAppWithBackingServicesParameters()
       
       >    java.lang.IllegalArgumentException: Plan does not exist for the my-db-service service
       >    	at org.cloudfoundry.util.ExceptionUtils.illegalArgument(ExceptionUtils.java:45)
       >    	at org.cloudfoundry.operations.services.DefaultServices.getOptionalValidatedServicePlanId(DefaultServices.java:511)
       - osb-cmdb side effect of disabled optimization and systematic backing service update ? => disable the test                                                                             
                                 
                                                                                                                            
 - DONE: Fill in annotations and labels for K8S client
 - DONE: Ensure metadata are also assigned when the service creation fails:
    - would need duplicating the metadata assignment sequence with a doOnError https://projectreactor.io/docs/core/release/reference/#_log_or_react_on_the_side
    - Q: how to test ?
       - Component test
          - Inject Metadata transformer: CreateBackingServicesMetadataTransformationService 


- DONE: Modified workflow 
   - breaks tests that asserts strictly BackendServices content
       - remove annotations from equals/hashcode
       - duplicate BackendServices in transformer ?
       - complexify tests to have passing asserts
       - **simplify tests to not assert BackingServices content**
          - mock return constant
          - mock return captured argument
          - replace mock with fake
   - how to assert the metadata is set ?
      - assert the mutation in a specific unit test
      - only assert invocation to AbstractBackingServicesMetadataTransformationService ??
      - assert the resulting backing service has annotations  
      - **don't test it within SCAB, plan to move this code out of SCAB**    
   - how to assign `backing_service_instance_guid` (since the id is only known in CloudfoundryDeployer) ?
      - directly in CloudfoundryDeployer.createService()
         - using a constant defined in a collaborator
      - in collaborator
      

- Bug catalog yml serialization due to spring-cloud-open-service-broker. Catalog fails to load with message
    >     Schemas
    >        Schema service_binding.create.parameters is not valid. Schema must have $schema key but was not present

   - where to make the fix ?
      - once serialized as json string
             - **as a plain string replace**
                 - might be fragile as sensitive to changes in json pretty print. 
                    - However protected by unit tests
                 - **would be missing invalid schema URIs** 
             - as a rexexp ignoring white spaces
                 - more complex and error prone 
      - before as a JsonTree by walking on it
         - more complex and error prone 

- Bug:
    > java.lang.IndexOutOfBoundsException: Source emitted more than one item
    > Error has been observed by the following operator(s):
    >	Flux.single ⇢ org.cloudfoundry.operations.services.DefaultServices.getSpaceService(DefaultServices.java:608)
    >	FluxMap$MapSubscriber.onNext ⇢ reactor.ipc.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:213)
    >	Mono.onErrorResume ⇢ org.cloudfoundry.operations.services.DefaultServices.getSpaceService(DefaultServices.java:609)
    >	Mono.map ⇢ org.cloudfoundry.operations.services.DefaultServices.getServiceIdByName(DefaultServices.java:570)
    >	Mono.zip ⇢ org.cloudfoundry.operations.services.DefaultServices.lambda$createInstance$5(DefaultServices.java:159)
    >	Mono.flatMap ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:159)
    >	Mono.flatMap ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:164)
    >	Mono.flatMap ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:169)
    >	Mono.flatMap ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:174)
    >	Mono.doOnSubscribe ⇢ org.cloudfoundry.operations.util.OperationsLogging.lambda$log$3(OperationsLogging.java:60)
    >	Mono.doFinally ⇢ org.cloudfoundry.operations.util.OperationsLogging.lambda$log$3(OperationsLogging.java:61)
    >	Mono.transform ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:175)
    >	Mono.checkpoint ⇢ org.cloudfoundry.operations.services.DefaultServices.createInstance(DefaultServices.java:176)
    >	Mono.then ⇢ org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer.lambda$createServiceInstance$104(CloudFoundryAppDeployer.java:1012)
    >	Mono.flatMap ⇢ org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer.createServiceInstance(CloudFoundryAppDeployer.java:1010)
    >	Mono.then ⇢ org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer.createServiceInstance(CloudFoundryAppDeployer.java:1008)
    >	Mono.doOnRequest ⇢ org.springframework.cloud.appbroker.deployer.DeployerClient.createServiceInstance(DeployerClient.java:104)
    >	Mono.doOnSuccess ⇢ org.springframework.cloud.appbroker.deployer.DeployerClient.createServiceInstance(DeployerClient.java:105)
    >	Mono.doOnError ⇢ org.springframework.cloud.appbroker.deployer.DeployerClient.createServiceInstance(DeployerClient.java:106)
    >	Mono.map ⇢ org.springframework.cloud.appbroker.deployer.DeployerClient.createServiceInstance(DeployerClient.java:108)
    >	ParallelFlux.flatMap ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:42)
    >	ParallelFlux.sequential ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:43)
    >	Flux.doOnRequest ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:44)
    >	Flux.doOnEach ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:45)
    >	Flux.doOnComplete ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:46)
    >	Flux.doOnError ⇢ org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService.createServiceInstance(DefaultBackingServicesProvisionService.java:47)
    >	Mono.flatMapMany ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.createBackingServices(AppDeploymentCreateServiceInstanceWorkflow.java:83)
    >	Flux.doOnRequest ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.createBackingServices(AppDeploymentCreateServiceInstanceWorkflow.java:84)
    >	Flux.doOnComplete ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.createBackingServices(AppDeploymentCreateServiceInstanceWorkflow.java:86)
    >	Flux.doOnError ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.createBackingServices(AppDeploymentCreateServiceInstanceWorkflow.java:88)
    >	Flux.thenMany ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.create(AppDeploymentCreateServiceInstanceWorkflow.java:70)
    >	Flux.thenMany ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.create(AppDeploymentCreateServiceInstanceWorkflow.java:70)
    >	Flux.then ⇢ org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.create(AppDeploymentCreateServiceInstanceWorkflow.java:71)
    >	Flux.concatMap ⇢ org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService.invokeCreateWorkflows(WorkflowServiceInstanceService.java:118)
    >	Flux.doOnRequest ⇢ org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService.create(WorkflowServiceInstanceService.java:102)
    >	Flux.doOnComplete ⇢ org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService.create(WorkflowServiceInstanceService.java:103)


- DONE Bug:
 - symptom 
    > Finished creating backing service onError(java.lang.IllegalArgumentException: Service p-mysql-cmdb does not exist
 - diagnostics
    - invalid BrokeredServices definition
                                                 >
    >    - serviceName: "p-redis-cmdb"
    >      planName: "dedicated-vm"
    >      apps: null
    >      services:
    >      - serviceInstanceName: "p-redis-cmdb"
    >        name: "p-redis-cmdb"
    >        plan: "dedicated-vm"
    >        parameters: {}
    >        properties: {}
    >        parametersTransformers: []
    >        rebindOnUpdate: false
    >      target:
    >        name: "SpacePerServiceDefinition"
    >

    - wrong jar deployed ?
       - add SCM to gradle-built jar
    - error during rebase
       - cherry picked lost commit
          - error during conflict handling ?
       - autoconfig test case checks suffix is propertly loaded in properties
       - sdmapper test asserts suffix is removed
           

Status: 
- DONE: finish dump writing to disk + echo message on stdout
- pause on debugging the test
   - later, ask support on stack overflow:
      - "how to debug spring binder issues in tests ?"
         - https://spring.io/blog/2018/03/28/property-binding-in-spring-boot-2-0
      - "how to test application.yml in unit tests ?"
      - "how to test & debug springboot configuration binding in unit tests ?"
- squash & release 


- DONE: understand and fix why ServiceDefinitionMapperProperties isn't filled in org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfigurationTest.serviceDefinitionMapperPropertiesAreProperlyLoaded
    - the bean is defined but not filled with its value "prefix"
    - diagnostics
        - tried triggering context loading failure with @NotBlank without luck https://www.baeldung.com/configuration-properties-in-spring-boot
        - try with real context loader to make this also reproduces 
    - test fixes
        - tried with property value and system properties
    - fix: missing @EnableConfigurationProperties
  

Bug: 
CF-ServiceBrokerRequestRejected(10001): The service broker rejected the request to https://osb-cmdb-broker.redacted-domain.org/v2/service_instances/2579ab08-3194-4de3-b9db-1f416092f317?accepts_incomplete=true. Status Code: 404 Not Found, Body: 404 Not Found: Requested route ('osb-cmdb-broker.redacted-domain.org') does not exist.

Can cloudfoundry send the request to the wrong broker ?

$ cf create-service p-mysql-cmdb 10mb osb-cmdb-broker-0-smoketest-1575470504 -b osb-cmdb-broker-0

 2019-12-04T14:41:51.31+0000 [APP/PROC/WEB/0] OUT 2019-12-04 14:41:51.311 ERROR 6 --- [ry-client-nio-8] o.s.c.appbroker.deployer.DeployerClient  : Error creating backing service p-mysql-cmdb with error 'CF-ServiceBrokerRequestRejected(10001): The service broker rejected the request to https://osb-cmdb-broker.redacted-domain.org/v2/service_instances/2579ab08-3194-4de3-b9db-1f416092f317?accepts_incomplete=true. Status Code: 404 Not Found, Body: 404 Not Found: Requested route ('osb-cmdb-broker.redacted-domain.org') does not exist.

- Q: could be brokered services be incorrect when suffix is provided ? 
- A: yes :-( missing component test
- Options
    - Add a suffix after the ServiceDefinitions were created. i.e. clone them
       - Pb: SCOSB's builder are missing a from()
          - contribute to SCOSB
          - factor this copy constructor in ServiceDefinitionMapper
       - Pb: suffix also needs to be applied to BrokeredService 
    - Fetch the service definitions a 2nd time for brokered services
    - Modify DynamicServiceImpl to map the ServiceEntity twice: once for catalog and brokered services (with suffix) and once for backend services (without suffix)
       - also need to match brokered service to backend service
       - which return values ?
           - two lists ?
           - a list of Tuples ?
    - **Remove the suffix in the BrokeredServicesCatalogMapper**
       - what about plan suffixes ? same post proccessing 
       - pros: 
        - simpler than complexifying DynamicCatalogServiceImpl#fetchServiceDefinitions() which is already complex
       - cons
        - feels a bit hacky 




Next step:
- Refine dynamic catalog configuration
    - broker excludes
       - DONE: prepare test case in DynamicServiceAutoConfigurationComponentTest
       - DONE implement filtering in DynamicCatalogServiceImpl 
       
    - squash commit 
    - run tests & static code analysis
    - PR autoconfig small change (cherrypick)
    - test the error case: should fail fast and clean
    - dump catalog and backing services as yaml on disk & stdout ?
       - Pb: enable to load generated yml into config properties (same pb as for ContextRunner) with SCPSB context code
       - same syntax
       - different classpath / lib versions ?
       - different imports ? 
    - conditionally trigger autoconfiguration depending on property
    - add filtering controlled by properties
    - add filtering based to service plan visibilities
    - integration tests using recorded mocks that can run on circleci
       - look at cf-java-client tests for inspiration ? 
    - override target strategy for some services ??




- DONE: Diagnose and fix invalid schema returned in catalog
   >       Schema service_instance.create.parameters is not valid. Schema must have $schema key but was not present
   >             "schemas": {
   >               "service_instance": {
   >                 "create": {
   >                   "parameters": {
   >                     "$schema": null,
   >                     "type": null,
   >                     "properties": null
   >                   }
   >                 },
   - Hypothesis:
      - Cf returns empty schemas that PlanMapper maps into nulls, that json serialization serializes as null
      - Plan json serializations serializes extra object
   - Diagnostics
      - Reproduce with component tests       
      - Reproduce with acceptance tests 
   - Possible fixes: 
      - post-process Plan object to remove Schemas object that are empty
      - replace json serialize/deserialize with builder mapping:
         - schemas        
         - schemas        
                                                     


- DONE launch paas-templates smoke tests
  - DONE missing strategy in PlanMapper
  - DONE add suffixes to service names: -cmdb
    - inject DynamicCatalogProperties to other beans
       - Q: inject     
  - adapt smoke tests to not expected suffix ?
          

          
       - DONE: Proceed with unit testing of autoconfig: https://www.baeldung.com/spring-boot-context-runner
          - DONE: Inject Mock for DynamicCatalogService
             - option 1:
                 - Conditionnally using 
                        >	@Autowired(required = false)
                        >	DynamicCatalogService dynamicCatalogService) {
                 - If not dependencies missing throw exception using https://www.baeldung.com/spring-assert
             - option 2: systematically with constructor injection a DynamicCatalogService bean defined in a new distinct auto config class   
             - **option 3: systematically with method injection a DynamicCatalogService bean defined in a new distinct auto config class**   
          - DONE: Pb: context is empty
              - Try to turn on logs 
                 > -Dlogging.level.org.springframework=trace
                 > -Dlogging.level.org.springframework.boot=trace
                 > -Dlogging.level.org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener=debug
                 > -Ddebug=true
                 - step into debugger: 
                    - slf4j level not set
                    - ConditionEvaluationReportLoggingListener don't pop in debugging
              - Q: no debugging traces available in sprinboottest supporting class ?
             - some warn logs are displayed when forcing a context error
               > 02-12-2019 11:18:26.172 [main] WARN  o.s.c.a.AnnotationConfigApplicationContext.refresh - Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'dynamicCatalogServiceAutoConfiguration': Unsatisfied dependency expressed through constructor parameter 0; nested exception is org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
               - The associated spring source code is using from Gradle: org.slf4j:jcl-over-slf4j:1.7.28
                  >    import org.apache.commons.logging.Log;
                  >    import org.apache.commons.logging.LogFactory;
                  >	/** Logger used by this class. Available to subclasses. */
                  >	protected final Log logger = LogFactory.getLog(getClass());
              - trying to have slf4j display its logs through has no effect https://stackoverflow.com/questions/3752921/is-it-possible-to-make-log4j-display-which-file-it-used-to-configure-itself
                  > -Dlog4j.debug=true
                  - runs into a distinct JVM ?
                     > /usr/lib/jvm/java-8-openjdk-amd64/bin/java -ea  -D .. com.intellij.rt.execution.application.AppMainV2 com.intellij.rt.execution.junit.JUnitStarter -ideVersion5 -junit5 org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfigurationTest
                      - print system properties: they properly display
                  - logback config problems ?
                     - https://stackoverflow.com/questions/48458052/logback-configuration-file-not-loaded 
                         >  Check the classpath, does it contain commons-logging as well as jcl-over-slf4j? If it does, exclude commons-logging and see if that works. I've had problems with apps when they have both dependencies, they seem to conflict.
                     - error triggers, debug does not trigger
                     - modify osb-cmdb/spring-cloud-app-broker-autoconfigure/src/test/resources/logback.xml instead of env variables !!!
  
              
         



- DONE refactor to make it easier to test, configure and implement new features
    - DONE: design interactions and responsibilities
       - DONE: DynamicCatalogServiceAutoConfiguration
          - conditional on opt-in
          - exposes catalog and brokered services beans
          - creates Mapper Beans
       - DONE: DynamicCatalogService bean
          - depends on CF client
          - Catalog getCatalog(): Catalog
       - DONE: ServiceDefinitionMapper bean
           > ServiceDefinition toServiceDefinition(ServiceResource resource,
           >  		List<ServicePlanResource> servicePlans)    
          - in the future, also takes an associated List<ServicePlanVisibilityEntity>                             
           >    public final class ServicePlanVisibilityEntity 
           >        extends org.cloudfoundry.client.v2.serviceplanvisibilities._ServicePlanVisibilityEntity {
           >      private final @Nullable String organizationId;
           >      private final @Nullable String organizationUrl;
           >      private final @Nullable String servicePlanId;
           >      private final @Nullable String servicePlanUrl;                          
           >                              
          - DONE: controlled by an associated ServiceDefinitionMapperProperties class 
             - DONE brokers/service excludes
             - brokers includes
             - service includes/excludes
             - DONE: service offering suffix
             - service offering prefix
          - DONE: With associated unit test 
       - PlanMapper bean
           > Plan toPlan(ServicePlanResource resource)
          - controlled by an associated PlanMapperProperties class 
             - plan includes/excludes
             - plan prefix/suffix ??
          - With associated unit test 
       - BrokeredServicesCatalogMapper bean
          > BrokeredServices toBrokeredServices(Catalog)
          - controlled by an associated BrokeredServiceMapperProperties class 
             - strategy to use ?
          - With associated unit test 
    - DONE extract classes
       - Q: can lambda reference instance methods ?  
          - https://www.codementor.io/eh3rrera/using-java-8-method-reference-du10866vx
            > (obj, args) -> obj.instanceMethod(args)
          - in our case obj needs to be final immutable accessible
          - get inspiration from Cf Java Client
          - expand lambda
       - DONE: BrokeredServicesCatalogMapper    
    - DONE fix fromJson: custom mapper should only be registered for plan
    - introduce properties classes & inject them to mappers
       - choose the structure for properties: 
          - scab or osb-cmdb prefix
          - single class, or class per Mapper ?
             - **start simple with a single one** 
          - Profiles can help selection which scanned configuration classes to load https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-profiles 
       - Q: can we have multiple @ConfigurationProperties with the same path ?
          - not clearly documented, try it
       - Q: how will test provide properties ?
          - @TestPropertySource ?
          - Inject properties in context
          	>	this.contextRunner
          	>		.withPropertyValues(
          	>			"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.com",
          - Instanciate bean explicitly in test configuration class
       - Q: how to support general opt-in for dynamic catalog ?
          - A mandatory field in the properties which must be set to true 
            - @AssertTrue
            - Catalog and BrokeredServices are @ConditionalOnBean(DynamicCatalogProperties)
          - @ConditionalOnProperty on the autoconfig class 
            >  @ConditionalOnProperty(
            >      value="module.enabled", 
            >      havingValue = "true"
       - DONE Initiate a new unit test to prototype this
          
          


DONE acceptance test pass but code should be vastly simplified, as well as unit test (still failing)
Check back the custom deserialized and and reparse the nested json string. Would avoid the ugly/fragile builder copy.

DONE: Cover the case of null extra into a component test

- dynamic catalog configuration:
  - DONE update component test for plan mapping
     - DONE: sample json schemas
     - DONE extra plan fields
        - contains json serialized string, how to deserialize it since it is not mapped in SCOSB's Plan ?
          > @JsonInclude(Include.NON_NULL)
          >  public class Plan {
          >  
          >  	@NotEmpty
          >  	private final String id;
          >   ...
          >  	private final Map<String, Object> metadata;
           - DONE Configure ObjectMapper with field renaming
              - Pb: Plan does not have setters for Jackson to assign fields, it fails using the constructor when metadata is provided
                 >	private final Map<String, Object> metadata;
                 >
                 >	Plan(String id, String name, String description, Map<String, Object> metadata, Boolean free, Boolean bindable, Schemas schemas) {
                 >		this.id = id;
                 >		this.name = name;
                 > java.lang.IllegalStateException: com.fasterxml.jackson.databind.exc.MismatchedInputException: 
                 > Cannot construct instance of `java.util.LinkedHashMap` (although at least one Creator exists): 
                 > no String-argument constructor/factory method to deserialize from String value ('{                                                                                                                     
                 - subclass Plan to provide accessors for all fields
                 - configure jackson to find the right constructor
                 - PR SCOsb to support Json parsong
                 - assign metadata after the Plan construction
           - Load into another intermediate POJO under our control before converting to SCOSB's Plan ?
           - preprocess the Json to rename extra into metadata 
              - using Json document Pojo
              - using plain string replace
              
                                                                                                              
  - DONE missing plan metadata, including costs: present in ServicePlanEntity: String extra. Need to Json deserialize it and store it into Plan: Map<String, Object> metadata;


DONE: try to run dynamic catalog config with controlled wiremocks reponse in component tests modules
- test current autoconfig spring context with wiremocks in component tests
    - Spring test context contains unorchestred beans available as @autowired
       - wiremock server
       - CF client
    - Unit test instanciates DynamicCatalogService bean explicitly, and con
     
- DONE wire cf client to localhost
   - cf client beans
   - target properties
- DONE start wiremock
   - import wiremock autoconfiguration 
      - how to trigger spring context loading and orchestration of autoconfigs to have wiremock started before dynamic catalog ?
          - Use ApplicationContextRunner programmatically
             - No autowired fields, need to fetch them manually
          - Use Spring junit annotation @ContextConfiguration
             - use initialize Before annotation to control scheduling
             - configure mocks outside of Spring context 
- DONE configure wiremock with stubs
   - Can't inherit from WiremockComponentTest since it triggers loading of the spring app
     > @SpringBootTest(
     > 	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
     > 	classes = {AppBrokerApplication.class,
     > 		WiremockServerFixture.class,
     > 		OpenServiceBrokerApiFixture.class,
     > 		CloudControllerStubFixture.class,
     > 		UaaStubFixture.class,
     > 		CredHubStubFixture.class,
     > 		TestBindingCredentialsProviderFixture.class},
     - Duplicate it instead
   - CloudControllerStubFixture does not exactly match our needs, but relies on many private methods that are expensive to duplicaye
      - Modified it.
- DONE tune the wiremock responses to return comprehensive service 

  - DONE: start broker locally to test it within springboot
  - DONE: manually review exposed catalog
  - DONE: tests that run independently of a live CF API with controlled CF marketplace responses
     - Split in 2 objects: 
        - one with dependencies: 
        - one disconnected: List<ServiceDefinitions>: unit tested without static methods
     - cf java client mocks
        - Get inspiration from cf-java-client tests
          >  org.cloudfoundry.operations.services.DefaultServicesTest#listServiceOfferings
          >  @Test
          >  public void listServiceOfferings() {
          >      requestListSpaceServicesTwo(this.cloudFoundryClient, TEST_SPACE_ID, "test-service1", "test-service2");
          >      requestListSpaceServicePlans(this.cloudFoundryClient, "test-service1-id", "test-service1-plan", "test-service1-plan-id");
          >      requestListSpaceServicePlans(this.cloudFoundryClient, "test-service2-id", "test-service2-plan", "test-service2-plan-id");
          >
            - Still requires to set up extra nested json
     - cf java client Fake
         > cloudFoundryClient.servicePlans()
         >       Mono<ListSpaceServicesResponse> listServices(ListSpaceServicesRequest request);                     
         > cloudFoundryClient.spaces()
         >       Mono<ListServicePlansResponse> list(ListServicePlansRequest request);
         > CloudFoundryOperations
         >      DefaultCloudFoundryOperations.builder()
         >       				.from(cfOperations)
         >       				.organization(targetProperties.getDefaultOrg())
         >       				.space(targetProperties.getDefaultSpace())
         >       				.build()                                                            
     - CF API stubs
        - Only reuse Stubs but only run the autoconfiguration class within spring context, not the full app
        
        - copied CatalogComponentTest -> DynamicCatalogComponentTest
        - SUSPENDED: Running DynamicCatalogComponentTest which launches the full SpringApp
           - adapt properties to run localhost ? 
           - pb with gradle dependencies: OsbCmdbApplication not found
              - **move autoconfig class in autoconfig project and use component test application instead**
                 - Q: do we want this in the long run ?
                 - Pros
                    - Be able to leverage SCAB component and acceptance tests, with dynamicCatalog opt-in
                 - Cons
                    - Harder to avoid forks, CMDB tests are within SCAB 
              - duplicate component test
           - Pb: autoconfig class is not loaded. Is component scan missing ?
              - diagnostics
                  - debugger breakpoint
                  - springboot debug traces 
              - fixes:
                  - **register in spring.factories**
           - Pb: autoconfig class starts before wiremock is started.
              - Control wiremock to start earlier
                 - Triggered in org.springframework.cloud.appbroker.integration.fixtures.WiremockServerFixture.startWiremock
                 - When ?
                    - Junit5 @BeforeAll
                    - **Spring Bean initialization in WiremockServerFixture**
            - Pb: mocks are not be registered soon enough
               - initialize mocks with required stubs common to dynamic catalog tests (other tests will not have autoconfig opt-in)
                  - in the DynamicCatalogComponentTest
                     - by getting the wiremock stub injected
                         > 	@Autowired
                         >  	private CloudControllerStubFixture cloudControllerFixture;
                     - by triggering wiremock initialization prior to DynamicCatalogAutoConfig
                        - using @AutoConfigureBefore(AnotherConfig.class) 
               - delay autoconfig bean instanciation, 
                  - explore springcontext phases 
                     - @PostConstruct and @PreDestroy https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-java-lifecycle-callbacks
                        - Catalog bean 
                           - currently defined as a class org.springframework.cloud.servicebroker.model.catalog.Catalog 
                              > public class Catalog {}
                        - SubClass it as LazyCatalog extends Catalog 
                           - injected the DynamicCatalogService bean
                           - initialized with @PostConstruct                                                                                                     
                  - explore springboot application events https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-application-events-and-listeners
                  - lazy loaded bean https://www.baeldung.com/spring-lazy-annotation with @Lazy
                      - implies adding @Lazy to all @Autowired Catalog and BrokeredServices beans
                      - how does this co-exist with @ConditionalOnMissingBean from org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration.beanCatalogService ?
                        > 	@Configuration
                        > 	@ConditionalOnMissingBean({Catalog.class, CatalogService.class})
                        > 	@EnableConfigurationProperties(ServiceBrokerProperties.class)
                        > 	@ConditionalOnProperty(prefix = "spring.cloud.openservicebroker.catalog.services[0]", name = "id")
                        > 	protected static class CatalogPropertiesMinimalConfiguration {
                        > 
                        > 		private final ServiceBrokerProperties serviceBrokerProperties;
                        > 
                        > 		public CatalogPropertiesMinimalConfiguration(ServiceBrokerProperties serviceBrokerProperties) {
                        > 			this.serviceBrokerProperties = serviceBrokerProperties;
                        > 		}
                        > 
                        > 		@Bean
                        > 		public Catalog catalog() {
                        > 			return this.serviceBrokerProperties.getCatalog().toModel();
                        > 		}
                        > 	}
                      
                        > 	@Bean
                        >  	@ConditionalOnMissingBean(CatalogService.class)
                        >  	public CatalogService beanCatalogService(@Autowired(required = false) Catalog catalog) {
                           
                        >  	@Bean
                        >  	@ConfigurationProperties(PROPERTY_PREFIX + ".services")
                        >  	@ConditionalOnMissingBean
                        >  	public BrokeredServices brokeredServices() {
                        >  		return BrokeredServices.builder().build();
                        >  	}                                                                                                                                                                          
                      - implies initialization time in uncontrolled and will happen at first access. 
                         - Catalog: org.springframework.cloud.servicebroker.service.BeanCatalogService.initializeMap
                            >	public BeanCatalogService(Catalog catalog) {
                            >		this.catalog = catalog;
                            >		initializeMap();
                            >	}
                            > 	private void initializeMap() {
                            >  		catalog.getServiceDefinitions().forEach(def -> serviceDefs.put(def.getId(), def));
                            >  	}
                              - replace BeanCatalogService with LazyBeanCatalogService
                                >  	@Bean
                                >  	@ConditionalOnMissingBean(CatalogService.class)
                                >  	public CatalogService beanCatalogService(@Autowired(required = false) Catalog catalog) {
                                >  		if (catalog == null) {
                                >  			throw new CatalogDefinitionDoesNotExistException();
                                >  		}
                                >  		return new BeanCatalogService(catalog);
                                >  	}

                         - BrokeredServices, seems only stored so far in beans
                                                                                                                                                                        
                                > 	@Bean
                                > 	public BackingAppManagementService backingAppManagementService(ManagementClient managementClient,
                                > 		AppDeployer appDeployer, BrokeredServices brokeredServices, TargetService targetService) {
                                > 		return new BackingAppManagementService(managementClient, appDeployer, brokeredServices, targetService);
                                > 	}
                                > 	
                                >  @Bean
                                >  public UpdateServiceInstanceWorkflow appDeploymentUpdateServiceInstanceWorkflow(
                                >      BrokeredServices brokeredServices,
                                >      BackingAppDeploymentService backingAppDeploymentService,
                                >      BackingServicesProvisionService backingServicesProvisionService,
                                >      BackingApplicationsParametersTransformationService appsParametersTransformationService,
                                >      BackingServicesParametersTransformationService servicesParametersTransformationService,
                                >      TargetService targetService) {
                                >
                                >      return new AppDeploymentUpdateServiceInstanceWorkflow(
                                >          brokeredServices,
                                >          backingAppDeploymentService,
                                >          backingServicesProvisionService,
                                >          appsParametersTransformationService,
                                >          servicesParametersTransformationService,
                                >          targetService);
                                >  }

                               	
               
                    




- CSI/USI does not yet return dashboard url 
  - Need to subclass workflow impl buildResponse() to set dashboard URL.
  - However, buildResponse isn't provided backing app/service response
  - Would need to change prototype from CreateServiceInstanceWorkflow
  > from:
  > Mono<Void> create(CreateServiceInstanceRequest request, CreateServiceInstanceResponse response)
  > to: 
  > Mono<Void> create(CreateServiceInstanceRequest request, CreateServiceInstanceResponseBuilder responseBuilder)
  - And remove CreateServiceInstanceWorkflow.buildResponse()
     - Update caller org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService
        > Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request)
        - Understand cases where threading memory model should be given care 
           > 	private Mono<CreateServiceInstanceResponse> invokeCreateResponseBuilders(CreateServiceInstanceRequest request) {
           >  		AtomicReference<CreateServiceInstanceResponseBuilder> responseBuilder =
           >  			new AtomicReference<>(CreateServiceInstanceResponse.builder());
     - Update callees to return the backingService CSIResp, currently returns void, and CreateServiceInstanceResponse param is immutable
           > org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow.AppDeploymentCreateServiceInstanceWorkflow:
           > CreateServiceInstanceWorkflow {
           > 	default Mono<Void> create(CreateServiceInstanceRequest request,
           > 							  CreateServiceInstanceResponse response) {
           > 		return Mono.empty();
           > 	}
           - Downcalls are returning the name of the backing service created
           >
           > 	private Flux<String> createBackingServices(CreateServiceInstanceRequest request) {
           > 		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
           > 			.flatMap(backingServices ->
           > 				targetService.addToBackingServices(backingServices,
           > 					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
           > 					request.getServiceInstanceId()))
           > 			.flatMap(backingServices ->
           > 				servicesParametersTransformationService.transformParameters(backingServices,
           > 					request.getParameters()))
           > 			.flatMapMany(backingServicesProvisionService::createServiceInstance)
           >
           > org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService
           >  	public Flux<String> createServiceInstance(List<BackingService> backingServices) {
           >  		return Flux.fromIterable(backingServices)
           >  			.parallel()
           >  			.runOn(Schedulers.parallel())
           >  			.flatMap(deployerClient::createServiceInstance)
           >  			.sequential()
           >  			.doOnRequest(l -> log.debug("Creating backing services {}", backingServices))
           >  			.doOnEach(response -> log.debug("Finished creating backing service {}", response))
           >  			.doOnComplete(() -> log.debug("Finished creating backing services {}", backingServices))
           >  			.doOnError(exception -> log.error(String.format("Error creating backing services %s with error '%s'",
           >  				backingServices, exception.getMessage()), exception));
           >  	}
           >   	
           > org.springframework.cloud.appbroker.deployer.DeployerClient 	
           >   	Mono<String> createServiceInstance(BackingService backingService) {
           >   		return appDeployer
           >   			.createServiceInstance(
           >   				CreateServiceInstanceRequest
           >   					.builder()
           >   					.serviceInstanceName(backingService.getServiceInstanceName())
           >   					.name(backingService.getName())
           >   					.plan(backingService.getPlan())
           >   					.parameters(backingService.getParameters())
           >   					.properties(backingService.getProperties())
           >   					.build())
           >   			.doOnRequest(l -> log.debug("Creating backing service {}", backingService.getName()))
           >   			.doOnSuccess(response -> log.debug("Finished creating backing service {}", backingService.getName()))
           >   			.doOnError(exception -> log.error(String.format("Error creating backing service %s with error '%s'",
           >   				backingService.getName(), exception.getMessage()), exception))
           >   			.map(CreateServiceInstanceResponse::getName);
           >   	}
           >   	
           > org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer  	
           >  	@Override
           >  	public Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
           >  		String appName = request.getName();
           >  		Resource appResource = getAppResource(request.getPath());
           >  		Map<String, String> deploymentProperties = request.getProperties();
           >  
           >  		logger.trace("Deploying application: request={}, resource={}",
           >  			appName, appResource);
           >  
           >  		return pushApplication(request, deploymentProperties, appResource)
           >  				.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
           >  				.doOnSuccess(item -> logger.info("Successfully deployed {}", appName))
           >  				.doOnError(error -> {
           >  					if (isNotFoundError().test(error)) {
           >  						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
           >  					}
           >  					else {
           >  						logError(String.format("Failed to deploy %s", appName)).accept(error);
           >  					}
           >  				})
           >  				.thenReturn(DeployApplicationResponse.builder()
           >  					.name(appName)
           >  					.build());
           >  	}
           >
           > org.springframework.cloud.appbroker.deployer.DeployApplicationResponse
           >  public class DeployApplicationResponse {
           >  
           >  	private final String name;
           >  }
           >  
           > 	@Override
           > 	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
           >         org.cloudfoundry.operations.services.CreateServiceInstanceRequest createServiceInstanceRequest =
           >             org.cloudfoundry.operations.services.CreateServiceInstanceRequest
           >                 .builder()
           >                 .serviceInstanceName(request.getServiceInstanceName())
           >                 .serviceName(request.getName())
           >                 .planName(request.getPlan())
           >                 .parameters(request.getParameters())
           >                 .completionTimeout(Duration.ofHours(4)) // Orange patch: COAB services may take up to 4 hours
           >                 .build();
           >   
           >         Mono<CreateServiceInstanceResponse> createServiceInstanceResponseMono =
           >             Mono.just(CreateServiceInstanceResponse.builder()
           >                 .name(request.getServiceInstanceName())
           >                 .build());
           >   
           >         if (request.getProperties().containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
           >             return createSpace(request.getProperties().get(DeploymentProperties.TARGET_PROPERTY_KEY))
           >                 .then(
           >                     operationsUtils.getOperations(request.getProperties())
           >                         .flatMap(cfOperations -> cfOperations.services()
           >                             .createInstance(createServiceInstanceRequest)
           >                             .then(createServiceInstanceResponseMono)));
           >         }
           >         else {
           >             return operations
           >                 .services()
           >                 .createInstance(createServiceInstanceRequest)
           >                 .then(createServiceInstanceResponseMono);
           >         }
           >    }
           >
           >     public class CreateServiceInstanceResponse {
           >     
           >     	private final String name;
           >     }
           > 
           > org.cloudfoundry.operations.services.Services
           >      Mono<Void> createInstance(CreateServiceInstanceRequest request);

        => consider instead fetching the dashboard url from service instance once instanciated ??
           
- Fix &  Test propagation of brokered service binding params to backing service key params
            
            
   - check current state for future regression. Need fast feedback from local build + ability to step into with debuger
      - Run full cycle with concourse
         - commit & push in a branch. 
         - Wait for circle build to complete 
         - Trigger concourse build (manually for through fly cli)
         - Watch output in concourse UI from browser   
      - run test-in-cf.bash or subparts (pushes to CF)
         - Provides additional coverage: plan update & noop exec  
         - !! May conflict with concourse execs (same target org, same broker instance)
            - option 1: make a distinct instance 
                - change target space from osb-cmdb-services to osb-cmdb-services
                - change broker instance, url, registred services
                - !! means duplicated diverging scripts
            - option 2: pause the integration instance when pushing locally
         - !! Currently as unversionned files added to .gitignore (manifest.yml with deployer properties, test-in-cf.bash)
            - option 1: extract secrets from test-in-cf.bash & add it to the repo
            - option 2: user intellij local versionning. https://www.jetbrains.com/help/idea/local-history.html
         - !! lacks assertions and requires visual inspections
             
      - run test-locally.bash
         - suiteable for local debugging of a specific case 


Product risks/opportunities:
- backing service location & future migration
   - quota use-cases
      - experiment space/org quotas & verify SCAB error handling
      - org & space dynamic creation with pluggeable strategy
   - analytics/inventory/billing use cases
      - labels
      - annotations
- dashboard
- opportunity: service sidecars 
   - UIs: phpmyadmin
   - Custom dashboard/alerting ?
- automated vs manual brokered service configuration

Technical risks:
- SCAB fork/patch cost w.r.t. current limitations for osb-cmdb


- run unit tests to check regression
- externalize spring.cloud.appbroker.deployer.cloudfoundry.api-port to manifest.yml as env vars
   - 
- remove application.properties + manifest.yml from repo
- rebase one or all branches to remove without history
- push to github 
   - as a SCAB fork to help creating PRs
      - still possible to unfork through ticketing or import see https://stackoverflow.com/questions/29326767/unfork-a-github-fork-without-deleting/41486339#41486339  
- set up circle ci & publish release jar as github release
- integrate into paas-templates 

- create space with custom strategy: see org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance: probably need to combine the TargetService SpacePerServiceInstance with ServiceInstanceGuidSuffix as a single target is currently supported in BackingService


- SCAB benefits for osb-cmdb:
  - Flexible model where different mapping can be changed by only performing configuration
  - Some use-cases already implemented :
     - sync BackingService create/update/delete (incl. service instance sharing)
  - State of the art framework/practices for leveraging reactive usage in cf-java-client & spring-cloud-open-service-broker
     - Faster to get started
     - Reduced maintenance on:
        - spring family bumps
        - CC API V3 ? https://github.com/spring-cloud/spring-cloud-app-broker/issues/243
    - Dynamic app provisionning may support additional use-cases such as:
     - Mysql php-myadmin side-car
     - Grafana / prometheus side-car
     - Kubernetes future support 
        - mentionned in [announcement blog](https://spring.io/blog/2019/05/30/introducing-spring-cloud-app-broker) comments
        - tracked in https://github.com/spring-cloud/spring-cloud-app-broker/issues?q=is%3Aissue+is%3Aopen+label%3Akubernetes
        - Q: KubeCtl client ?
        - Q: helm chart client ?

- SCAB Limitations/drawbacks for osb-cmdb
   - Static catalog announcement
   - OSB conformance
      - GetSI not implemented
   - CSI/USI does not yet return dashboard url 
      - Need to subclass workflow impl buildResponse() to set dashboard URL.
   - Partial Async backing service support: hangs if SCAB is restarted during async backing service provisonning  
      - OSB get last operation returns persisted service state, fed from current flux. 
      - Flux is not restarted to update service state if SCAB is restarted
   - Target isn't supporting dynamically created Space yet shared Space among multiple SI. 
      - Impl Hardcoded into Deployer looking up properties (even though externally visbilite strategy)
   - Deployer service does not yet support service key: would significant PR be accepted ?
   - complexity of the reactive architecture & associated tests 
      - architecture is'nt easy to understand and not documented
         - rationale for multiple layers: 
            - SCOSB model: CSIReq, CISIResp     
            - CJC model: CSReq, CSResp     
            - deployer + deployer-cloudfoundry model: BackingService
            - ProvisionService model: BackingServices
            - Workflow: 
               - accept(CSIReq): Mono<Boolean> //sync 
               - create(CSIReq, CSIResp): Mono<Void> //async ?   
               - buildResponse(CISReq, CSIRespBuilder): Mono<CSIRespBuilder> //Synchronously to provide OSB resp fast
      - lots of boilerplate/POJO wrapping needed when extending use-cases
      - threading model isn't explicit
         - need to be careful in workflows impl ?
   - Maintenance risks on SCAB ? Which commercial offering is backed by SCAB ?      
      - SCAB background & ecosystem
          - https://docs.pivotal.io/spring-cloud-services/3-0/common/index.html 
             - Only ConfigServer currently shipped
             - Service Registry and Circuit Breaker dashboard deprecated
                - https://docs.pivotal.io/spring-cloud-services/3-0/release-notes.html
                 
                > Spring Cloud Services 3.0.0 does not include Service Registry or Circuit Breaker Dashboard services. To use these services, you can install Spring Cloud Services 2.0.x alongside 3.0.0.
          - Sign on https://docs.pivotal.io/p-identity/1-10/manage-service-instances.html 



```
org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService:

	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return invokeDeleteResponseBuilders(request)
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> delete(request, response)
				.subscribe());
	}

	private Mono<DeleteServiceInstanceResponse> invokeDeleteResponseBuilders(DeleteServiceInstanceRequest request) {
		AtomicReference<DeleteServiceInstanceResponseBuilder> responseBuilder =
			new AtomicReference<>(DeleteServiceInstanceResponse.builder());

		return Flux.fromIterable(deleteServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(DeleteServiceInstanceResponseBuilder::build);
	}
```




          

 
- PR SCAB CJC completionTimeout fix
   - should this be configureable with a property ?
   - should bump the backoff strategy in CJC polling to only poll every 30 s (CC default polling) instead of 15 s ?
   - reproduce with a unit test ?? 

- PR SCAB log refinement: org.springframework.cloud.appbroker.deployer.DeployerClient.createServiceInstance() matching https://github.com/spring-cloud/spring-cloud-app-broker/issues/228
   - Need to rebase/update followin https://github.com/spring-cloud/spring-cloud-app-broker/pull/277/files

```   
2019-09-27 12:30:55.397 ERROR 26038 --- [ry-client-nio-1] o.s.c.appbroker.deployer.DeployerClient  : Error creating backing service noop-ondemand with error 'null'
2019-09-27 12:30:55.398 DEBUG 26038 --- [ry-client-nio-1] cloudfoundry-client.operations           : FINISH Create Service Instance (cancel/5.2 m)
2019-09-27 12:30:55.398 ERROR 26038 --- [ry-client-nio-1] .s.c.a.d.BackingServicesProvisionService : Error creating backing services [BackingService{serviceInstanceName='noop-ondemand-noop-guid-3', name='noop-ondemand', plan='default', parameters={}, properties={}, parametersTransformers=[]}] with error 'null'
2019-09-27 12:30:55.399 ERROR 26038 --- [ry-client-nio-1] pDeploymentCreateServiceInstanceWorkflow : Error creating backing services for noop-ondemand-cmdb/default with error 'null'
```   

```   
			.doOnError(exception -> log.error("Error creating backing service {} with error '{}'",
				backingService.getName(), exception.getMessage()))

becomes 

			.doOnError(exception -> log.error("Error creating backing service {} with error '{}'",
				backingService.getName(), exception))
```           

becomes
```
2019-09-30 09:14:42.364 ERROR 28924 --- [ry-client-nio-2] d.DefaultBackingServicesProvisionService : Error creating backing services [BackingService{serviceInstanceName='noop-ondemand-noop-guid-7', name='noop-ondemand', plan='default', parameters={}, properties={}, parametersTransformers=[], rebindOnUpdate=false}] with error 'org.cloudfoundry.util.DelayTimeoutException'
2019-09-30 09:14:42.365 ERROR 28924 --- [ry-client-nio-2] pDeploymentCreateServiceInstanceWorkflow : Error creating backing services for noop-ondemand-cmdb/default with error 'null'
2019-09-30 09:14:42.367 ERROR 28924 --- [ry-client-nio-2] o.s.c.a.s.WorkflowServiceInstanceService : Error creating service instance ServiceBrokerRequest{platformInstanceId='null', apiInfoLocation='redacted.api/v2/info', originatingIdentity=null}AsyncServiceBrokerRequest{asyncAccepted=true}AsyncParameterizedServiceInstanceRequest{parameters={}, context=Context{platform='cloudfoundry', properties={spaceGuid=568e6244-471b-466c-a372-3ab2109b69aa, instance_name=test-mysql-cmdb-2, space_name=osb-cmdb-broker, organizationGuid=87505508-4c09-499e-933c-eb89582cb9a5, organization_name=system_domain}}}CreateServiceInstanceRequest{serviceDefinitionId='noop-ondemand-cmdb-guid', planId='noop-ondemand-cmdb-default-plan-guid', organizationGuid='87505508-4c09-499e-933c-eb89582cb9a5', spaceGuid='568e6244-471b-466c-a372-3ab2109b69aa', serviceInstanceId='noop-guid-7'} with error 'org.cloudfoundry.util.DelayTimeoutException'
``` 
   - Reproduce with a test
   - Make the fix
   - Push the branch and PR
   - Merge the fix in and orange fork
      - pending_upstream_merges
   - Update OsbCmdb to reference the fork using jitpack maven repo


- add assertions to smoke tests 
   - using bats https://github.com/bats-core/bats-core/blob/master/test/bats.batsd
      - learn bats core & its limitation
          - select only some test cases to execute 
      - investigate additional bats libs for better assert diagnostic
      - workaround intellij syntax errors



- Investigate existing Java acceptance tests configured at org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryProperties see spring-cloud-app-broker-acceptance-tests/README.adoc
   - run from gradle build test

```   
   guillaume@guillaume-box:~/public-code/spring-cloud-app-broker/spring-cloud-app-broker-acceptance-tests$ (master) jar tvf src/main/resources/backing-app.jar 
        [...]
     0 Thu Jun 21 16:22:00 CEST 2018 BOOT-INF/classes/
     0 Thu Jun 21 16:21:58 CEST 2018 BOOT-INF/classes/com/
     0 Thu Jun 21 16:21:58 CEST 2018 BOOT-INF/classes/com/example/
     0 Thu Jun 21 16:21:58 CEST 2018 BOOT-INF/classes/com/example/demo/
   733 Thu Jun 21 16:21:58 CEST 2018 BOOT-INF/classes/com/example/demo/DemoApplication.class
```

- Investigate use of the management controller org.springframework.cloud.appbroker.acceptance.ManagementController to extend for 
   - delete all backing services (e.g. for tests or change of strategy) 
   - update all backing services (e.g. stemcell update in coab backing services)   

```
  2019-09-25T20:56:24.73+0200 [APP/PROC/WEB/0] OUT 2019-09-25 18:56:24.733 ERROR 14 --- [ry-client-nio-6] o.s.c.appbroker.deployer.DeployerClient  : Error creating backing service noop-ondemand with error 'null'
   2019-09-25T20:56:24.73+0200 [APP/PROC/WEB/0] OUT 2019-09-25 18:56:24.734 DEBUG 14 --- [ry-client-nio-6] .s.c.a.d.BackingServicesProvisionService : Finished creating backing service onError(org.cloudfoundry.util.DelayTimeoutException)
   2019-09-25T20:56:24.73+0200 [APP/PROC/WEB/0] OUT 2019-09-25 18:56:24.734 ERROR 14 --- [ry-client-nio-6] .s.c.a.d.BackingServicesProvisionService : Error creating backing services [BackingService{serviceInstanceName='noop-ondemand-d1ac7254-e37e-4963-af54-1c8ece717094', name='noop-ondemand', plan='default', parameters={}, properties={}, parametersTransformers=[]}] with error 'null'
   2019-09-25T20:56:24.73+0200 [APP/PROC/WEB/0] OUT 2019-09-25 18:56:24.734 ERROR 14 --- [ry-client-nio-6] pDeploymentCreateServiceInstanceWorkflow : Error creating backing services for noop-ondemand-cmdb/default with error 'null'
```

- also copy & adapt unit tests associated to copied classes to speed up tests 

- document debugging reactor

https://www.jetbrains.com/help/idea/debug-asynchronous-code.html
https://projectreactor.io/docs/core/release/reference/#debugging


- learning reactive

https://projectreactor.io/learn
https://projectreactor.io/docs/core/release/reference/#_from_imperative_to_reactive_programming
https://projectreactor.io/docs/core/release/reference/#which-operator
 
