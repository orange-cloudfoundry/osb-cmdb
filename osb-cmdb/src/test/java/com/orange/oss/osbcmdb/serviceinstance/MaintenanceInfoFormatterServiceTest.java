package com.orange.oss.osbcmdb.serviceinstance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceInfoFormatterServiceTest {

//	MaintenanceInfoFormatterService maintenanceInfoFormatterService;
//	MaintenanceInfo osbCmdbMaintenanceInfo;
//	MaintenanceInfo backendCatalogMaintenanceInfo;
//	MaintenanceInfo existingInstanceCatalogMaintenanceInfo;

	MaintenanceInfo aBackendInfoV1() {
		return MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47")
			.description("mariadb version update to x")
			.build();

	}

	MaintenanceInfo aBackendInfoV2() {
		return MaintenanceInfo.builder()
			.version("2.1.0+coab-mysql-v48")
			.description("mariadb version update to y")
			.build();

	}

	MaintenanceInfo aMergedBackendV1AndOsbCmdbV1() {
		return MaintenanceInfo.builder()
			.version("3.1.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build();
	}

	MaintenanceInfo anOsbCmdbInfoV1() {
		return MaintenanceInfo.builder()
			.version("1.1.0")
			.description("Provides access to metrics")
			.build();

	}

	MaintenanceInfo anOsbCmdbInfoV2() {
		return MaintenanceInfo.builder()
			.version("1.2.0")
			.description("Provides access to multiple dashboards")
			.build();

	}

	@DisplayName("maintenance info is served for catalog endpoint")
	@Test
	void test_format_catalog() {
		formatForCatalog(null, null, null);
		formatForCatalog(null, aBackendInfoV1(), aBackendInfoV1());
		formatForCatalog(anOsbCmdbInfoV1(), null, anOsbCmdbInfoV1());
		formatForCatalog(anOsbCmdbInfoV1(), aBackendInfoV1(), aMergedBackendV1AndOsbCmdbV1());
	}

	@DisplayName("maintenance info is served for CSI or UPI endpoints to backing service")
	@Test
	void test_format_instance() {
		//FIXME: rework the table below: wrong number of args and value
		formatForInstance(null, null, null, null);
		formatForInstance(null, aBackendInfoV1(), null, aBackendInfoV1());
		formatForInstance(anOsbCmdbInfoV1(), null, null, anOsbCmdbInfoV1());

		formatForInstance(anOsbCmdbInfoV1(), aBackendInfoV1(), null, MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("mariadb version update to x\nProvides access to metrics")
			.build());
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

	@Test
	@Disabled("to complete")
	void test_should_upgrade_backing_service() {

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

	private void formatForInstance(MaintenanceInfo osbCmdbMaintenanceInfo,
		MaintenanceInfo backendCatalogMaintenanceInfo,
		MaintenanceInfo existingInstanceCatalogMaintenanceInfo,
		Object expectedCatalogMaintenance) {
		MaintenanceInfoFormatterService maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(
			osbCmdbMaintenanceInfo);
		if (expectedCatalogMaintenance != null) {
			assertThat(maintenanceInfoFormatterService.formatForInstance(backendCatalogMaintenanceInfo,
				existingInstanceCatalogMaintenanceInfo))
				.isEqualToComparingFieldByField(expectedCatalogMaintenance);
		}
		else {
			assertThat(maintenanceInfoFormatterService
				.formatForInstance(backendCatalogMaintenanceInfo, existingInstanceCatalogMaintenanceInfo)).isNull();
		}
	}

}