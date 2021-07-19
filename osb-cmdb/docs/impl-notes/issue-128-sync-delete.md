See orange-cloudfoundry/paas-templates#1234

## Initial analysis

Root cause:

osb-cmdb/osb-cmdb/src/main/java/com/orange/oss/osbcmdb/serviceinstance/OsbCmdbServiceInstance.java

Lines 809 to 814 in 068759e
```
case "failed":
LOG.info("Backing service failed to delete with {}, flowing up the error to the osb " + "client", deletedSi.getMessage());
//500 error
throw new ServiceBrokerException(deletedSi.getMessage());
```
May need to inspect the original to infer sync error and wrap it such as in

osb-cmdb/osb-cmdb/src/main/java/com/orange/oss/osbcmdb/serviceinstance/OsbCmdbServiceInstance.java

Line 770 in 068759e
```
throw redactExceptionAndWrapAsServiceBrokerException(originalException); 
```


## How to reproduce ?
* recorded traces from production: missing `cf service` output confirming failed status 
* manual test
* acceptance test: no existing test
   * test scenario: how to avoid service instance leaks ?
      * create an instance
      * conditional deletion  
         * using params ?
            * ask to delete it with an invalid param: check exception is properly thrown 
            * ask to delete it without an invalid param: check no exception is thrown and backing service is deleted
            * Problem: delete does not accept params
         * using invocation numbers
          * first request to delete: check exception is properly thrown ("delete attempt #1 always rejected" )
          * 2nd request to delete it without an invalid param: check no exception is thrown and backing service is deleted
      * leave a leak and rely on the ci cleaning script to remove it: 
         * pb: requires execution between each execution test
            * workaround: use unique service instance names
  * test impl steps
    * [ ] Existing interceptor
    * [ ] New test case


```
$ cf services
Getting services in org osb-cmdb-services-acceptance-tests / space development as gberche...

name                                           service                                         plan       bound apps   last operation   broker                                                  upgrade available
si-delete-instance-with-sync-backing-failure   bsn-delete-instance-with-sync-backing-failure   standard                delete failed    test-broker-delete-instance-with-sync-backing-failure   
guillaume@guillaume-dev-box:~$ cf service si-delete-instance-with-sync-backing-failure
Showing info of service si-delete-instance-with-sync-backing-failure in org osb-cmdb-services-acceptance-tests / space development as gberche...

name:             si-delete-instance-with-sync-backing-failure
service:          bsn-delete-instance-with-sync-backing-failure
tags:             
plan:             standard
description:      An osb-cmdb service that deploys a backing service
documentation:    
dashboard:        https://my-dasboard-domain.org
service broker:   test-broker-delete-instance-with-sync-backing-failure

Showing status of last operation from service si-delete-instance-with-sync-backing-failure...

status:    delete failed
message:   
started:   2021-07-19T12:24:35Z
updated:   2021-07-19T12:24:37Z

There are no bound apps for this service.

Upgrades are not supported by this broker.


$ cf curl '/v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c/service_instances?q=name%3Asi-delete-instance-with-sync-backing-failure&return_user_provided_service_instances=true'{
   "total_results": 1,
   "total_pages": 1,
   "prev_url": null,
   "next_url": null,
   "resources": [
      {
         "metadata": {
            "guid": "3d6f2b76-71e8-4c03-84ab-3279b24d6860",
            "url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860",
            "created_at": "2021-07-19T12:24:14Z",
            "updated_at": "2021-07-19T12:24:14Z"
         },
         "entity": {
            "name": "si-delete-instance-with-sync-backing-failure",
            "credentials": {},
            "service_plan_guid": "9fb62c13-7918-4c11-a6cb-5ea287464f4f",
            "space_guid": "ff120500-0550-4ce5-abb6-7ad52243c70c",
            "gateway_data": null,
            "dashboard_url": "https://my-dasboard-domain.org",
            "type": "managed_service_instance",
            "last_operation": {
               "type": "delete",
               "state": "failed",
               "description": null,
               "updated_at": "2021-07-19T12:24:37Z",
               "created_at": "2021-07-19T12:24:35Z"
            },
            "tags": [],
            "maintenance_info": {},
            "service_guid": "ef771749-2b18-46f0-820b-e2971ea4c218",
            "space_url": "/v2/spaces/ff120500-0550-4ce5-abb6-7ad52243c70c",
            "service_plan_url": "/v2/service_plans/9fb62c13-7918-4c11-a6cb-5ea287464f4f",
            "service_bindings_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/service_bindings",
            "service_keys_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/service_keys",
            "routes_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/routes",
            "service_url": "/v2/services/ef771749-2b18-46f0-820b-e2971ea4c218",
            "shared_from_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/shared_from",
            "shared_to_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/shared_to",
            "service_instance_parameters_url": "/v2/service_instances/3d6f2b76-71e8-4c03-84ab-3279b24d6860/parameters"
         }
      }
   ]
}
 
```