package com.orange.oss.osbcmdb.catalog;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;

class PlanMapperPropertiesTest {

	private PlanMapperProperties planMapperProperties = new PlanMapperProperties();

	@Test
	void supports_maintenance_info_format() {
		planMapperProperties.setMaintenanceInfo(MaintenanceInfo.builder()
			.version("2.0.0+coab-mysql-v47.osb-cmdb.1.1.0")
			.description("Provides access to metrics\n.Mariadb version update to y")
			.build());
	}

}