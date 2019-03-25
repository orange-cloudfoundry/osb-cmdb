/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithServicesParametersComponentTest.APP_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithServicesParametersComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithServicesParametersComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",
	"spring.cloud.appbroker.services[0].services[0].parameters-transformers[0].name=ParameterMapping",
	"spring.cloud.appbroker.services[0].services[0].parameters-transformers[0].args.include=paramA,paramC"
})
class UpdateInstanceWithServicesParametersComponentTest extends WiremockComponentTest {

	static final String APP_NAME = "app-update-services-param";

	static final String BACKING_SI_NAME = "my-db-service";
	static final String BACKING_SERVICE_NAME = "db-service";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void updateAppWithBackingServicesParameters() {
		cloudControllerFixture.stubAppExists(APP_NAME);
		cloudControllerFixture.stubUpdateApp(APP_NAME);

		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		// will update with filtered parameters and bind the service instance
		HashMap<String, Object> expectedCreationParameters = new HashMap<>();
		expectedCreationParameters.put("paramA", "valueA");
		expectedCreationParameters.put("paramC", Collections.singletonMap("paramC1", "valueC1"));

		cloudControllerFixture.stubUpdateServiceInstanceWithParameters(BACKING_SI_NAME, expectedCreationParameters);

		// when a service instance is created with parameters
		HashMap<String, Object> creationParameters = new HashMap<>();
		creationParameters.put("paramA", "valueA");
		creationParameters.put("paramB", "valueB");
		creationParameters.put("paramC", Collections.singletonMap("paramC1", "valueC1"));

		given(brokerFixture.serviceInstanceRequest(creationParameters))
			.when()
			.patch(brokerFixture.createServiceInstanceUrl(), "instance-id")
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
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}
}