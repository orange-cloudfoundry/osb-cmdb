
# https://github.com/orange-cloudfoundry/osb-cmdb/issues/52 osb-cmdb creat-service-instance async status is lacking message

* [x] reproduce sync failure case
   * [x] Assert exception message
  ```
  java.lang.AssertionError: 
  Expecting:
   <"CF-ServiceBrokerBadResponse(10001): Service broker error: CF-ServiceBrokerBadResponse(10001): Service broker error: SyncFailedCreateBackingSpaceInstanceInterceptor">
  to contain:
   <"com.orange.oss.osbcmdb.testfixtures.SyncFailedCreateBackingSpaceInstanceInterceptor"> 
  ```
* [x] reproduce async failure case
* [x] fix async failure case