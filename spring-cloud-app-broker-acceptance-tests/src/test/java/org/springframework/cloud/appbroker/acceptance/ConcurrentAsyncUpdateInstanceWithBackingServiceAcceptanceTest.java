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

package org.springframework.cloud.appbroker.acceptance;

import java.time.Duration;

import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("cmdb")
class ConcurrentAsyncUpdateInstanceWithBackingServiceAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final Logger LOG = LoggerFactory.getLogger(
		ConcurrentAsyncUpdateInstanceWithBackingServiceAcceptanceTest.class);

	private static final String SUFFIX = "concurrent-staled-update-instance";

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	/**
	 * Maintain state before tearDown
	 */
	private String backingServiceName = null;


	@Test
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		// control backing service response
		"spring.profiles.active=acceptanceTests,ASyncStalledUpdateBackingSpaceInstanceInterceptor",
		//cf java client wire traces
		"logging.level.cloudfoundry-client.wire=debug",
//		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
	})
	void brokeredServiceUpdates() {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());

		//When requesting an invalid update request to the same broker and invalid plan id
		//then it returns a 400 Bad request
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, "invalid-plan-id"))
			.when()
			.patch(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.BAD_REQUEST.value());


		//given a backend service is configured to stall on any update
		//when a brokered service update plan is requested
		updateServiceInstanceWithoutAsserts(brokeredServiceInstanceName(), PLAN2_NAME, Duration.ofSeconds(5));

		//then a brokered service is stalled updating
		brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		assertThat(brokeredServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("in progress");

		//and backing service is also stalled updating
		backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("in progress");
		assertThat(backingServiceInstance.getPlan()).isEqualTo(PLAN_NAME); //Updated Plan name only gets published
		// once update is complete


		//And the brokered service instance service instance fetch endpoint returns a 422 status when update is
		// in progress
		final ServiceInstance updatingBrokeredServiceInstance = brokeredServiceInstance;
		ClientV2Exception exception = assertThrows(ClientV2Exception.class, () -> {
			getServiceInstanceParams(updatingBrokeredServiceInstance.getId());
		});
		assertThat(exception.toString()).contains("CF-AsyncServiceInstanceOperationInProgress");



		//When requesting a concurrent update request to the same broker with the same instance id, service
		// definition,
		// plan and params
		//then it returns a 202 accepted status
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN2_ID))
			.when()
			.patch(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());
	}

	@AfterEach
	void tearDown() {
		purgeServiceInstance(brokeredServiceInstanceName());
		if (backingServiceName != null) {
			purgeServiceInstance(backingServiceName, brokeredServiceName());
		}
	}



}
