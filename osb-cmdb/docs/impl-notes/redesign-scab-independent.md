
# Rationale for an impl independent of scab:

- easier to address backlog issues
   - that conflict with scab
      - sync service instance #3
      - dashboard support #4
      - 12 factors: broker restart #7:
         - get last operation would poll service instance status
   - that still need custom code in scab extensions
       - COAB custom params: #15
       - service key deletion in DSI: #28
       - metadata update on USI
- simpler implementation in the long run
   - no additional scab complexity
   - no dependency on 3rd party team to implement features

What features are we deprecating ?
- Space per service plan target:
   - Not adopted in production, no migration path possible
   - Alternative exists with cmdb-native quota per service plan
- Flexibility in naming backing service instance: always OSB service instance guid

What SCAB features would be lost and how to accomodate ?
- app brokering for mysql php-my-admin
   - nest a scab broker for mysql
      - without need for osb-cmdb features: metadata, K8S, service key
      - how to deal with since instance plan update ?
         - possibly with use a UPS to scab
         - contribute fix
         - use custom app deployer
- BackingService spec: what are use-cases that would require a configuration per brokered service ?
   - coab custom params opt-in ?
   - ~~dashboard Url configuration ? => serve it in nested brokers instead ?~~
   - ~~sync vs async processing~~



# Design outline:

- Reuse acceptance tests
   - [x] handle test broker
- Reuse component tests
- custom ServiceInstanceService impl
   - stateless design: CF is the only state, no more state repository.
      - OSB Operation field contains a Json formatted string:
        - operation type: create|update|delete
        - backingCfServiceInstanceGuid
      - Ignore "Entity does not exists" errors due to K8S dupls during delete
         - Pb: Improperly documented/typed errors in cf-java-client, risks of silently failing upon real errors.
      - Ignore "Entity already exists" error due to K8S dupls during create/bind
   -  **stateless + cache for K8S dupls**
      - Maintain state repository for K8S dupls race conditions: activeOperations
         - one synchronized HashMap for SI/SB
            - key: OSB service instance guid (String)
            - values:
               - start date ?
               - VCAP request id ?
         - accept for now K8S dupl mishandled upon osb-cmdb concurrent restart
            - use persistent backend in the future if needed
      - Decorate ServiceInstanceService impl with K8Sdupls Impl
      - Fail fast on errors from CF in main cse

   - can coexist with scab if necessary
	> @ConditionalOnMissingBean(ServiceInstanceService.class)
   - maps 1-to-1 CSIR to CSIR
   - + metadata
   - + create space
   - + K8S dupl request sync
      - **lookup for an existing service instance, compare params**
         - CFOperations.getInstance(GetServiceInstanceRequest): only match by service instance name in the current space
            - returns service name, service plan name, dates. Sufficient to handle K8S dupls (params not required)
                > List<String> applications;
                > String id;
                > String name;
                > @Nullable String plan;
                > @Nullable String service;
                > ServiceInstanceType type;
                > @Nullable String dashboardUrl;
                > @Nullable String description;
                > @Nullable String documentationUrl;
                > @Nullable String lastOperation;
                > @Nullable String message;
                > @Nullable String startedAt;
                > @Nullable String status;
                > @Nullable List<String> tags;
                > @Nullable String updatedAt;

   - unit test with one interface
   - synchronous processing (no threading)
      - pb: would lock OSB clients on async services
      - alternatives:
         1- have two different branches of code for sync and async CSI
            - OSB catalog does not describe whether provisionning is sync or async
            - configured per BackingService
         2- inline cf-java-client DefaultServices.createServiceInstance() to not wait for last operation completion
         3- **configure a very short completion timeout in CF-Java-client and perform ourselves the last operation polling in get_last_operation**

- custom ServiceInstanceBindingService impl
   - can coexist with scab if necessary
   - Q: use CloudFoundryOperations or v2 cloudfoundry client ?
      - pros of CloudFoundryOperations
         - easier to migrate to v3: will be transparent
         - security avoids guid injection: remains in the target org/space


- No scab workflow
- No scab Backing model
- Leverage custom AppDeployer impl ?
   - pros
      - Existing impl
   - cons
      - need to clean up app deployment related code
      - an extra layer without much benefits
      - space mgt still complex

# Target packaging

* single module ?
   * src/main/
      * java
         * cmdb
            * config: autoconfig, security config
            * catalog
            * serviceinstance
            * servicebinding
      * resources: application.yml: default config: actuactor, logs 
   * src/test
      * java
         * component-tests: make OSB API calls, wiremock dependencies, springboot tests
         * acceptance-tests: make CF API calls, run against real dependencies, **depends on boot jar to be produced**


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
          > No serializer found for class com.orange.oss.osbcmdb.OsbCmdbServiceInstance$CmdbOperationState and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
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