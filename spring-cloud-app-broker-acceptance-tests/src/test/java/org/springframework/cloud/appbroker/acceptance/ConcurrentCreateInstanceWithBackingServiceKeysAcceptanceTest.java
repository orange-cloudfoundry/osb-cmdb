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
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("cmdb")
@Tag("k8s") //supports K8S
class ConcurrentCreateInstanceWithBackingServiceKeysAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SUFFIX = "concurrent-create-instance-with-service-keys";

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
		"spring.profiles.active=acceptanceTests,ASyncStalledCreateBackingSpaceInstanceInterceptor",
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
	void deployAppsAndCreateServiceKeysOnBindService() {
		// given an async backing service is configured to stall on OSB provision request
		// given a brokered service instance is requested
		ServiceInstance brokeredServiceInstance = createServiceInstanceWithoutAsserts(appServiceName(), PLAN_NAME,
			brokeredServiceInstanceName(), emptyMap(), Duration.ofSeconds(5));

		// then the brokered service instance is returned and remains stalled "in progress"
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("in progress");

		//And the brokered service instance service instance fetch endpoint returns a 422 status when provisionning is
		// in progress
		ClientV2Exception exception = assertThrows(ClientV2Exception.class, () -> {
			getServiceInstanceParams(brokeredServiceInstance.getId());
		});
		assertThat(exception.toString()).contains("CF-AsyncServiceInstanceOperationInProgress");

		//When requesting a concurrent request to the same broker with the same instance id, service definition,
		// plan and params
		//Then the duplicate is ignored as expected
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		//When requesting a concurrent request to the same broker with the different plan
		// then get a 409
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN2_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.CONFLICT.value());

		//When requesting a concurrent request to the same broker with the different service definition
		// then get a 409

		//noinspection UnnecessaryLocalVariable
		String mismatchingServiceId = BACKING_SERVICE_ID; //The id of SCAB backing service that we don't use
		given(brokerFixture.serviceInstanceRequest(mismatchingServiceId, BACKING_SERVICE_PLAN_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.CONFLICT.value());

		// given a set of parameters
		Map<String, Object> params = new HashMap<>();
		params.put("parameter1", "value1");
		params.put("parameter2", true);
		params.put("count", 2);

		//When requesting a concurrent request to the same broker with the different instance id, service
		// definition,
		// plan and params
		// then get a 409
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, PLAN_ID, params))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			//.statusCode(HttpStatus.CONFLICT.value());
			.statusCode(HttpStatus.ACCEPTED.value()); //Not yet implemented, waiting for GSIP prioritization
	}

	@AfterEach
	@Override
	public void tearDown(TestInfo testInfo) {
		//Avoid stalled service instance from failing service broker deletion
		purgeServiceInstance(brokeredServiceInstanceName());
		super.tearDown(testInfo);
	}

}
