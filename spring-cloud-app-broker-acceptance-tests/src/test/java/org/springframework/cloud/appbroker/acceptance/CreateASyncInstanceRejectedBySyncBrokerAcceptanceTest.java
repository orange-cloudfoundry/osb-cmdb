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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("cmdb")
/**
 * Validates that an osb-client provisionning request with "accept_incomplete=true" is indeed propagated to the
 * backing service
 */
class CreateASyncInstanceRejectedBySyncBrokerAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SUFFIX = "create-sync-only-instance";


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
		"spring.profiles.active=acceptanceTests,ASyncOnlyBackingSpaceInstanceInterceptor",
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
	void deployAppsAndCreateServiceKeysOnBindService() throws InterruptedException {
		//if a request is received with an "accept_incomplete" field that is not compatible with the current backing
		//service (accepting sync only), then make sure osb-cmdb propagates the field and the response code
		assertThatThrownBy(() -> { cloudFoundryService.createServiceInstanceLowLevel(PLAN_NAME, appServiceName(),
			emptyMap(), false).block(); })
			.isInstanceOf(ClientV2Exception.class)
			.hasMessageContaining("CF-AsyncRequired");
	}

}
