
* [ ] **Set up component test, mocking CF API** to get faster feedback than AT
   * [ ] Initiate CmdbCreateServiceInstanceComponentTest from CreateInstanceWithSpacePerServiceInstanceTargetComponentTest
      * [ ] configure scab-integration-tests to depend on osb-cmdb project
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
* [ ] Refactor osb-cmdb packaging (see [target packaging](redesign-scab-independent.md))
    * [x] study removing dependencies to scab code to make tests faster: stashed in `remove-scab-gradle-dependency`
       * CloudFoundryDeploymentProperties
       * CloudFoundryTargetProperties
       * CreateBackingServicesMetadataTransformationService
       * CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient
    * [ ] Study to Only keep single project (to speed up builds). Find solutions for
       * boot jar dependency for acceptance tests execution
       * multiple classpath for tests ?
    * [ ] move cmdb code into packages
    * [ ] Clean up acceptance test fixture in production code
       * [ ] Conditioned by a spring profile acceptance test
    * [ ] duplicate SCAB AT to not depend on SCAB anymore
       * [ ] copy code into a package
       * [ ] check/handle resources
    * [ ] Migrate from gradle to maven ?

     
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
