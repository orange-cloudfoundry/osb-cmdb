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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class CreateInstanceWithBackingServiceSyncTimeoutAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SUFFIX = "create-instance-with-sync-backing-timeout";

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	private static final Logger LOG = LoggerFactory.getLogger(CreateInstanceWithBackingServiceSyncTimeoutAcceptanceTest.class);

	@Test
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		// control backing service response: have it sync timeout after 2 mins
		"spring.profiles.active=acceptanceTests,SyncTimeoutCreateBackingSpaceInstanceInterceptor",
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
	void aFailedBackingService_is_reported_as_a_last_operation_state_failed() {
		// given a brokered service instance is created
		// and a backing service is asked to hang for 2 mins and trigger CF 60s sync timeout
		String responseString = given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "a-random-service-instance-guid")
			//then it fails after CF API timeout (60s) with
			.then()
			.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.extract()
			.asString();
		assertThat(responseString).contains("CF-HttpClientTimeout"); //response include some diagnostics
		assertThat(responseString).doesNotContain("https"); //but response is redacted
	}

}
