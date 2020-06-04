package com.orange.oss.osbcmdb.catalog;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.lang.Nullable;

public class PlanMapperProperties {

	public static final String PROPERTY_PREFIX = DynamicCatalogConstants.PROPERTY_PREFIX + ".plans";

	/**
	 * The Osb-cmdb maintenance info that augments the backing services maintenance info as build metadata, see
	 * https://semver.org/spec/v2.0.0.html
	 */
	@Nullable
	public MaintenanceInfo maintenanceInfo;

	public MaintenanceInfo getMaintenanceInfo() {
		return maintenanceInfo;
	}

	public void setMaintenanceInfo(MaintenanceInfo maintenanceInfo) {
		this.maintenanceInfo = maintenanceInfo;
	}

	//For future use
//	/**
//	 * Define a suffix to service plan names
//	 */
//	public static final String SUFFIX_PROPERTY_KEY="suffix";
//
//	private String planNameSuffix="";
//
//	public String getPlanNameSuffix() { return planNameSuffix; }
//
//	public void setPlanNameSuffix(String planNameSuffix) { this.planNameSuffix = planNameSuffix; }
//
}
