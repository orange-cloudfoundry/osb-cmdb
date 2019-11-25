package org.springframework.cloud.appbroker.autoconfigure;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceDefinition;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class BrokeredServicesCatalogMapperTest {
	private final static Logger logger = LoggerFactory.getLogger(BrokeredServicesCatalogMapperTest.class);


	@Test
	void mapsCatalogToBrokeredServices() {
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql", "10mb", "20mb"),
				buildServiceDefinition("noop", "default")))
			.build();

		BrokeredServices brokeredServices = new BrokeredServicesCatalogMapper().toBrokeredServices(catalog);
		logger.info("brokered services: {}", brokeredServices);
		BrokeredServices expectedBrokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("mysql", "10mb"))
			.service(buildBrokeredService("mysql", "20mb"))
			.service(buildBrokeredService("noop", "default"))
			.build();
		assertThat(brokeredServices).isEqualTo(expectedBrokeredServices);
	}

	@Test
	@Disabled(value="Not implemented")
	void testDumpsBrokeredServicesToYaml() {
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql", "10mb", "20mb"),
				buildServiceDefinition("noop", "default")))
			.build();

		BrokeredServicesCatalogMapper brokeredServicesCatalogMapper = new BrokeredServicesCatalogMapper();

		BrokeredServices expectedBrokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("mysql", "10mb"))
			.service(buildBrokeredService("mysql", "20mb"))
			.service(buildBrokeredService("noop", "default"))
			.build();

		String yaml = brokeredServicesCatalogMapper.dumpToYaml();
	}



	private ServiceDefinition buildServiceDefinition(String serviceName, String ... planNames) {
		return ServiceDefinition.builder()
			.id(serviceName + "-id")
			.name(serviceName)
			.plans(
				buildPlan(planNames))
			.build();
	}

	private Plan[] buildPlan(String[] planNames) {
		return Stream.of(planNames)
			.map(planName-> Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.build())
			.toArray(Plan[]::new);
	}

	private BackingService buildBackingService(String serviceName, String planName) {
		return BackingService.builder()
			.name(serviceName)
			.plan(planName)
			.serviceInstanceName(serviceName)
			.build();
	}

	private BrokeredService buildBrokeredService(String serviceName, String planName) {
		return
			BrokeredService.builder()
				.serviceName(serviceName)
				.planName(planName)
				.services(BackingServices.builder()
					.backingService(buildBackingService(serviceName, planName))
					.build())
				.target(TargetSpec.builder()
					.name("SpacePerServiceDefinition")
					.build())
				.build();
	}



}