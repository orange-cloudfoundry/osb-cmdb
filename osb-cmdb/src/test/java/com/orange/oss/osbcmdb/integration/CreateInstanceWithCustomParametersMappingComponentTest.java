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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture;
import com.orange.oss.osbcmdb.integration.fixtures.OpenServiceBrokerApiFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import  static  com.orange.oss.osbcmdb.integration.CreateInstanceWithCustomParametersMappingComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].name=CustomMapping"
})
@Tag("scab")
class CreateInstanceWithCustomParametersMappingComponentTest extends WiremockComponentTest {

	private static final Logger LOG =
		LoggerFactory.getLogger(CreateInstanceWithCustomParametersMappingComponentTest.class);

	protected static final String APP_NAME = "app-with-request-create-params";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithParametersTransformedUsingCustomTransformer() {
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME);
		cloudControllerFixture.stubPushApp(APP_NAME,
			matchingJsonPath(
				"$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*otherNestedKey.*:.*otherKey.*:.*keyValue.*/)]"),
			matchingJsonPath(
				"$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*otherNestedKey.*:.*otherLabel.*:.*labelValue.*/)]"));

		// given a set of parameters
		Map<String, Object> params = new HashMap<>();
		params.put("firstKey", "{\"label\":\"labelValue\",\"secondKey\":\"keyValue\"}");

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest(params))
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

