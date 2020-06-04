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

	@DisplayName("validates upgrade requests include invalid maintenance info")
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
			.isInstanceOf(ServiceBrokerMaintenanceInfoConflictException.class);
	}

	@DisplayName("an upgrade request should be a noop, when backing service has no MI")
	@Test
	void test_isNoOpUpgradeBackingService_when_default_backend() {
		//Given
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			anOsbCmdbInfoV1());
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
		MaintenanceInfo mergedCmdbBackendMI = maintenanceInfoFormatterService.formatForCatalog(aBackendInfoV1());
		UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
			.planId("a-plan")
			.maintenanceInfo(mergedCmdbBackendMI)
			.previousValues(new UpdateServiceInstanceRequest.PreviousValues("a-plan", null))
			.plan(Plan.builder()
				.maintenanceInfo(mergedCmdbBackendMI)
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