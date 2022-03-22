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

import java.util.List;

import org.assertj.core.api.Assertions;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import reactor.core.publisher.Hooks;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("cmdb")
class BrokerRegistrationRestrictedToXApiInfoLocationAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SUFFIX = "incorrect-x-api-info-location-rejected";

	private BrokerProperties brokerProperties;


	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@BeforeEach
	@Override
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		//save to use it in the test
		this.brokerProperties = brokerProperties;
	}

	@Test
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		"spring.profiles.active=acceptanceTests,SyncSuccessfulBackingSpaceInstanceInterceptor",
		//cf java client wire traces
		"logging.level.cloudfoundry-client.wire=debug",
//		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
		"osbcmdb.broker.rejectRequestsWithNonMatchingXApiInfoLocationHeader=true",
		"osbcmdb.broker.expectedXApiInfoLocationHeader=invalid_value_set_to_fail_catalog_fetching"
	})
	void assertServiceBrokerRegistrationFailsFromInvalidXApiLocation() throws InterruptedException {

		ClientV2Exception exception =
			assertThrows(ClientV2Exception.class, () -> {
				Hooks.onOperatorDebug(); // get human readeable reactor stack traces
				List<String> appBrokerProperties = getAppBrokerProperties(brokerProperties);
				blockingSubscribe(initializeBroker(appBrokerProperties, false));
			});
		assertThat(exception.getMessage()).contains("The service broker rejected the request. Status Code: 400 Bad " +
			"Request");

	}

}