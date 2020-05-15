
## Rationale for an impl independent of scab

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
   - coab custom params support: 
      - opt-in through metadata filtering: 
         - broker name matching "coa" prefix. Depends on CF-java-client
      - systematic opt-in but fallback in case of errors    
   - ~~dashboard Url configuration ? => serve it in nested brokers instead ?~~
   - ~~sync vs async processing~~
- K8S collab => independent of cmdb code base

## Design outline

- custom ServiceInstanceService impl
   - blocking non-reactive style
      - simpler to reason than reactive, in particular for
         - conditionals
         - multiple local variables in the stack
         - nested try/catch/finally clauses
      - all api calls are sequential, reduced opportunity to leverage reactive parallelism/flow control/timeouts
   - stateless design: CF is the only state, no more state repository.
      - OSB Operation field contains a Json formatted string:
        - operation type: create|update|delete
        - backingCfServiceInstanceGuid
      - high availability through 2 osb-cmdb app instances, and zero downtime deployments
      - scale-out scalability if needed          
   - Error recovery and K8S dupl concurrent requests
         - Pb: Improperly documented/typed errors in cf-java-client, risks of silently failing upon real errors.
         - Adopted approach: 
            - don't rely on errors returned by cf-java-client and CAPI overall
            - instead upon errors, query back inventory to assume proper response status to return and possible return redacted original CAPI error status 
   - service instance delete
      - deletes nested key to avoid being stricter than osb api
      - does not ask to purge the service instance, as this would create leaks in backing service instance broker.
   - error handling: does not try to hide/recover from backing service errors
      - on failed backing delete
         - CF will perform retries to delete backing service
         - Eventually backing instance will be removed either
            - manually by an operator 
            - upon CF max retry
         - brokered delete will return failure
            - CF will perform retries to delete brokered service
            - Brokered service delete should avoid asking deletion again ? 
               - to avoid interruptin CF orphan deletion
               - to avoid error traces polution, 
            - Instead, cmdb just polls the backing service status
   - use of CF java client:
      - try to favor CfOperation (high level api) in tests, in hope it would be more stable with CAPI V3 than low-level CFClient
      - in production code, use low-level CFClient to avoid CfOperation performing undesired polling

- custom ServiceInstanceBindingService impl


## Target packaging

* single module
   * src/main/
      * java
         * cmdb
            * config: autoconfig, security config ??
            * serviceinstance
            * servicebinding
      * resources: application.yml: default config: actuactor, logs 
   * src/test
      * java
         * component-tests: make OSB API calls, wiremock dependencies, springboot tests
            * **component tests need a specific fork model ??**
            ```
            	// force a new fork for every test to eliminate issues with wiremock state
            	forkEvery = 1
            	maxHeapSize = '1G'
            ```
         * acceptance-tests: make CF API calls, run against real dependencies, **depends on boot jar to be produced**

As of V1.0, acceptance tests are kept in their own SCAB-named module.

Moving this into the cmdb project was delayed for now until SCAB prioritizes spinning off acceptance tests in a 1st class repo
   * In order to ease getting updates in the meantime.

## Test strategy

* Glossary/definitions
   * Acceptance test: 
      * Use real CF to make OSB API calls. 
      * Cmdb uses real CF api to act on backing services
      * assertion use CF API api (often high level api and sometimes low level api). 
         * Easier to refactor when moving to CC API V3 
   * Component  test: 
      * make OSB API calls
      * CF API is served by wiremock
      * Intended for stress client OSB tests outside of what CF API allows e.g.
         * Deleting a service instance without first deleting service bindings
         * Forging last operation state parameter 

- v1.0 mostly uses acceptance test
- Rationales
   - delayed investments in component and unit tests until CAPI V3 is available (in order to avoid costly rework)
   - enables changes in CAPI calls with less rework (e.g. optimizations)
   - missing engineering capacity to invest in component and unit tests for 1.0
- would need refactoring to unit test OsbCmdbServiceInstance and  OsbCmdbServiceBinding and possibly split them into smaller objects
- SCAB acceptance tests fixture are reused
- SCAB component tests fixture are reused, although in v1.0 component tests are skipped 


## dashboard auth & aggregator

cmdb
   * osb endpoint /v2/catalog...
   * Auth service: (route service): spring cloud gateway + config oauth multi tenant
      * req route service-instance-guid-shield/prometheus/quota...
         *  extract service-instance-guid. 
         * oauth vers le sevreur du client
         * API CF V2/service-instance-perm-service-instance-guid
            *  OK/KO
         * gateway proxifie le traffic
   * dashboard aggregator: display-dashboard?url1={0}-shield.{1}&label1=shield&url
   2={0}-prometheus.{1}&label1=prometheus
 
Q: how to manage dependency to route service & gorouter in the long term?

Steps:
* CF4K8S: for CF-deployed aps (e.g. php-my-admin)
   * Native route service support ?
   * Istio
* gorouter depreciation for other workload: bosh releases (such as shield/prometheus)
   * K8S ingress/ Traeffic
      * Custom authZ backed by REST API endpoint
         * Native K8S ingress ?
         * Native Istio ?
         * Extension ?
            * Traeffic middleware ?




