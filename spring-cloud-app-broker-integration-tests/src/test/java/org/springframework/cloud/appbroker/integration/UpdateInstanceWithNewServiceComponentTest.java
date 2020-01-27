/*
 * Copyright 2002-2020 the original author or authors.
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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.APP_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.NEW_BACKING_PLAN_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.NEW_BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.NEW_BACKING_SI_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.PLAN_NAME;
import static org.springframework.cloud.appbroker.integration.UpdateInstanceWithNewServiceComponentTest.SERVICE_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=" + SERVICE_NAME,
	"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + NEW_BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + NEW_BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + NEW_BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=" + NEW_BACKING_PLAN_NAME
})
class UpdateInstanceWithNewServiceComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-update-with-new-service";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "backing-standard";

	protected static final String NEW_BACKING_SERVICE_NAME = "new-service";

	protected static final String NEW_BACKING_SI_NAME = "my-new-service";

	protected static final String NEW_BACKING_PLAN_NAME = "new-backing-standard";

	protected static final String SERVICE_NAME = "example";

	protected static final String PLAN_NAME = "standard";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void updateAppWithNewService() {
		cloudControllerFixture.stubAppExistsWithBackingService(APP_NAME, BACKING_SI_NAME,
			BACKING_SERVICE_NAME, BACKING_PLAN_NAME);
		cloudControllerFixture.stubGetServiceInstanceWithNoBinding("instance-id", "instance-name",
			SERVICE_NAME, PLAN_NAME);
		cloudControllerFixture.stubUpdateApp(APP_NAME);

		// will unbind and delete the existing service instance
		cloudControllerFixture.stubGetBackingServiceInstance(BACKING_SI_NAME, BACKING_SERVICE_NAME, BACKING_PLAN_NAME);
		cloudControllerFixture.stubServiceBindingExists(APP_NAME, BACKING_SI_NAME);
		cloudControllerFixture.stubDeleteServiceBinding(APP_NAME, BACKING_SI_NAME);
		cloudControllerFixture.stubDeleteServiceInstance(BACKING_SI_NAME);

		// will create and bind the service instance
		cloudControllerFixture.stubServiceExists(NEW_BACKING_SERVICE_NAME, NEW_BACKING_PLAN_NAME);
		cloudControllerFixture.stubCreateServiceInstance(NEW_BACKING_SI_NAME);
		cloudControllerFixture.stubCreateServiceBinding(APP_NAME, NEW_BACKING_SI_NAME);
		cloudControllerFixture.stubServiceInstanceExists(NEW_BACKING_SI_NAME);

		// when a service instance is updated
		given(brokerFixture.serviceInstanceRequest())
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
