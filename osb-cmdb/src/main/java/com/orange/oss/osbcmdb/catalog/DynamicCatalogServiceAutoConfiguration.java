package com.orange.oss.osbcmdb.catalog;

import java.io.IOException;
import java.util.List;

import com.orange.oss.osbcmdb.CloudFoundryTargetProperties;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@ConditionalOnProperty(value= DynamicCatalogConstants.OPT_IN_PROPERTY)
@EnableConfigurationProperties
public class DynamicCatalogServiceAutoConfiguration {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Catalog catalog;

	@Bean
	@ConfigurationProperties(prefix = PlanMapperProperties.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public PlanMapperProperties planMapperProperties() {
		return new PlanMapperProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = ServiceDefinitionMapperProperties.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public ServiceDefinitionMapperProperties serviceDefinitionMapperProperties() {
		return new ServiceDefinitionMapperProperties();
	}

	@Bean
	public ServiceDefinitionMapper serviceDefinitionMapper(
		PlanMapper planMapper,
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		return new ServiceDefinitionMapper(planMapper, serviceDefinitionMapperProperties);
	}

	@Bean
	public PlanMapper planMapper(PlanMapperProperties planMapperProperties) {
		return new PlanMapper(planMapperProperties);
	}

	@Bean
	public DynamicCatalogService dynamicCatalogService(
		CloudFoundryOperations operations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		ServiceDefinitionMapper serviceDefinitionMapper) {

		logger.info("Will be fetching catalog from org {} and space {}",
			targetProperties.getDefaultOrg(), targetProperties.getDefaultSpace());

		return new DynamicCatalogServiceImpl(
			operations,
			cloudFoundryClient,
			targetProperties,
			serviceDefinitionMapper);
	}

	@Bean
	public Catalog catalog(DynamicCatalogService dynamicCatalogService) {
		initializeCatalog(dynamicCatalogService);
		return catalog;
	}

	private void initializeCatalog(DynamicCatalogService dynamicCatalogService) {
		if (catalog == null) {
			List<ServiceDefinition> serviceDefinitions = dynamicCatalogService.fetchServiceDefinitions();
			Assert.notEmpty(serviceDefinitions, "Unexpected empty marketplace dynamically fetched");

			this.catalog = Catalog.builder().serviceDefinitions(serviceDefinitions).build();
			Assert.notEmpty(catalog.getServiceDefinitions(),
				"Unexpected empty mapped catalog, check configured filters");


			logger.debug("Mapped catalog is: {}", catalog);
			dumpCatalogToDisk();
		}
	}

	private void dumpCatalogToDisk() {
		try {
			ServiceConfigurationYamlDumper serviceConfigurationYamlDumper = new ServiceConfigurationYamlDumper();
			serviceConfigurationYamlDumper.dumpToYamlFile(catalog);
			if (logger.isDebugEnabled()) {
				String yamlDebug = serviceConfigurationYamlDumper.dumpToYamlString(catalog);
				logger.debug("Mapped catalog yml is {}", yamlDebug);
			}
		}
		catch (IOException e) {
			//Don't fail application start
			logger.error("Unable to dump dynamic catalog to disk, caught: " + e, e);
		}
	}

}
