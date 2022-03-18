
* check how x-api-info-location is currently retrieved
   * in CreateServiceRequest.apiInfoLocation 
* check whether servlet filter or spring mvc equivalent can be applied on all requests
   * https://www.baeldung.com/spring-webflux-filters#2-handlerfilterfunction
      * q: how to restrict to only osb requests and not reject actuator requests ?
        * by inspecting request path
   * unit test 
     * https://github.com/eugenp/tutorials/blob/master/spring-5-reactive/src/test/java/com/baeldung/reactive/filters/PlayerHandlerIntegrationTest.java
   * currently using springmvc, so reactive webfilter gets ignored
   * Pb: still needs to comply to osb api spec and return error message as json
     * basic filter tests return 404 but without clear message 
     * **refined filter to write plain json output**
* explicit code in current code base
   * ServiceInstanceController:  
   * CatalogController: hard to intercept. This is where fast feedback would be required when operators register broker
=> sticked to ServletFilter

How to test ?
- [x] locally start the cfapp + make manual OSB requests
   - [ ] create new Integration test with variables
   - [x] craft OSB requests manually: v2/catalog
- use acceptance tests E2E framework
   - [x] adapt CreateDeleteInstanceWithBackingServiceKeysAcceptanceTest to check current x-api-info-location: checks non-regression
     - update locally used password (client-id has not changed)  
   - [x] add assertions verifying invalid x-api-info-location gets rejected: BrokerRegistrationRestrictedToXApiInfoLocationAcceptanceTest which checks broker registration fails (v2/catalog)

- [x] debug why the filter does not seem to load when pushed as a cf app
  - bean is indeed loaded
  - logger level is debug (configured to trace)
  - traces don't display
  - [x] gradle clean + build
  - **environment variables not taken into account: typo/camel case**

```
osbcmdb.expectedXApiInfoLocationHeader: invalid_value_set_to_fail_catalog_fetching
osbcmdb.rejectRequestsWithNonMatchingXApiInfoLocationHeader: true
```

## Draft User documentation

With invalid cf-api-info-location fails service broker registration would fail with 

```
Creating service broker test-broker as gberche...
Job (ac1cd040-0bef-45dd-8e38-d68c6d821eb2) failed: The service broker rejected the request. Status Code: 400 Bad Request. Please check that the URL points to a valid service broker.
FAILED
```

The message isn't displayed by CF since this is an async job. 

Checking the cf job with `cf curl v3/jobs/<jobid>` does not provide more details:

```json
"errors": [
    {
    "detail": "The service broker rejected the request. Status Code: 400 Bad Request. Please check that the URL points to a valid service broker.",
    "title": "CF-ServiceBrokerRequestRejected",
    "code": 270020
    }
],
``` 

It might be then necessary