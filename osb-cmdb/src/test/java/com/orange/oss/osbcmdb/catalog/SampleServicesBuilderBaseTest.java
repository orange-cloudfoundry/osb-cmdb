package com.orange.oss.osbcmdb.catalog;

import java.util.stream.Stream;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class SampleServicesBuilderBaseTest {

	protected ServiceDefinition buildServiceDefinition(String serviceName, String... planNames) {
		return ServiceDefinition.builder()
			.id(serviceName + "-id")
			.name(serviceName)
			.description("description " +  serviceName)
			.plans(
				buildPlan(planNames))
			.build();
	}

	protected Plan[] buildPlan(String[] planNames) {
		return Stream.of(planNames)
			.map(planName-> Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.description("description " +  planName)
				.build())
			.toArray(Plan[]::new);
	}

}
