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

package org.springframework.cloud.appbroker.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SI_NAME;


@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",
	"service-bindings-as-service-keys=true"
})
class CreateServiceKeyBindingComponentTest extends WiremockComponentTest {

	private static final String SERVICE_INSTANCE_ID = "instance-id";
	private static final String BINDING_ID = "binding-id";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	@Test
	void createAppBindingCreatesBackendServiceKey() {

		// when a service binding is created
		given(brokerFixture.serviceAppBindingRequest())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value());
	}

	@Test
	@Disabled
	void createServiceKeyCreatesBackendServiceKey() {

		// when a service binding is created
		given(brokerFixture.serviceKeyRequest())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value());
	}

	protected static final String APP_NAME = "app-with-new-services";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	@Disabled
	void createsServicesWhenOnlyBackingServiceIsRequested() {

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME);

		// will create the service instance
		cloudControllerFixture.stubCreateServiceInstance(BACKING_SI_NAME);

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
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

}