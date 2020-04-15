
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
   - support
   - ~~dashboard Url configuration ? => serve it in nested brokers instead ?~~
   - ~~sync vs async processing~~



# Design outline:

- Reuse acceptance tests
   - [ ] handle test broker
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

# Pseudo code implementation

# Implementation steps

* [~] Set up acceptance test
   * [x] Make POC compile
      * [x] Make OsbCmdbServiceInstance compile
      * [x] Make OsbCmdbServiceBinding compile
         * [x] Extract superclass with default space behavior
   * [x] Wire autoconfiguration in OsbCmdbApplication. For now reuse scab autoconfiguration and just override ServiceInstanceService
   * [x] Cherry pick service key acceptance test from service-key-support branch & associated files
      * alternatives
      * [ ] create a `service-key-support-flattened-Aprl-5`
      * using git reset
         * [ ] git reset --keep `service-key-support`: brings in all files from service-key support in working dir
            * from d4da3f73
         * [ ] git reset --soft : reset index
      * using git checkout https://jasonrudolph.com/blog/2009/02/25/git-tip-how-to-merge-specific-files-from-another-branch/
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
             * [ ] Test same set up than prod: brokered service name == backend service name
                * [ ] **Modify ServiceInstanceInterceptor to only accept OSB calls in backing spaces**,
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


    * [ ] Adapt clean up script to also clean up spacePerServiceDefinition
* [ ] Collect wire traces from CF API
* [ ] Set up component test, mocking CF API
* [ ] Collect Cf java client exception
* [ ] Set up unit test, mocking CF java client
   * [ ] Extract collaborator to deal with CloudFoundryOperations mock lifecycle (similar to CloudFoundryOperationsUtils)

* [ ] Run AT in CI
   * create PR
   * [ ] Adapt JVM arg -Dtests.broker-app-path=/home/guillaume/code/osb-cmdb-spike/osb-cmdb/build/libs/osb-cmdb-0.10.0-SNAPSHOT.jar