
- create space with custom strategy: see org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance: probably need to combine the TargetService SpacePerServiceInstance with ServiceInstanceGuidSuffix as a single target is currently supported in BackingService
        - Pb: Impl Hardcoded into `CloudFoundryAppDeployer.createServiceInstance()`  looking up DeploymentProperties.TARGET_PROPERTY_KEY (target) property to create a space display a warn log if space already exists
            - Option 1: avoid setting target property when space exists
               - Can't lookup in SpacePerServiceInstance strategy whether the space exists
            - Option 2: ignore the warn trace with current message saying space name is already taken
            - Option 3: modify the warn trace to hint this may be normal
            - Option 4: make CloudFoundryAppDeployer idempotent and not issue a warn trace if space exists
         - Pb: Target interface is only providing service name and instance id, how to lookup service name, service plan name ? 
             > public interface Target {
             >  	ArtifactDetails apply(Map<String, String> properties, String name, String serviceInstanceId);
             >  }
           - Option 1: Refactor Target interface to inject serviceName & servicePlanName
              - Implies propagating up to org.springframework.cloud.appbroker.extensions.targets.TargetService.addToBackingServices()  
                - Implies propagating to SpacePerServiceInstance and ServiceInstanceGuidSuffix classes  
           - Option 2: Have the strategy lookup service name, plan using CF API + create the space
         - Pb: Space is provisionned in defaultOrg. We'd eventually like to have it dynamically selected from client credentials in a future multi-tenant setting
            - Option 1 : don't support this yet   
            - Option 2 : Add new properties into BackingService
         - Pb: with this naming a plan update would translate into a service instances be moved across spaces   
            - Option 1 : don't support plan updates when a quota is set on a service plan (i.e. when target is set to SpacePerServicePlan)
               - Need to check proper error message is returned to user
            - Option 2 : share service instances across spaces to create service keys. 
               - Pb: this still defeats the quota per service plans
            - Option 3 : provide option to configure quota per service definition, i.e. SpacePerServiceDefinition
               - Q: how to handle service definition renamings ?  
                  - A: expect operators to rename the space  
         - Pb: `CloudFoundryAppDeployer.deleteServiceInstance()`  looks up DeploymentProperties.TARGET_PROPERTY_KEY (target) property to **delete** the space. This is an issue when the space should be shared among multiple service instances: cf delete-service fails with 
            > Error deleting service instance p-mysql-5f037acf-eaa1-4ec8-9b34-9827f21a3ad9 with error 'CF-AssociationNotEmpty(10006): 
            >   Please delete the service_bindings, service_keys, and routes associations for your service_instances.'
            > org.cloudfoundry.client.v2.ClientV2Exception: CF-AssociationNotEmpty(10006): Please delete the service_bindings, service_keys, and routes associations for your service_instances.
            - Option 1: use different properties for target space creation and target space deletion 
            - Option 2: add a property to skip target space deletion 
            - Option 3:  
            
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
 
