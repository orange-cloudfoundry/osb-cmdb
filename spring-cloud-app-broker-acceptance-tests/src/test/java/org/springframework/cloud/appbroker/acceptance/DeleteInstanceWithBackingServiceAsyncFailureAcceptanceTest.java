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
import reactor.util.Logger;
import reactor.util.Loggers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class DeleteInstanceWithBackingServiceAsyncFailureAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	protected final Logger LOG = Loggers.getLogger(this.getClass());

	private static final String SUFFIX = "delete-instance-with-async-backing-failure";

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
//		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
	})
	void aFailedBackingService_is_reported_as_a_last_operation_state_failed() throws InterruptedException {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());

		//given a backend service is configured to async reject any update
		//when a brokered service deletion is requested
		deleteServiceInstance(brokeredServiceInstanceName());

		//then a brokered service deletion eventually fails
		ServiceInstance brokeredServiceInstance = pollServiceInstanceUntilNotInProgress(brokeredServiceInstanceName(), 180*1000);
		assertThat(brokeredServiceInstance.getLastOperation()).isEqualTo("delete");
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("failed");

		//and backing service is also left as failed
		backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("delete");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("failed");

	}

	@AfterEach
	void tearDown() {
		purgeServiceInstance(brokeredServiceInstanceName());
		if (backingServiceName != null) {
			purgeServiceInstance(backingServiceName, brokeredServiceName());
		}
	}

}
