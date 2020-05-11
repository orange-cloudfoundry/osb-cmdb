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
import java.util.Map;

import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceKey;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class CreateDeleteInstanceWithBackingServiceKeysAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SK_NAME = "sk-create-service-keys";

	private static final String SUFFIX = "create-instance-with-service-keys";

	public static final Map<String, Object> STATIC_CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop" +
		"-binding-value");

	public String getSkName() {
		return SK_NAME;
	}


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
	})
	void deployAppsAndCreateServiceKeysOnBindService() throws InterruptedException {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());
		// then the brokered service instance is indeed successfully created
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("succeeded");

		// and a backing service instance is created in the backing service with the id as service name
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//and the backing service has the right type
		assertThat(backingServiceInstance.getService()).isEqualTo(brokeredServiceName());
		//and the backing service has metadata associated
		String backingServiceInstanceId = backingServiceInstance.getId();
		assertServiceInstanceHasAttachedNonEmptyMetadata(backingServiceInstanceId);

		//and the brokered service dashboard url, is the same as the backing service's one
		assertThat(brokeredServiceInstance.getDashboardUrl())
			.isNotEmpty()
			.isEqualTo(backingServiceInstance.getDashboardUrl());

		//when concurrent requests as received, they are properly handled
		assertDuplicateCreateServiceInstanceOsbRequestsHandling(brokeredServiceInstance);

		//when a service key is created with params
		createServiceKey(getSkName(), brokeredServiceInstanceName());
		ServiceKey brokeredServiceKey = getServiceKey(getSkName(), brokeredServiceInstanceName());

		//then a backing service key with params is created, whose name matches the brokered service binding id
		String backingServiceKeyName = brokeredServiceKey.getId();
		assertThat(listServiceKeys(backingServiceName, brokeredServiceName())).containsOnly(backingServiceKeyName);
		ServiceKey backingServiceKey = getServiceKey(backingServiceKeyName, backingServiceName, brokeredServiceName());
		// and credentials from backing service key is returned in brokered service key
		assertThat(backingServiceKey.getCredentials()).isEqualTo(STATIC_CREDENTIALS);

		//when a service key is deleted
		deleteServiceKey(getSkName(), brokeredServiceInstanceName());

		//then the backing service key is deleted
		assertThat(listServiceKeys(backingServiceName, brokeredServiceName())).isEmpty();

		// when the service instance is deleted
		deleteServiceInstance(brokeredServiceInstanceName());

		// and the backing service instance is deleted
		assertThat(listServiceInstances(brokeredServiceName())).doesNotContain(backingServiceName);

		//when concurrent deprovision requests as received, they are properly handled
		assertDuplicateDeleteServiceInstanceOsbRequestsHandling(brokeredServiceInstance);

		//when invalid service id or service plan is passed, they are rejected
		assertInvalidServiceProvisionningRequestsAreRejected();
	}

	private void assertInvalidServiceProvisionningRequestsAreRejected() {
		//When requesting an invalid create request with invalid plan id
		//then it returns a 400 Bad request
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, "invalid-plan-id"))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "a-fake_id")
			.then()
			.statusCode(HttpStatus.BAD_REQUEST.value());

		//When requesting an invalid create request with invalid service definition id
		//then it returns a 400 Bad request
		given(brokerFixture.serviceInstanceRequest("invalid-service-id", PLAN_ID))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "a-fake_id")
			.then()
			.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	private void assertDuplicateCreateServiceInstanceOsbRequestsHandling(ServiceInstance brokeredServiceInstance) {
		//When requesting a concurrent request to the same broker with the same instance id, service definition,
		// plan and params
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), brokeredServiceInstance.getId())
			.then()
			//Then the duplicate is ignored as expected
			.statusCode(HttpStatus.OK.value());
	}

	private void assertDuplicateDeleteServiceInstanceOsbRequestsHandling(ServiceInstance brokeredServiceInstance) {
		//When requesting a concurrent deprovision request to the same broker with the same instance id, service
		// definition,
		// plan and params
		// when the service instance is deleted
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(),brokeredServiceInstance.getId())
			.then()
			.statusCode(HttpStatus.GONE.value());
	}

}
