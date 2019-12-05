package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceDefinition;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class BrokeredServicesCatalogMapper {

	public BrokeredServices toBrokeredServices(Catalog catalog) {
		List<BrokeredServices> services = catalog.getServiceDefinitions().stream()
			.map(this::toBrokeredServices)
			.collect(Collectors.toList());

		BrokeredServices.BrokeredServicesBuilder builder = BrokeredServices.builder();
		services.stream()
			.sequential()
			.forEach(builder::services);
		return builder.build();
	}

	private BrokeredServices toBrokeredServices(ServiceDefinition serviceDefinition) {
		BrokeredServices.BrokeredServicesBuilder builder = BrokeredServices.builder();
		serviceDefinition.getPlans().stream().sequential()
			.forEach(p -> builder.service(toBrokeredService(serviceDefinition.getName(), p.getName())));
		return builder.build();
	}

	private BrokeredService toBrokeredService(String serviceName, String planName) {
		return
			BrokeredService.builder()
				.serviceName(serviceName)
				.planName(planName)
				.services(BackingServices.builder()
					.backingService(toBackingService(serviceName, planName))
					.build())
				.target(TargetSpec.builder()
					.name(SpacePerServiceDefinition.class.getSimpleName())
					.build())
				.build();
	}

	private BackingService toBackingService(String serviceName, String planName) {
		return BackingService.builder()
			.name(serviceName)
			.plan(planName)
			.serviceInstanceName(serviceName)
			.build();
	}



}
