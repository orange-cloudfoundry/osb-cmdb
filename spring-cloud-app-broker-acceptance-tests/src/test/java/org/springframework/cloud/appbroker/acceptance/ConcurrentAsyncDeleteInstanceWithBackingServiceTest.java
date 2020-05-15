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

import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class ConcurrentAsyncDeleteInstanceWithBackingServiceTest extends CmdbCloudFoundryAcceptanceTest {

	protected final Logger LOG = Loggers.getLogger(this.getClass());

	private static final String SUFFIX = "delete-instance-with-async-stalled";

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
		// control backing service response: have it stall async on delete
		"spring.profiles.active=acceptanceTests,ASyncStalledDeleteBackingSpaceInstanceInterceptor",
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
	void aConcurrentDeprovisionService_is_reported_with_right_status() {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());

		//given a backend service is configured to async stall
		//when a brokered service deletion is requested
		deleteServiceInstanceWithoutAsserts(brokeredServiceInstanceName(), Duration.ofSeconds(5));

		//then a backing service is also left as pending deletion
		backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("delete");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("in progress");

		//When requesting a concurrent deprovision request to the same broker with the same instance id, service
		// definition,
		// plan and params
		//then it returns a 202 accepted status
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN_ID))
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(SERVICE_ID, PLAN_ID), brokeredServiceInstance.getId())
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
