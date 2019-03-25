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

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithEnvironmentComponentTest.APP_NAME_1;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithEnvironmentComponentTest.APP_NAME_2;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME_1,
	"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_1=value1",
	"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_2=true",
	"spring.cloud.appbroker.services[0].apps[1].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[1].name=" + APP_NAME_2,
	"spring.cloud.appbroker.services[0].apps[1].properties.use-spring-application-json=false",
	"spring.cloud.appbroker.services[0].apps[1].environment.ENV_VAR_3=value3",
	"spring.cloud.appbroker.services[0].apps[1].environment.ENV_VAR_4=true",
})
class CreateInstanceWithEnvironmentComponentTest extends WiremockComponentTest {

	static final String APP_NAME_1 = "app-with-env1";
	static final String APP_NAME_2 = "app-with-env2";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithEnvironmentVariables() {
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME_1);
		cloudControllerFixture.stubPushApp(APP_NAME_1,
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*ENV_VAR_1.*:.*value1.*/)]"),
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*ENV_VAR_2.*:.*true.*/)]"));

		cloudControllerFixture.stubAppDoesNotExist(APP_NAME_2);
		cloudControllerFixture.stubPushApp(APP_NAME_2,
			matchingJsonPath("$.environment_json[?(@.ENV_VAR_3 == 'value3')]"),
			matchingJsonPath("$.environment_json[?(@.ENV_VAR_4 == 'true')]"));

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