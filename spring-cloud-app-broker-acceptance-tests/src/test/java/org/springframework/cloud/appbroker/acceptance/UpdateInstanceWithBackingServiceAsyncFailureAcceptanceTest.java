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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class UpdateInstanceWithBackingServiceAsyncFailureAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-update-service-async-fail";

	private static final String SUFFIX = "update-instance-with-async-backing-failure";

	private static final String BROKERED_SERVICE_NAME = "app-service-" + SUFFIX;

	@Override
	String brokeredServiceName() { return BROKERED_SERVICE_NAME; }

	@Override
	protected String testSuffix() {
		return SUFFIX;
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
		"spring.profiles.active=acceptanceTests,AsyncFailedUpdateBackingSpaceInstanceInterceptor",
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

		//given a backend service is configured to async reject any update
		//when a brokered service update is requested
		ServiceInstance brokeredServiceInstance = updateServiceInstanceWithoutAsserts(SI_NAME, Collections.emptyMap());

		//then a brokered service is updated with failed status
		assertThat(brokeredServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("failed");

		//and backing service was indeed updated
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("failed");

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service instance is deleted
		assertThat(listServiceInstances(brokeredServiceName())).doesNotContain(backingServiceName);
	}

}
