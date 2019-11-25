package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

@Configuration
@AutoConfigureBefore(AppBrokerAutoConfiguration.class)
@ConditionalOnProperty(value=DynamicCatalogProperties.OPT_IN_PROPERTY)
public class DynamicCatalogServiceAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCatalogServiceAutoConfiguration.class);
	private BrokeredServices brokeredServices;
	private Catalog catalog;

	@Bean
	@ConfigurationProperties(DynamicCatalogProperties.PROPERTY_PREFIX)
	public DynamicCatalogProperties dynamicCatalogProperties() {
		return new DynamicCatalogProperties();
	}

	@Bean
	@ConfigurationProperties(PlanMapperProperties.PROPERTY_PREFIX)
	public PlanMapperProperties planMapperProperties() {
		return new PlanMapperProperties();
	}

	@Bean
	@ConfigurationProperties(ServiceDefinitionMapperProperties.PROPERTY_PREFIX)
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
		CloudFoundryDeploymentProperties defaultDeploymentProperties,
		CloudFoundryOperations operations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		ServiceDefinitionMapper serviceDefinitionMapper) {

		return new DynamicCatalogServiceImpl(
			defaultDeploymentProperties,
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

	@Bean
	public BrokeredServices brokeredServices(DynamicCatalogService dynamicCatalogService) {
		initializeCatalog(dynamicCatalogService);
		return brokeredServices;
	}

	private void initializeCatalog(DynamicCatalogService dynamicCatalogService) {
		if (catalog == null || brokeredServices == null) {
			List<ServiceDefinition> serviceDefinitions = dynamicCatalogService.fetchServiceDefinitions();
			Assert.notEmpty(serviceDefinitions, "Unexpected empty marketplace dynamically fetched");

			this.catalog = Catalog.builder().serviceDefinitions(serviceDefinitions).build();
			Assert.notEmpty(catalog.getServiceDefinitions(),
				"Unexpected empty mapped catalog, check configured filters");
			BrokeredServicesCatalogMapper brokeredServicesCatalogMapper = new BrokeredServicesCatalogMapper();

			brokeredServices = brokeredServicesCatalogMapper.toBrokeredServices(this.catalog);
			Assert.notEmpty(catalog.getServiceDefinitions(),
				"Unexpected empty list of brokered services, check configured filters");
		}
	}

}
