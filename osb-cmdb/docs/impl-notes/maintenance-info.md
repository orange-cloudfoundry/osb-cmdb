Design maintenance info
* [x] Decide which version format to use when merging/unmerging osb-cmdb and backing service versions
   * [x] List use-cases
      * Osb-cmdb version bump 0.12.0 to 1.0.0: includes a non-breaking change by passing backing service dashboards
         * bump by a minor version.
      * cf-Mysql bump from 36.19.0 to 37.1.0: includes a non breaking change: dashboard authN,authZ is removed, mysql version change (likely )    
      * Osb-cmdb version bump 1.0.0 to 1.2.0: includes a non-breaking change: dashboard url format change (dashboard aggregator)  
      * Osb-cmdb version bump 1.2.0 to 2.0.0: includes a breaking change: dashboard aggregator format change
   * [x] Explicit requirements
      * Backend SI update need to match backend catalog see https://www.pivotaltracker.com/story/show/171702179
         * Osb-cmdb fetched backing brokers catalog is always in sync with CC since their fetch it from eq `cf marketplace` api calls
         * Osb-cmdb eq `cf update-service --upgrade` need to pass in the MI present in the backing service catalog, i.e. without the osb-cmdb merged MI.
            * **SC-OSB UpdateServiceInstanceRequest includes a Plan object with the brokered service MI. Unmerge it** 
            * Fetch the original backing service catalog in UPSI, or a cached version. 
      * Brokered catalog and SI update need to include osb-cmdb bumps to enable brokered si client cache refresh.  
   * [x] List alternatives merge and associated unmerge strategies
      * a) **Just add version numbers to both components + document osb-cmdb as build info**
         * Pros:
            * Conveys properly semver2.0 semantics, reduce risks that CC would refuse upgrade, see https://www.pivotaltracker.com/story/show/171702179
            * Simpler and more systematic algorithm. Easier to reason about
         * Cons:
            * Somewhat confusing for end-users and backend service authors
         * Unmerge: Just substract version numbers
      * b) ~~Only suffix osb-cmdb as build info.~~ Pb build info is ignored from semver precedence, and possibly from CAPI, would lead to upgrade requests ending up as noop
      * c) Just add osb-cmdb bumps 
         * Merge:
            * If backing service has MI: 
               * add osb-cmdb version bump (possibly 0.0.0, but initially 0.1.0 to trigger dashboard refresh)
               * set osb-cmdb version bump as build info 
            * If backing service has no MI: 
               * set default version (1.0.0)
               * set osb-cmdb "osb-cmdb defaulting" flag as build info 
         * UnMerge:
            * Extract osb-cmdb version bump from build info
               * If "osb-cmdb defaulting" flag, then backing service catalog has no MI. Don't pass upgrade to backing service
               * Otherwise, substract extracted osb-cmdb version bump from brokered service plan MI
         * Pros: robust and semver compliant
         * Cons: 
            * more complex
            * likely fragile: relies on build-info to be properly propagated 
            * Not much more intuitive to authors or users: still differences with backing service versions
               * E.g. https://github.com/orange-cloudfoundry/cf-mysql-release/releases includes both minor and major bumps, osb-cmdb minor bumps might as well be confusing  
* [x] Validate CF CC propagates the `maintenance_info` in its api serving `cf marketplace` 


Implement maintenance info
* Naming
   * MaintenanceInfoController(osbcmdb)
      * formatForCatalog(backing service mi)
         * merge(backing service mi, osb-cmdb mi)
      * shouldUpgradeBackingService(requested osb-cmdb-mi): 
         * unmerge(backing service mi, osb-cmdb mi))
         * true if unmerged != 0.0.0
* [x] DynamicCatalog
   * [x] configuration properties
   * [x] set maintenance info to broker value
   * [x] merge maintenance info
      * [x] add a new collaborator to OsbCmdbServiceInstance that can unit-tested: MaintenanceInfoFormatterService 
         * [x] inject PlanMapperProperties
   * [x] Make sure to set `plan_updateable: true` when defaulting MI beyond no backing service MI
      * Not necessary according to CLI tests at https://github.com/cloudfoundry/cli/blob/eb8e9de1025aa3e86ae22a10c01796a0b30a9366/command/v6/service_command_test.go#L702-L732
      * Only the presence of maintenance_info seems sufficient to trigger upgrades, no need for `plan_updateable` or `allow_context_updates`
   * [x] test
      * [x] unit test
      * [x] ~~component test~~
      * [x] ~~acceptance test~~ 
         * which minimal case to cover ? which risk not covered by UT ?
            * no specific code in ServiceInstanceService, only in DynamicCatalog which is covered by UT
         
* [x] CSI
   * [x] MI is not matching brokered service MI: reject
      * Pb: missing MI from CSIReq https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/290 
      * [x] contribute SC-OSB PR (+ bump dependency using jit pack)
* [ ] USI
   * [x] MI is not matching brokered service MI: reject
      * Pb: missing MI from USIReq https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/290 
      * [x] contribute SC-OSB PR (+ bump dependency using jit pack)
   * [x] backing broker has no maintenance info in its catalog (following unmerge) (case of cf-mysql already returning dashboard) 
      * [x] backing broker does not receive USI
         * [x] UT
         * [ ] AT
             * AsyncUgradeInstanceWithBackingServiceAcceptanceTest
                * Simulates backend broker upgrade with SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor that only differ in returned dashboard depending on presence of maintenance info
   * [ ] backing broker has maintenance info (case of coab to start returning dashboard)
      * [x] backing broker does NOT receive USI if already in same version (relying on UPSI.PreviousValues.MaintenanceInfo) 
         * [x] UT
         * [ ] AT
      * [x] backing broker does receive USI with proper maintenance info
         * [x] UT
         * [x] AT
             * AsyncUgradeInstanceWithBackingServiceAcceptanceTest
                * Simulates backend broker upgrade with SyncSuccessfulBackingSpaceInstanceWithoutDashboardUntilV2VersionInterceptor that only differ in returned dashboard depending on presence of maintenance info
                   * Pb: when dynamic catalog is enabled, it fetched the hosting platform catalog and not the AT one
                   * If we don't enable dynamic catalog, then our spring instanciates a MaintenanceInfoFormatterService with no osb-cmdb bump, and therefore our logic won't be called and like all USI with MI would be rejected
                   * Options
                      * **modify config so that MaintenanceInfoFormatterService is instanciated regardless of dynamic catalog**
                         * move maintenance_info out of `osbcmdb.dynamic-catalog.maintenance_info` at `osbcmdb.maintenance_info`
                            * MaintenanceInfoFormatterService is created in main and made available for PlanMapper to use
                         * just move PlanMapperProperties and MaintenanceInfoFormatterService into main config    
    
* [x] finish AT: 
   * [x] bump cf-java-client and sc-osb
   * [x] Relaunch acceptance test, make it pass
      * [x] Configure maintenance info bump
   * [x] Commit & push, and get feedback from AT pipeline
   * [x] Remove workarounds following cf-java-client and sc-osb bumps as possible 
   * [x] Review AT scenario indeed matches the most important scenarios
      * [x] existing dedicated brokered cf-mysql instances missing dashboard URLs (backing service had no dashboard url): COVERED
      * [x] existing shared brokered cf-mysql instances missing dashboard URLs (backing service had dashboard url): NOT YET COVERED, will be covered manually, in addition to unit test coverage. 
* [x] update README.md with supported properties and MI
* [x] integrate in paas-templates
   * [x] update manifest.yml to include new properties for enabling osb-cmdb MI bump
* [x] manually test with cf-mysql
   * [x] configure osb-cmdb-0 to use `maintenance-info` tarball
   * [x] bump one of osb-cmdb instances with an existing SI: osb-cmdb-0
      * [x] Fix PlanMapper bug
         * [x] Fix wrong expections in UT: Plan
            * [x] PlanMapperTest
         * [ ] Reproduce in AT ?
      * 4 instances available created before osb-cmdb 1.0
   * [x] manually upgrade service and check dashboard appears
   * [x] configure osb-cmdb-0 to use `p-mysql` in smoke test & check new instances indeed have dashboard url
* [x] manually test with error handling of missing backing service in USI, DSI and BS/UBS
