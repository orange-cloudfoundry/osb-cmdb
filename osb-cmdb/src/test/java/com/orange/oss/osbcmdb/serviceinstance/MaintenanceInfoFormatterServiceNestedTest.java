package com.orange.oss.osbcmdb.serviceinstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceInfoFormatterServiceNestedTest {

	MaintenanceInfoFormatterService maintenanceInfoFormatterService;
	MaintenanceInfo osbCmdbMaintenanceInfo;
	MaintenanceInfo backendCatalogMaintenanceInfo;
	MaintenanceInfo existingInstanceCatalogMaintenanceInfo;

	@BeforeEach
	void setUp() {
		maintenanceInfoFormatterService = new MaintenanceInfoFormatterService(osbCmdbMaintenanceInfo);
	}

	@DisplayName("When osb-cmdb has no maintenance info")
	@Nested
	class OsbCmdbHasNoMaintenanceInfo {

		@BeforeEach
		void setUp() {
			osbCmdbMaintenanceInfo = null;
		}



		@DisplayName("And backend catalog has no maintenance info")
		@Nested
		class BackendCatalogHasNoMaintenanceInfo {

			@BeforeEach
			void setUp() {
				backendCatalogMaintenanceInfo = null;
			}

			@Test
			@DisplayName("null is served in catalog")
			void catalog_is_served() {
				assertThat(formatForCatalog()).isNull();
			}



			@DisplayName("instance has no maintenance info")
			@Nested
			class WhenInstanceHasNoCatalogMaintenanceInfo {

				@BeforeEach
				void setUp() {
					existingInstanceCatalogMaintenanceInfo = null;
				}

				@Test
				@DisplayName("null is served in catalog")
				void null_is_served() {
					assertThat(formatForCatalog()).isNull();
				}


			}

			@DisplayName("instance has a maintenance info")
			@Nested
			class WhenInstanceHasCatalogMaintenanceInfo {

				@BeforeEach
				void setUp() {
					existingInstanceCatalogMaintenanceInfo = MaintenanceInfo.builder()
						.version("2.0.0+coab-mysql-v47")
						.description("mariadb version update to x")
						.build();
				}

				@Test
				@DisplayName("instance info is served in catalog")
				void instance_is_served() {
					assertThat(formatForInstance()).isEqualToComparingFieldByField(existingInstanceCatalogMaintenanceInfo);
				}


			}

		}

		@DisplayName("And backend catalog has maintenance info")
		@Nested
		class BackendCatalogHasMaintenanceInfo {

			@BeforeEach
			void setUp() {
				backendCatalogMaintenanceInfo =  MaintenanceInfo.builder()
					.version("2.1.0+coab-mysql-v47")
					.description("mariadb version update to y")
					.build();
			}

			@Test
			@DisplayName("backend info is served in catalog")
			void catalog_is_served() {
				assertThat(formatForCatalog()).isEqualToComparingFieldByField(backendCatalogMaintenanceInfo);
			}


			@DisplayName("And instance has no maintenance info")
			@Nested
			class WhenInstanceHasNoCatalogMaintenanceInfo {

				@BeforeEach
				void setUp() {
					existingInstanceCatalogMaintenanceInfo = null;
				}

			}

			@DisplayName("And instance has a maintenance info")
			@Nested
			class WhenInstanceHasCatalogMaintenanceInfo {

				@BeforeEach
				void setUp() {
					existingInstanceCatalogMaintenanceInfo = MaintenanceInfo.builder()
						.version("2.0.0+coab-mysql-v47")
						.description("mariadb version update to x")
						.build();
				}

				@Test
				@DisplayName("instance info is served in catalog")
				void null_is_served() {
					assertThat(formatForInstance()).isEqualToComparingFieldByField(existingInstanceCatalogMaintenanceInfo);
				}


			}

		}


	}

	private MaintenanceInfo formatForCatalog() {
		return maintenanceInfoFormatterService.formatForCatalog(backendCatalogMaintenanceInfo
		);
	}

	private MaintenanceInfo formatForInstance() {
		return maintenanceInfoFormatterService.formatForInstance(backendCatalogMaintenanceInfo,
			existingInstanceCatalogMaintenanceInfo);
	}


	@DisplayName("When osb-cmdb has some maintenance info")
	@Nested
	class OsbCmdbHasMaintenanceInfo {

		@BeforeEach
		void setUp() {
			osbCmdbMaintenanceInfo =
				MaintenanceInfo.builder()
				.version("2.1.0+coab-mysql-v48")
				.description("mariadb version update to y")
				.build();
		}


	}



}