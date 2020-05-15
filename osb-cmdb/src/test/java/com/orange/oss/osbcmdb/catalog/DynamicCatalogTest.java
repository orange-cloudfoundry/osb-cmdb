package com.orange.oss.osbcmdb.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
