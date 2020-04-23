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

import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class DeleteInstanceWithBackingServiceAsyncFailureAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-delete-service-async-fail";

	private static final String SUFFIX = "delete-instance-with-async-backing-failure";

	private static final String BROKERED_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;

	@Override
	protected String appServiceName() {
		return BROKERED_SERVICE_NAME;
	}

	@Override
	protected String backingServiceName() {
		return BACKING_SERVICE_NAME;
	}

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
		// control backing service response: have it fail async
		"spring.profiles.active=acceptanceTests,AsyncFailedDeleteBackingSpaceInstanceInterceptor",
		//cf java client wire traces
		"logging.level.cloudfoundry-client.wire=debug",
		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
	})
	void aFailedBackingService_is_reported_as_a_last_operation_state_failed() throws InterruptedException {
		// given a brokered service instance is created
		createServiceInstance(SI_NAME);

		//given a backend service is configured to async reject any update
		//when a brokered service deletion is requested
		deleteServiceInstance(SI_NAME);

		//then a brokered service deletion eventually fails


		ServiceInstance brokeredServiceInstance;
		int retry=0;
		final int MAX_POLL_DURATION_MS = 60*1000;
		long pollStartTime = currentTimeMillis();
		do {
			brokeredServiceInstance = getServiceInstance(SI_NAME);
			if (retry >0) {
				//noinspection BusyWait
				Thread.sleep(5*1000);
			}
			retry++;
		} while (
			brokeredServiceInstance.getStatus().equals("in progress") &&
			timehasElapsedLessThan(MAX_POLL_DURATION_MS, pollStartTime)
		);
		assertThat(brokeredServiceInstance.getLastOperation()).isEqualTo("delete");
		assertThat(brokeredServiceInstance.getStatus())
			.withFailMessage("after retrying " + retry + " times and " + (currentTimeMillis() - pollStartTime)/1000 + " seconds")
		.isEqualTo("failed");

		//and backing service is also left as failed
		backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, BROKERED_SERVICE_NAME);
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("delete");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("failed");

	}

	private boolean timehasElapsedLessThan(int MAX_POLL_DURATION_MS, long pollStartTime) {
		return currentTimeMillis() - pollStartTime < MAX_POLL_DURATION_MS;
	}

	@AfterEach
	void tearDown() {
		purgeServiceInstance(SI_NAME);
		if (backingServiceName != null) {
			purgeServiceInstance(backingServiceName, BROKERED_SERVICE_NAME);
		}
	}

}
