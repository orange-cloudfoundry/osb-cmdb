package org.springframework.cloud.appbroker.autoconfigure;

public class ServiceDefinitionMapperProperties {

	public static final String PROPERTY_PREFIX = DynamicCatalogProperties.PROPERTY_PREFIX + ".catalog.services";

	/**
	 * Define a suffix to service names
	 */
	public static final String SUFFIX_PROPERTY_KEY="suffix";

	private String suffix ="";

	public String getSuffix() { return suffix; }

	public void setSuffix(String suffix) { this.suffix = suffix; }

}
