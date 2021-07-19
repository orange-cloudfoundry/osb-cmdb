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

import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.util.Logger;
import reactor.util.Loggers;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("cmdb")
class DeleteInstanceWithBackingServiceSyncFailureAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	protected final Logger LOG = Loggers.getLogger(this.getClass());

	private static final String SUFFIX = "delete-instance-with-sync-backing-failure";

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
		"spring.profiles.active=acceptanceTests,SyncFailedDeleteBackingSpaceInstanceInterceptor",
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
	void aFailedSyncBackingService_is_reported_synchronously_with_root_cause() throws InterruptedException {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());

		//record the backing service name to ease its purge
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		backingServiceName = brokeredServiceInstance.getId();


		//given a backend service is configured to sync reject any delete with an error message
		//when a brokered service deletion is requested
		//then the original broker error message is returned to the end-user
		assertThatThrownBy(() -> { deleteServiceInstanceWithoutCatchingException(brokeredServiceInstanceName()); })
			.isInstanceOf(ClientV2Exception.class)
			.hasMessageContaining("SyncFailedDeleteBackingSpaceInstanceInterceptor");
	}

	@AfterEach
	void tearDown() {
		purgeServiceInstance(brokeredServiceInstanceName());
		if (backingServiceName != null) {
			purgeServiceInstance(backingServiceName, brokeredServiceName());
		}
	}

}
