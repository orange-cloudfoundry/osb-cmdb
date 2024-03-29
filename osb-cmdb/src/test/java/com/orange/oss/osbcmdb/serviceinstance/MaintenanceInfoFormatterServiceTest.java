package com.orange.oss.osbcmdb.serviceinstance;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerMaintenanceInfoConflictException;
import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceInfoFormatterServiceTest {

	@DisplayName("maintenance info is served for catalog endpoint")
	@Test
	void test_format_catalog() {
		formatForCatalog(null, null, null);
		formatForCatalog(null, aBackendInfoV1(), aBackendInfoV1());
		formatForCatalog(anOsbCmdbInfoV1(), null, anOsbCmdbInfoV1());
		formatForCatalog(anOsbCmdbInfoV1(), aBackendInfoV1(), aMergedBackendV1AndOsbCmdbV1());
	}

	@DisplayName("validates upgrade requests include valid maintenance info")
	@Test
	void test_validate_upgrade_request() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.maintenanceInfo(anOsbCmdbInfoV1())
				.build())
			.maintenanceInfo(anOsbCmdbInfoV1())
			.build();
		maintenanceInfoFormatterService.validateAnyUpgradeRequest(updateServiceInstanceRequest);
	}

	@DisplayName("validates upgrade requests include valid maintenance info with only version and no decription")
	@Test
	void test_validate_upgrade_request_only_version() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		MaintenanceInfo abOsbCmdbV1Version = MaintenanceInfo.builder()
			.version(anOsbCmdbInfoV1().getVersion())
			.build();
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.maintenanceInfo(abOsbCmdbV1Version)
				.build())
			.maintenanceInfo(abOsbCmdbV1Version)
			.build();
		maintenanceInfoFormatterService.validateAnyUpgradeRequest(updateServiceInstanceRequest);
	}

	@DisplayName("validates upgrade requests include valid maintenance info")
	@Test
	void test_validate_upgrade_request_no_mi() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.build())
			.build();
		maintenanceInfoFormatterService.validateAnyUpgradeRequest(updateServiceInstanceRequest);
	}

	@DisplayName("validates upgrade requests include valid maintenance info (when no plan specified)")
	@Test
	void test_validate_upgrade_request_no_plan_specified() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.build();
		maintenanceInfoFormatterService.validateAnyUpgradeRequest(updateServiceInstanceRequest);
	}

	@DisplayName("validates upgrade requests include valid maintenance info")
	@Test
	void test_validate_create_request() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.maintenanceInfo(anOsbCmdbInfoV1())
				.build())
			.maintenanceInfo(anOsbCmdbInfoV1())
			.build();
		maintenanceInfoFormatterService.validateAnyCreateRequest(createServiceInstanceRequest);
	}

	@DisplayName("validates upgrade requests include invalid maintenance info")
	@Test
	void test_validate_conflicting_upgrade_request() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.maintenanceInfo(anOsbCmdbInfoV1())
				.build())
			.maintenanceInfo(MaintenanceInfo.builder()
				.version("6.3.2")
				.description("Unrelated version")
				.build())
			.build();
		assertThatThrownBy(() -> maintenanceInfoFormatterService.validateAnyUpgradeRequest(updateServiceInstanceRequest))
		.isInstanceOf(ServiceBrokerMaintenanceInfoConflictException.class);
	}

	@DisplayName("rejects create requests with invalid maintenance info")
	@Test
	void test_validate_conflicting_create_request() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
			.plan(Plan.builder()
				.maintenanceInfo(anOsbCmdbInfoV1())
				.build())
			.maintenanceInfo(MaintenanceInfo.builder()
				.version("6.3.2")
				.description("Unrelated version")
				.build())
			.build();
		assertThatThrownBy(() -> maintenanceInfoFormatterService.validateAnyCreateRequest(createServiceInstanceRequest))
			.isInstanceOf(ServiceBrokerMaintenanceInfoConflictException.class)
			.hasMessageContaining("Potentially due to a stale copy of service catalog in client platform. Please " +
				"check with platform " +
				"owner whether the equivalent of \"cf update-service-broker\" was recently applied")
		;
	}

	/**
	 *  Rationale: we are expecting osb client (CF) to be filtering noops upstreams.
	 *  We only force noops for osb-cmdb upgrades (i.e. without backend upgrades)
	 */
	@DisplayName("an upgrade request should not be a noop, when formatter has no osb-cmdb bump, regardless of backend and request")
	@Test
	void test_isNoOpUpgradeBackingService_without_cmdb_bump() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			null);
		MaintenanceInfo brokeredServiceMi = maintenanceInfoFormatterService.formatForCatalog(aBackendInfoV2());
		UpdateServiceInstanceRequest updateServiceInstanceRequestWithMaintenanceUpgradeRequested = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.maintenanceInfo(brokeredServiceMi)
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", aBackendInfoV1()))
			.plan(Plan.builder()
				.maintenanceInfo(brokeredServiceMi)
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequestWithMaintenanceUpgradeRequested);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("an upgrade request should be a noop, when backing service has no MI")
	@Test
	void test_isNoOpUpgradeBackingService_when_default_backend() {
		//Given
		MaintenanceInfo osbCmdbMaintenanceInfo = anOsbCmdbInfoV1();
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			osbCmdbMaintenanceInfo);
		MaintenanceInfo cmdbDefaultMI = maintenanceInfoFormatterService.formatForCatalog(null);
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", null))
			.maintenanceInfo(cmdbDefaultMI)
			.plan(Plan.builder()
				.maintenanceInfo(cmdbDefaultMI)
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isTrue();
	}

	@DisplayName("an upgrade request should NOT be a noop, when backing service has some MI")
	@Test
	void test_isNoOpUpgradeBackingService_with_specific_backend() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		MaintenanceInfo brokeredServiceMi = maintenanceInfoFormatterService.formatForCatalog(aBackendInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.maintenanceInfo(brokeredServiceMi)
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", null))
			.plan(Plan.builder()
				.maintenanceInfo(brokeredServiceMi)
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("an upgrade request should NOT be a noop, when backing service has some older MI")
	@Test
	void test_isNoOpUpgradeBackingService_with_older_specific_backend() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		MaintenanceInfo brokeredServiceMi = maintenanceInfoFormatterService.formatForCatalog(aBackendInfoV2());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.maintenanceInfo(brokeredServiceMi)
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", aBackendInfoV1()))
			.plan(Plan.builder()
				.maintenanceInfo(brokeredServiceMi)
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("an update request should NOT be a noop, when params requested, even if backend has no MI")
	@Test
	void test_isNoOpUpgradeBackingService_with_params() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", null))
			.maintenanceInfo(null) //no MI passed in
			.parameters(Collections.singletonMap("a-key", "a-value"))
			.plan(Plan.builder()
				.maintenanceInfo(maintenanceInfoFormatterService.formatForCatalog(null))
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("an update request should NOT be a noop, when params requested (no plan update), even if backend has" +
		" no MI")
	@Test
	void test_isNoOpUpgradeBackingService_with_params_without_plan() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.parameters(Collections.singletonMap("a-key", "a-value"))
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("an update request should NOT be a noop, when plan update requested, even if backend has no MI")
	@Test
	void test_isNoOpUpgradeBackingService_with_plan_update() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("another-plan", null))
			.plan(Plan.builder()
				.maintenanceInfo(maintenanceInfoFormatterService.formatForCatalog(null))
				.build())
			.build();
		//when
		boolean isNoOpUpgradeBackingService = maintenanceInfoFormatterService
			.isNoOpUpgradeBackingService(updateServiceInstanceRequest);
		assertThat(isNoOpUpgradeBackingService).isFalse();
	}

	@DisplayName("Formats the request to send to backend service : case of a client K8S svcat request without MI/ plan")
	@Test
	void test_formatForBackendInstance_case0() {
		//given an osb-cmdb configured with a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		//when receiving a request made by a client from catalog
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.build());
		//then no MI is passed to backend
		assertThat(requestBackendServiceMi).isNull();
		//actually this should previously be detected as a noop and update should not be requested to backend
	}

	@DisplayName("Formats the request to send to backend service : case of backend service has no Mi and osb-cmdb " +
		"bump " +
		"(e.g. cf-mysql")
	@Test
	void test_formatForBackendInstance_case1() {
		//given an osb-cmdb configured with a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		//when receiving a request made by a client from catalog
		MaintenanceInfo brokeredServiceMi = anOsbCmdbInfoV1();
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.maintenanceInfo(brokeredServiceMi)
				.plan(Plan.builder()
					.maintenanceInfo(brokeredServiceMi)
					.build())
				.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan-id", null))
				.build());
		//then no MI is passed to backend
		assertThat(requestBackendServiceMi).isNull();
		//actually this should previously be detected as a noop and update should not be requested to backend
	}

	@DisplayName("Formats the request to send to backend service : case of backend service has Mi and osb-cmdb bump " +
		"(e.g. coab mysql)")
	@Test
	void test_formatForBackendInstance_case2() {
		//given an osb-cmdb configured with a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		//when receiving a request made by a client from catalog
		MaintenanceInfo brokeredServiceMi = aMergedBackendV1AndOsbCmdbV1();
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.maintenanceInfo(brokeredServiceMi)
				.plan(Plan.builder()
					.maintenanceInfo(brokeredServiceMi)
					.build())
				.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan-id", null))
				.build());
		//then request to pass to backend
		assertThat(requestBackendServiceMi.getVersion()).isEqualTo(aBackendInfoV1().getVersion());
		assertThat(requestBackendServiceMi.getDescription()).isEqualTo(aBackendInfoV1().getDescription());
	}

	@DisplayName("Formats the request to send to backend service : case of backend service has outdated Mi and osb-cmdb bump (e.g. coab mysql)")
	@Test
	void test_formatForBackendInstance_case3() {
		//given an osb-cmdb configured with a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
		//when receiving a request made by a client from catalog
		MaintenanceInfo brokeredServiceMi = aMergedBackendV2AndOsbCmdbV1();
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.maintenanceInfo(brokeredServiceMi)
				.plan(Plan.builder()
					.maintenanceInfo(brokeredServiceMi)
					.build())
				.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan-id", null))
				.build());
		//then request to pass to backend
		assertThat(requestBackendServiceMi.getVersion()).isEqualTo(aBackendInfoV2().getVersion());
		assertThat(requestBackendServiceMi.getDescription()).isEqualTo(aBackendInfoV2().getDescription());
	}

	@DisplayName("Formats the request to send to backend service : case of backend service has Mi and NO osb-cmdb " +
		"bump")
	@Test
	void test_formatForBackendInstance_case4() {
		//given an osb-cmdb configured without a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			null);
		//when receiving a request made by a client from catalog with backend MI
		MaintenanceInfo brokeredServiceMi = aBackendInfoV1();
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.maintenanceInfo(brokeredServiceMi)
				.plan(Plan.builder()
					.maintenanceInfo(brokeredServiceMi)
					.build())
				.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan-id", null))
				.build());
		//then request to pass to backend
		assertThat(requestBackendServiceMi.getVersion()).isEqualTo(aBackendInfoV1().getVersion());
		assertThat(requestBackendServiceMi.getDescription()).isEqualTo(aBackendInfoV1().getDescription());
	}

	@DisplayName("Formats the request to send to backend service : case of backend service has Mi, request has no MI, and NO osb-cmdb bump")
	@Test
	void test_formatForBackendInstance_case5() {
		//given an osb-cmdb configured without a bump
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			null);
		//when receiving a request made by a client from catalog without MI
		MaintenanceInfo brokeredServiceMi = aMergedBackendV1AndOsbCmdbV1();
		org.cloudfoundry.client.v2.MaintenanceInfo requestBackendServiceMi = maintenanceInfoFormatterService
			.formatForBackendInstance(UpdateServiceInstanceRequest.builder()
				.maintenanceInfo(null)
				.plan(Plan.builder()
					.maintenanceInfo(brokeredServiceMi)
					.build())
				.build());
		//then request to pass to backend
		assertThat(requestBackendServiceMi).isNull();
	}



	@DisplayName("Merges info with backend build")
	@Test
	void test_merge_infos_1() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		assertMergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Merges info without backend build")
	@Test
	void test_merge_infos_2() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("2.0.0")
			.description("mariadb version update to x")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("3.1.0+osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		assertMergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Merges info with cmdb and backend build")
	@Test
	void test_merge_infos_3() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0+hotfix.01042020")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0.hotfix.01042020")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		assertMergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Unmerges info with backend build")
	@Test
	void test_unmerge_infos_1() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();
		assertUnmergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Unmerges info without backend build")
	@Test
	void test_unmerge_infos_2() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("3.1.0+osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("2.0.0")
			.description("mariadb version update to x")
			.build();
		assertUnmergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Unmerges info with cmdb and backend build")
	@Test
	void test_unmerge_infos_3() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0+hotfix.01042020")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0.hotfix.01042020")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();
		assertUnmergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	@DisplayName("Unmerges info without backend mi")
	@Test
	void test_unmerge_infos_4() {
		MaintenanceInfo osbCmdbMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0+hotfix.01042020")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo backendMaintenanceInfo = MaintenanceInfo.builder()
			.version("1.1.0+hotfix.01042020")
			.description("Provides access to metrics")
			.build();
		MaintenanceInfo expected = MaintenanceInfo.builder()
			.version("0.0.0")
			.description("")
			.build();
		assertUnmergeInfo(osbCmdbMaintenanceInfo, backendMaintenanceInfo, expected);
	}

	private MaintenanceInfo aBackendInfoV1() {
		return MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();

	}

	private MaintenanceInfo aBackendInfoV2() {
		return MaintenanceInfo.builder()
			.version("2.1.0+coab-mysql-v48")
			.description("mariadb version update to y")
			.build();

	}

	private MaintenanceInfo aMergedBackendV1AndOsbCmdbV1() {
		return MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
	}

	private MaintenanceInfo aMergedBackendV2AndOsbCmdbV1() {
		return MaintenanceInfo.builder()
			.version("3.2.0+coab-mysql-v48.osb-cmdb.1.1.0")
			.description("mariadb version update to y\nProvides access to metrics")
			.build();
	}

	private MaintenanceInfo anOsbCmdbInfoV1() {
		return MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();

	}

	private MaintenanceInfo anOsbCmdbInfoV2() {
		return MaintenanceInfo.builder()
			.version("1.2.0")
			.description("Provides access to multiple dashboards")
			.build();

	}

	private void assertMergeInfo(MaintenanceInfo osbCmdbMaintenanceInfo, MaintenanceInfo backendMaintenanceInfo,
		MaintenanceInfo expected) {
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			osbCmdbMaintenanceInfo);
		MaintenanceInfo mergedInfos = maintenanceInfoFormatterService.mergeInfos(backendMaintenanceInfo);
		assertThat(mergedInfos).isEqualToComparingFieldByField(expected);
	}

	private void assertUnmergeInfo(MaintenanceInfo osbCmdbMaintenanceInfo, MaintenanceInfo backendMaintenanceInfo,
		MaintenanceInfo expected) {
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			osbCmdbMaintenanceInfo);
		MaintenanceInfo mergedInfos = maintenanceInfoFormatterService.unmergeInfos(backendMaintenanceInfo);
		assertThat(mergedInfos).isEqualToComparingFieldByField(expected);
	}

	private void formatForCatalog(MaintenanceInfo osbCmdbMaintenanceInfo, MaintenanceInfo backendCatalogMaintenanceInfo,
		Object expectedCatalogMaintenance) {
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			osbCmdbMaintenanceInfo);
		if (expectedCatalogMaintenance != null) {
			assertThat(maintenanceInfoFormatterService.formatForCatalog(backendCatalogMaintenanceInfo))
				.isEqualToComparingFieldByField(expectedCatalogMaintenance);
		}
		else {
			assertThat(maintenanceInfoFormatterService.formatForCatalog(backendCatalogMaintenanceInfo)).isNull();
		}
	}

}