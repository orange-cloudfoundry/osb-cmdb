/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orange.oss.osbcmdb.integration.cmdb;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationServiceImpl;
import  com.orange.oss.osbcmdb.integration.WiremockComponentTest;
import  com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithServicesComponentTest.BACKING_SERVICE_NAME;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithServicesComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard"
})
@ContextConfiguration(classes = CreateInstanceFailureWithOnlyABackingServiceAndMetadataTransformerComponentTest.CustomConfig.class)
class CreateInstanceFailureWithOnlyABackingServiceAndMetadataTransformerComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-with-new-services";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "standard";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@BeforeAll
	void setUpReactorDebugging() {
		Hooks.onOperatorDebug();
	}



	@Test
	void createsServicesFailuresStillUpdatesMetadataAndReportsFailure() {

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME, BACKING_PLAN_NAME);

		// will fail to create the service instance
		cloudControllerFixture.stubCreateServiceInstanceFailure(BACKING_SI_NAME);

		// will list the created service instance
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		// will list the created service instance bindings with no results
		cloudControllerFixture.stubListServiceBindingsWithNoResult(BACKING_SI_NAME);

		// will update the metadata on the service instance
		// results

		Map<String, Object> labels = new HashMap<>();
		labels.put("brokered_service_instance_guid","instance-id");
		labels.put("backing_service_instance_guid", "my-db-service-GUID");
		Map<String, Object> annotations = new HashMap<>();
		cloudControllerFixture.stubUpdateServiceInstanceMetadata(BACKING_SI_NAME, labels, annotations);

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete("instance-id");
		assertThat(state).isEqualTo(OperationState.FAILED.toString());
	}

	@Configuration
	static class CustomConfig {

		@Bean
		@ConditionalOnMissingBean
		public CreateBackingServicesMetadataTransformationService createBackingServicesMetadataTransformationService() {
			return new CreateBackingServicesMetadataTransformationServiceImpl();
		}

	}

}
