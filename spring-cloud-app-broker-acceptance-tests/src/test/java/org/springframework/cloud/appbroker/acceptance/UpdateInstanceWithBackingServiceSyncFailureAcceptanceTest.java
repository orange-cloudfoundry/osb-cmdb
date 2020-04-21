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

import java.util.Collections;

import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class UpdateInstanceWithBackingServiceSyncFailureAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-update-service-sync-fail";

	private static final String SUFFIX = "update-instance-with-sync-backing-failure";

	private static final String BROKERED_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;


	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@Override
	protected String appServiceName() {
		return BROKERED_SERVICE_NAME;
	}

	@Override
	protected String backingServiceName() {
		return BACKING_SERVICE_NAME;
	}

	@Test
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		// control backing service response: have it fail async
		"spring.profiles.active=acceptanceTests,SyncFailedUpdateBackingSpaceInstanceInterceptor",
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
	void aFailedBackingService_is_reported_as_a_last_operation_state_failed() {
		// given a brokered service instance is created
		createServiceInstance(SI_NAME);
		// then a brokered service instance is created
		ServiceInstance brokeredServiceInstance = getServiceInstance(SI_NAME);

		//given a backend service is configured to reject any update
		//when a brokered service update is requested
		try {
			updateServiceInstance(SI_NAME, Collections.emptyMap());
			Assertions.fail("Expected sync CSI failure");
		}
		catch (Exception e) {
			//then a sync exception is returned
		}
		// and the associated backing service instance
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, BROKERED_SERVICE_NAME);
		//and the backing service has the right type
		assertThat(backingServiceInstance.getService()).isEqualTo(BROKERED_SERVICE_NAME);
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getStatus()).isEqualTo("failed");


		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service instance is deleted
		assertThat(listServiceInstances(BROKERED_SERVICE_NAME)).doesNotContain(backingServiceName);
	}

}