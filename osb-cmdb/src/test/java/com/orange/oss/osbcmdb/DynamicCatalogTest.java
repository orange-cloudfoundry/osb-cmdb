package com.orange.oss.osbcmdb;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.autoconfigure.BrokeredServicesCatalogMapper;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration;
import org.springframework.cloud.appbroker.autoconfigure.PlanMapper;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class DynamicCatalogTest {

	private final static Logger logger = LoggerFactory.getLogger(DynamicCatalogTest.class);



	//	@Test
//	void testFetchServiceDefinitions() {
//
//		Flux<ServiceDefinition> serviceDefinitions = dynamicCatalogService.fetchServiceDefinitions();
//
//		StepVerifier.create(serviceDefinitions.doOnNext(p -> logger.info("Service definitions: {}", p)))
//			.recordWith(ArrayList::new)
//			.thenConsumeWhile(x -> true)
//			.expectRecordedMatches(l -> !l.isEmpty())
//			.verifyComplete();
//	}


}
