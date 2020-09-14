
# Base case: CSI CF-AsyncServiceInstanceOperationInProgress(60016)

CreateDeleteAsyncInstanceWithBackingServiceKeysAcceptanceTest

```
deployAppsAndCreateServiceKeysOnBindService()

org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerRequestRejected(10001): The service broker rejected the request. Status Code: 404 Not Found, Body:
	at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$0(ErrorPayloadMappers.java:49)


```

```
2020-09-11 09:24:39.349  INFO 6 --- [nio-8080-exec-4] c.o.o.o.s.OsbCmdbServiceInstance         : Unable to provision service, caught:org.cloudfoundry.client.v3.ClientV3Exception: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance 1bd2ec38-2afb-44e1-8803-73d62314dd81 is in progress., messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354612912}
org.cloudfoundry.client.v3.ClientV3Exception: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance 1bd2ec38-2afb-44e1-8803-73d62314dd81 is in progress., messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354642986}
	at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$2(ErrorPayloadMappers.java:57) ~[cloudfoundry-client-reactor-4.8.0.RELEASE.jar:na], messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354651601}
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: , messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354658043}
Assembly trace from producer [reactor.core.publisher.MonoFlatMap] :, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354663262}
	reactor.core.publisher.Mono.flatMap(Mono.java:2734), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354668575}
	org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$mapToError$12(ErrorPayloadMappers.java:110), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354676127}
Error has been observed at the following site(s):, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354680801}
	|_       Mono.flatMap ⇢ at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$mapToError$12(ErrorPayloadMappers.java:110), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354687291}
	|_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$clientV3$3(ErrorPayloadMappers.java:55), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354692930}
	|_     Flux.transform ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.processResponse(Operator.java:245), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354698205}
	|_     Flux.transform ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:180), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354702694}
	|_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:181), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354707119}
	|_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:191), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354712532}
	|_ Flux.singleOrEmpty ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToMono(Operator.java:195), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354717669}
	|_       Mono.flatMap ⇢ at org.cloudfoundry.reactor.client.v3.AbstractClientV3Operations.patch(AbstractClientV3Operations.java:95), messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354722298}
Stack trace:, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354727110}
		at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$2(ErrorPayloadMappers.java:57) ~[cloudfoundry-client-reactor-4.8.0.RELEASE.jar:na], messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1599816279354732104}
		at org.cloudfoundry.
```

* [ ] Reproduce in debugger
```
   2020-09-11T12:44:12.59+0200 [APP/PROC/WEB/0] OUT 2020-09-11 10:44:12.596 DEBUG 42 --- [-client-epoll-7] cloudfoundry-client.response             : 202    /v2/service_instances (297 ms)
   2020-09-11T12:44:12.62+0200 [APP/PROC/WEB/0] OUT 2020-09-11 10:44:12.622 DEBUG 42 --- [nio-8080-exec-8] c.o.o.o.s.OsbCmdbServiceInstance         : Assigning metadata to service instance with id=70e39f6
2-eb6e-461f-aa0b-cebac0552ac6 annotations={brokered_service_context_spaceName=development, brokered_service_context_organizationName=osb-cmdb-services-acceptance-tests, brokered_service_api_info_location=
api.redacted-domain.org/v2/info, brokered_service_context_instanceName=si-create-async-instance-with-service-keys} and labels={brokered_service_instance_guid=f0a8f886-76a2-49d8-80eb-5f7230e5f8fe,
 backing_service_instance_guid=70e39f62-eb6e-461f-aa0b-cebac0552ac6, brokered_service_context_organization_guid=14af188e-b07f-4041-9488-d97bacfcb49c, brokered_service_originating_identity_user_id=321ae0c8
-1289-4e49-9aa4-4fca806754f1, brokered_service_context_space_guid=ff120500-0550-4ce5-abb6-7ad52243c70c}
   2020-09-11T12:44:12.64+0200 [APP/PROC/WEB/0] OUT 2020-09-11 10:44:12.649 DEBUG 42 --- [-client-epoll-8] cloudfoundry-client.request              : PATCH  /v3/service_instances/70e39f62-eb6e-461f-aa0b-c
ebac0552ac6
   2020-09-11T12:44:12.68+0200 [APP/PROC/WEB/0] OUT 2020-09-11 10:44:12.681 DEBUG 42 --- [-client-epoll-8] cloudfoundry-client.response             : 409    /v3/service_instances/70e39f62-eb6e-461f-aa0b-c
ebac0552ac6 (31 ms)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT 2020-09-11 10:44:12.703  INFO 42 --- [nio-8080-exec-8] c.o.o.o.s.OsbCmdbServiceInstance         : Unable to provision service, caught:org.cloudfoundry.c
lient.v3.ClientV3Exception: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance f0a8f886-76a2-49d8-80eb-5f7230e5f8fe is in progress.
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT org.cloudfoundry.client.v3.ClientV3Exception: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance f0a8f886-76a2-49d8-80
eb-5f7230e5f8fe is in progress.
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$2(ErrorPayloadMappers.java:57) ~[cloudfoundry-client-reactor-4.8.0.RELEASE.jar:na]
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT Assembly trace from producer [reactor.core.publisher.MonoFlatMap] :
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     reactor.core.publisher.Mono.flatMap(Mono.java:2734)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$mapToError$12(ErrorPayloadMappers.java:110)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT Error has been observed at the following site(s):
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_       Mono.flatMap ⇢ at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$mapToError$12(ErrorPayloadMappers.java:110)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$clientV3$3(ErrorPayloadMappers.java:55)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_     Flux.transform ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.processResponse(Operator.java:245)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_     Flux.transform ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:180)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:181)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_       Flux.flatMap ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToFlux(Operator.java:191)
   2020-09-11T12:44:12.71+0200 [APP/PROC/WEB/0] OUT     |_ Flux.singleOrEmpty ⇢ at org.cloudfoundry.reactor.util.Operator$ResponseReceiver.parseBodyToMono(Operator.java:195)
```
* suspecting change in CF behavior using cf-deployment bump
   * [ ] check capi / cf-deployment changes since last success build
      * last successful cmdb acceptance test build: July 28th 10am
      * master-depls/master-depls-versions.yml capi-version: "1.92.0" since june 6th
      * deployment-manifest cf.yml
        ```
            - name: capi
              version: 1.97.0
        ```
      * bosh
      ```
     $bosh releases
     [...]
        capi                                	1.97.0*              	3a7ba6d5

     (*) Currently deployed
     (+) Uncommitted changes

     $ bosh deployment
     Using environment '192.168.116.158' as user 'gberche'

     Name  Release(s)                     Stemcell(s)                                       Config(s)             Team(s)
     cf    backup-and-restore-sdk/1.17.4  bosh-openstack-kvm-ubuntu-xenial-go_agent/621.81  3801 cloud/default    -
[...] capi/1.97.0
     ```
      * cf-deployment Date: Tue Aug 25 15:00:51 2020 +0200 in branch feature-bump-for-v48 (#213)
   * [ ] check capi logs
   * [ ] check capi issues
   * [ ] check capi release notes
      * https://github.com/cloudfoundry/capi-release/releases/tag/1.93.0
         v3: Create managed service instance using ASYNC broker details
         v3: Create managed service instance using SYNC broker details
   * [ ] check capi api doc http://v3-apidocs.cloudfoundry.org/version/3.88.0/index.html#update-a-service-instance: nothing about concurrency.

CF_TRACE=true cf curl -X PATH /v3/service_instances/39308492-68d3-4601-adb6-8f76f37392f5 -d '{
    "metadata": {
      "annotations": {
        "note": "detailed information"
      },
      "labels": {
        "key": "value"
      }
    }
  }'

   * [x] report issue upstream https://github.com/cloudfoundry/capi-release/issues/183
   * workaround options:
      * 1) add meta-data update in GetLastOperation once the service status is'nt in progress
         * can we extract metadata from GetLastOperation ?
            * Plain request is missing CSIReq context, params ...
            * Would require to store the Original CSIReq in the operation field,
               * or in another persistent store (since osb-cmdb is stateless can't store it in memory)
                  * space meta-data: risk of race conditions
                  * a database
         * during async service provisioning, the GetServiceInstance and concurrency prevention in CSI will still be broken as they rely on metadata that would be missing
            * might break acceptance tests
         * [X] CSI/USI: add metadata to returned state
            * update parsed returned state
         * GetLastOperation
            * [X] extract metadata from returned state
            * [x] update metadata


      * 1b) transiently move meta-data update in GetLastOperation once the service status is'nt in progress
      * 2) ~~wait for capi fix~~ => CAPI confirmed they won't fix this https://github.com/cloudfoundry/cloud_controller_ng/issues/1781
         * implies no metadata for async services
         * also breaks GetServiceInstance, and concurrency prevention in CSI which rely on metadata that would be missing
      * 3) Change CSI CAPI call to /v3 and pass the metadata in the provisionning call
         * API is still under the experimental section of V3 API  http://v3-apidocs.cloudfoundry.org/version/3.88.0/index.html#create-a-service-instance
            * CSI
         ```
            Example Request for Managed Service Instance

            curl "https://api.example.org/v3/service_instances" \
              -X POST \
              -H "Authorization: bearer [token]" \
              -H "Content-type: application/json" \
              -d '{
                "type": "managed",
                "name": "my_service_instance",
                "parameters": {
                  "foo": "bar",
                  "baz": "qux"
                },
                "tags": ["foo", "bar", "baz"],
                "metadata": {
                  "annotations": {
                    "foo": "bar"
                  },
                  "labels": {
                    "baz": "qux"
                  }
                },
                "relationships": {
                  "space": {
                    "data": {
                      "guid": "7304bc3c-7010-11ea-8840-48bf6bec2d78"
                    }
                  },
                  "service_plan": {
                    "data": {
                      "guid": "e0e4417c-74ee-11ea-a604-48bf6bec2d78"
                    }
                  }
                }
              }'

            Example Response for Managed Service Instance

            HTTP/1.1 202 Accepted
            Content-Type: application/json
            Location: https://api.example.org/v3/jobs/af5c57f6-8769-41fa-a499-2c84ed896788
         ```
            * Get job status
         ```
        curl "https://api.example.org/v3/jobs/[guid]" \
          -X GET \
          -H "Authorization: bearer [token]"

        Completed Job Example Response

        HTTP/1.1 200 OK
        Content-Type: application/json

        {
          "guid": "b19ae525-cbd3-4155-b156-dc0c2a431b4c",
          "created_at": "2016-10-19T20:25:04Z",
          "updated_at": "2016-11-08T16:41:26Z",
          "operation": "app.delete",
          "state": "COMPLETE",
          "links": {
            "self": {
              "href": "https://api.example.org/v3/jobs/b19ae525-cbd3-4155-b156-dc0c2a431b4c"
            }
          },
          "errors": [],
          "warnings": []
        }
         ```
           * Async/sync changes http://v3-apidocs.cloudfoundry.org/version/3.88.0/index.html#asynchronous-operations
         ```
            Unlike V2, clients cannot opt-in for asynchronous responses from endpoints. Instead, end
            points that require asynchronous processing will return 202 Accepted with a Location header pointing to the job resource to poll. Endpoints that do not require asynchronous processing will respond synchronously.
         ```
         * Risk of being a beta tester until CF7 uses v3 api to create-service-instance
            * [x] add shortcut to switch between CF7 and CF6 without the heavy documented apt-get support. https://github.com/cloudfoundry/cli/issues/1974#issuecomment-691204698
            * [x] verified `cf version 7.1.0+4c3168f9a.2020-09-09` does not use v3
         * Cf-java-client is lacking support for V3 CSI.
            * This would mean contributing support for it
            * new v3 API style need significant time and effort to get 1st class support into cf-java-client
               * Async Jobs
            * No yet related issues
            * No yet related work in the pivotal tracker backlog https://www.pivotaltracker.com/n/projects/816799
         * Although CAPI intends to support a mix of V2 and V3, I'm observing 500 errors at times from CAPI


#  async operation in progress on USI

ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest

```31-08-2020 08:16:36.163 ?[35m[cloudfoundry-client-epoll-2]?[0;39m ?[1;31mERROR?[0;39m o.s.c.a.a.f.cf.CloudFoundryService.lambda$updateServiceInstance$57 - Error updating service instance si-concurrent-staled-update-instance: org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerBadResponse(10001): Service broker error: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance cd88dc8f-d60e-4bc2-b8d3-c597e3a49ab7 is in progress.
31-08-2020 08:16:36.164 ?[35m[cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.operations.lambda$log$2 - FINISH Update Service Instance (onError/2.3 s)
```

```
brokeredServiceUpdates()

org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerBadResponse(10001): Service broker error: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance cd88dc8f-d60e-4bc2-b8d3-c597e3a49ab7 is in progress.
	at org.cloudfoundry.reactor.util.ErrorPayloadMappers.lambda$null$0(ErrorPayloadMappers.java:49)
```

```

Test worker]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.operations.lambda$log$1 - START  Update Service Instance
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.request.request - GET    /v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c/service_instances?q=name%3Asi-concurrent-staled-update-instance&page=1&return_user_provided_service_instances=true
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.response.response - 200    /v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c/service_instances?q=name%3Asi-concurrent-staled-update-instance&page=1&return_user_provided_service_instances=true (33 ms)
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.request.request - GET    /v2/service_plans/9e14453e-ba64-4f3f-ad01-127f767c9dd4
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.response.response - 200    /v2/service_plans/9e14453e-ba64-4f3f-ad01-127f767c9dd4 (147 ms)
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.request.request - GET    /v2/services/95fb2364-17d4-403d-bc79-86d846c3b291
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.response.response - 200    /v2/services/95fb2364-17d4-403d-bc79-86d846c3b291 (14 ms)
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.request.request - GET    /v2/service_plans?q=service_guid%3A95fb2364-17d4-403d-bc79-86d846c3b291&page=1
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.response.response - 200    /v2/service_plans?q=service_guid%3A95fb2364-17d4-403d-bc79-86d846c3b291&page=1 (21 ms)
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.request.request - PUT    /v2/service_instances/cd88dc8f-d60e-4bc2-b8d3-c597e3a49ab7?accepts_incomplete=true
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.response.response - 502    /v2/service_instances/cd88dc8f-d60e-4bc2-b8d3-c597e3a49ab7?accepts_incomplete=true (2.0 s)
cloudfoundry-client-epoll-2]?[0;39m ?[1;31mERROR?[0;39m o.s.c.a.a.f.cf.CloudFoundryService.lambda$updateServiceInstance$57 - Error updating service instance si-concurrent-staled-update-instance: org.cloudfoundry.client.v2.ClientV2Exception: CF-ServiceBrokerBadResponse(10001): Service broker error: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance cd88dc8f-d60e-4bc2-b8d3-c597e3a49ab7 is in progress.
cloudfoundry-client-epoll-2]?[0;39m ?[39mDEBUG?[0;39m cloudfoundry-client.operations.lambda$log$2 - FINISH Update Service Instance (onError/2.3 s)
Test worker]?[0;39m ?[34mINFO ?[0;39m o.s.c.a.a.CloudFoundryAcceptanceTest.purgeServiceInstance - Purging service instance with name si-concurrent-staled-update-instance
```


# K8S concurrency:  CSI 202 instead of 409

ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest
```
		//When requesting a concurrent request to the same broker with the different service definition
		// then get a 409
		given(brokerFixture.serviceInstanceRequest(mismatchingServiceId, BACKING_SERVICE_PLAN_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.CONFLICT.value());
```

```
java.lang.AssertionError: 1 expectation failed.
Expected status code <409> but was <202>.

	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
	at org.codehaus.groovy.reflection.CachedConstructor.invoke(CachedConstructor.java:80)
	at org.codehaus.groovy.reflection.CachedConstructor.doConstructorInvoke(CachedConstructor.java:74)
	at org.codehaus.groovy.runtime.callsite.ConstructorSite$ConstructorSiteNoUnwrap.callConstructor(ConstructorSite.java:84)
	at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCallConstructor(CallSiteArray.java:59)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callConstructor(AbstractCallSite.java:237)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callConstructor(AbstractCallSite.java:249)
	at io.restassured.internal.ResponseSpecificationImpl$HamcrestAssertionClosure.validate(ResponseSpecificationImpl.groovy:494)
	at io.restassured.internal.ResponseSpecificationImpl$HamcrestAssertionClosure$validate$1.call(Unknown Source)
	at io.restassured.internal.ResponseSpecificationImpl.validateResponseIfRequired(ResponseSpecificationImpl.groovy:656)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.codehaus.groovy.runtime.callsite.PlainObjectMetaMethodSite.doInvoke(PlainObjectMetaMethodSite.java:43)
	at org.codehaus.groovy.runtime.callsite.PogoMetaMethodSite$PogoCachedMethodSiteNoUnwrapNoCoerce.invoke(PogoMetaMethodSite.java:190)
	at org.codehaus.groovy.runtime.callsite.PogoMetaMethodSite.callCurrent(PogoMetaMethodSite.java:58)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callCurrent(AbstractCallSite.java:168)
	at io.restassured.internal.ResponseSpecificationImpl.statusCode(ResponseSpecificationImpl.groovy:125)
	at io.restassured.specification.ResponseSpecification$statusCode$0.callCurrent(Unknown Source)
	at io.restassured.internal.ResponseSpecificationImpl.statusCode(ResponseSpecificationImpl.groovy:133)
	at io.restassured.internal.ValidatableResponseOptionsImpl.statusCode(ValidatableResponseOptionsImpl.java:119)
	at org.springframework.cloud.appbroker.acceptance.ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest.deployAppsAndCreateServiceKeysOnBindService(ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest.java:101)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
```

* [x] report issue upstream https://github.com/cloudfoundry/capi-release/issues/183

```
   2020-09-11T12:59:44.58+0000 [APP/PROC/WEB/1] OUT 2020-09-11 12:59:44.584  INFO 11 --- [nio-8080-exec-8] c.o.o.o.s.OsbCmdbServiceInstance         : Inspecting exception caught org.cloudfoundry.client.v3
.ClientV3Exception: CF-AsyncServiceInstanceOperationInProgress(60016): An operation for service instance 6df55e7f-b909-4f37-8581-12ed4d750608 is in progress. for possible concurrent dupl while handling re
quest ServiceBrokerRequest{platformInstanceId='null', apiInfoLocation='api.redacted-domain.org/v2/info', originatingIdentity=Context{platform='cloudfoundry', properties={user_id=0d02117b-aa21-43e
2-b35e-8ad6f8223519}}', requestIdentity=653031cf-99b2-44c7-a86d-04bcd5f1ef5e}AsyncServiceBrokerRequest{asyncAccepted=true}AsyncParameterizedServiceInstanceRequest{parameters={}, context=Context{platform='
cloudfoundry', properties={spaceGuid=1a603476-a3a1-4c32-8021-d2a7b9b7c6b4, spaceName=smoke-tests, organizationName=osb-cmdb-brokered-services-org-client-0, instanceName=osb-cmdb-broker-0-smoketest-1599829
144, organizationGuid=c2169b61-9360-4d67-968c-575f3a10edf5}}}CreateServiceInstanceRequest{serviceDefinitionId='b0300e6e-8f93-4309-bdee-01099f644b97', planId='477aef10-2433-4c5f-8a7a-46489f04e2fa', organiz
ationGuid='c2169b61-9360-4d67-968c-575f3a10edf5', spaceGuid='1a603476-a3a1-4c32-8021-d2a7b9b7c6b4', serviceInstanceId='6df55e7f-b909-4f37-8581-12ed4d750608', maintenanceInfo='MaintenanceInfo{version='1.1.
0, description='osb-cmdb now propagates dashboard url (instant upgrade, no downtime)}'}

[...]

   2020-09-11T12:59:44.82+0000 [APP/PROC/WEB/1] OUT 2020-09-11 12:59:44.828  INFO 11 --- [nio-8080-exec-8] c.o.o.o.s.OsbCmdbServiceInstance         : Concurrent request is not incompatible and is still in
 progress success: 202
```



