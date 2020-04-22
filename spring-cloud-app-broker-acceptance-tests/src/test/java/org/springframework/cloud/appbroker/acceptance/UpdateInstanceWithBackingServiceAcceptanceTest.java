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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class UpdateInstanceWithBackingServiceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-update-service";

	private static final String SUFFIX = "update-instance";

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
		// control backing service response: use default interceptor which accepts all requests
		"spring.profiles.active=acceptanceTests",
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

		//given a backend service is configured to accept any update
		//when a brokered service update plan is requested
		updateServiceInstance(SI_NAME, PLAN2_NAME);

		//then a brokered service is updated
		ServiceInstance brokeredServiceInstance = getServiceInstance(SI_NAME);
		// then the brokered service instance once completes, is expected to be failed
		assertThat(brokeredServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("succeeded");

		//and backing service was indeed updated
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, BROKERED_SERVICE_NAME);
		//was indeed updated, and has still its last operation as failed
		assertThat(backingServiceInstance.getLastOperation()).isEqualTo("update");
		assertThat(backingServiceInstance.getStatus()).isEqualTo("succeeded");
		assertThat(backingServiceInstance.getPlan()).isEqualTo(PLAN2_NAME);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service instance is deleted
		assertThat(listServiceInstances(BROKERED_SERVICE_NAME)).doesNotContain(backingServiceName);
	}

}