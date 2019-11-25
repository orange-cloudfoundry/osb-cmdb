package org.springframework.cloud.appbroker.autoconfigure;

public class PlanMapperProperties {

	public static final String PROPERTY_PREFIX = DynamicCatalogProperties.PROPERTY_PREFIX + ".plans";

	/**
	 * Define a suffix to service plan names
	 */
	public static final String SUFFIX_PROPERTY_KEY="suffix";

	private String planNameSuffix="";

	public String getPlanNameSuffix() { return planNameSuffix; }

	public void setPlanNameSuffix(String planNameSuffix) { this.planNameSuffix = planNameSuffix; }

}
