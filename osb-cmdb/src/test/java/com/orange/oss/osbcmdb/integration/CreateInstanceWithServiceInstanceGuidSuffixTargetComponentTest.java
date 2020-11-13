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

package com.orange.oss.osbcmdb.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import  com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithServiceInstanceGuidSuffixTargetComponentTest.APP_NAME;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithServiceInstanceGuidSuffixTargetComponentTest.BACKING_SERVICE_NAME;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithServiceInstanceGuidSuffixTargetComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,

	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",

	"spring.cloud.appbroker.services[0].target.name=ServiceInstanceGuidSuffix"
})
@Tag("scab")
class CreateInstanceWithServiceInstanceGuidSuffixTargetComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-with-target";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "standard";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void deployAppsWithServiceInstanceGuidSuffixOnCreateServiceWhenCreatingMoreThanOnceInstance() {
		String serviceInstanceId = "instance-id";
		String applicationName = APP_NAME + "-" + serviceInstanceId;
		String backingServiceInstanceName = BACKING_SI_NAME + "-" + serviceInstanceId;

		cloudControllerFixture.stubAppDoesNotExist(applicationName);
		cloudControllerFixture.stubPushApp(applicationName);

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME, BACKING_PLAN_NAME);

		// will create and bind the service instance
		cloudControllerFixture.stubCreateServiceInstance(backingServiceInstanceName);
		cloudControllerFixture.stubCreateServiceBinding(applicationName, backingServiceInstanceName);
		cloudControllerFixture.stubServiceInstanceExists(backingServiceInstanceName);

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

		// then a new service instance was created
		String state = brokerFixture.waitForAsyncOperationComplete("instance-id");
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());

		// when a second service instance is created
		String otherServiceInstanceId = "other-instance-id";
		String otherBackingServiceInstanceName = BACKING_SI_NAME + "-" + otherServiceInstanceId;
		String otherApplicationName = APP_NAME + "-" + otherServiceInstanceId;

		cloudControllerFixture.stubAppDoesNotExist(otherApplicationName);
		cloudControllerFixture.stubPushApp(otherApplicationName);

		cloudControllerFixture.stubCreateServiceInstance(otherBackingServiceInstanceName);
		cloudControllerFixture.stubCreateServiceBinding(otherApplicationName, otherBackingServiceInstanceName);
		cloudControllerFixture.stubServiceInstanceExists(otherBackingServiceInstanceName);

		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "other-instance-id")
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), "other-instance-id")
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		// then the second instance was created
		String otherState = brokerFixture.waitForAsyncOperationComplete("other-instance-id");
		assertThat(otherState).isEqualTo(OperationState.SUCCEEDED.toString());
	}

}
