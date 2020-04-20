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

import com.orange.oss.osbcmdb.integration.CreateInstanceWithServicesComponentTest;
import com.orange.oss.osbcmdb.integration.WiremockComponentTest;
import com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture;
import com.orange.oss.osbcmdb.integration.fixtures.OpenServiceBrokerApiFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static com.orange.oss.osbcmdb.integration.CreateInstanceWithServicesComponentTest.BACKING_SERVICE_NAME;
import static com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture.serviceInstanceGuid;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@TestPropertySource(properties = {
	"spring.cloud.openservicebroker.catalog.services[0].id=SERVICE_ID",
	"spring.cloud.openservicebroker.catalog.services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.openservicebroker.catalog.services[0].description=A service that deploys a backing app",
	"spring.cloud.openservicebroker.catalog.services[0].bindable=true",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=PLAN_ID",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].name=standard",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable=true",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].description=A simple plan",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].free=true",
})
class AsyncCreateInstanceFailureStillAssignsMetadataComponentTest extends WiremockComponentTest {

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "standard";

	public static final String BROKERED_SERVICE_INSTANCE_ID = "instance-id";

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

		//given a space creation request is made
		cloudControllerFixture.stubCreateSpace(BACKING_SERVICE_NAME);
		cloudControllerFixture.stubAssociatePermissions(BACKING_SERVICE_NAME);

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME, BACKING_PLAN_NAME);

		// given CSI returns 202 accepted
		cloudControllerFixture.stubCreateServiceInstanceAsync(BROKERED_SERVICE_INSTANCE_ID);

		// given the created service instance is listed within space
		cloudControllerFixture.stubServiceInstanceExists(BROKERED_SERVICE_INSTANCE_ID, BACKING_SERVICE_NAME,
			BACKING_PLAN_NAME);

		// given the service instance can be looked up by name in the space
		cloudControllerFixture.stubGetBackingServiceInstance(BROKERED_SERVICE_INSTANCE_ID, BACKING_SERVICE_NAME,
			CreateInstanceWithServicesComponentTest.BACKING_PLAN_NAME);

		// will list the created service instance bindings with no results
		cloudControllerFixture.stubListServiceBindingsWithNoResult(BROKERED_SERVICE_INSTANCE_ID);

		// will update the metadata on the service instance
		// results
		Map<String, Object> labels = new HashMap<>();
		labels.put("brokered_service_instance_guid", BROKERED_SERVICE_INSTANCE_ID);
		labels.put("backing_service_instance_guid", serviceInstanceGuid(BROKERED_SERVICE_INSTANCE_ID));
		Map<String, Object> annotations = new HashMap<>();
		cloudControllerFixture.stubUpdateServiceInstanceMetadata(BROKERED_SERVICE_INSTANCE_ID, labels, annotations);

		// when a service instance provisionning is requested
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), BROKERED_SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), BROKERED_SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete(BROKERED_SERVICE_INSTANCE_ID);
		assertThat(state).isEqualTo(OperationState.FAILED.toString());
	}

}
