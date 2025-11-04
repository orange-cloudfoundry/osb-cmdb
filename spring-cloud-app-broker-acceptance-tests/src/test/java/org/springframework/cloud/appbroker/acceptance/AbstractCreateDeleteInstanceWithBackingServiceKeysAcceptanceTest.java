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
abstract class AbstractCreateDeleteInstanceWithBackingServiceKeysAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

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
		"logging.level.com.orange.oss.osbcmdb.serviceinstance.ApiInfoLocationHeaderFilter=trace",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
		"osbcmdb.rejectRequestsWithNonMatchingXApiInfoLocationHeader=true"
	})
	void deployAppsAndCreateServiceKeysOnBindService() throws InterruptedException {
		// given a brokered service instance is created with some params
		Map<String, Object> parameters = Collections.singletonMap("a-key", "a-value");
		createServiceInstance(brokeredServiceInstanceName(), parameters);
		// then the brokered service instance is indeed successfully created
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("succeeded");
		//And the brokered service instance returns the same params provisionned
		Map<String, Object> brokeredServiceInstanceParams = getServiceInstanceParams(brokeredServiceInstance.getId());
		assertThat(brokeredServiceInstanceParams).containsExactlyInAnyOrderEntriesOf(parameters);

		// and a backing service instance is created in the backing service with the id as service name
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, brokeredServiceName());
		//and the backing service has the right type
		assertThat(backingServiceInstance.getService()).isEqualTo(brokeredServiceName());
		//and the backing service has metadata associated
		String backingServiceInstanceId = backingServiceInstance.getId();
		assertServiceInstanceHasAttachedNonEmptyMetadata(backingServiceInstanceId);
		//and the backing service has params plus custom param
		Map<String, Object> backingServiceParams = getServiceInstanceParams(backingServiceInstance.getId());
		assertThat(backingServiceParams).containsAllEntriesOf(parameters);
		assertCustomParams(backingServiceParams);


		//and the brokered service dashboard url, is the same as the backing service's one
		assertThat(brokeredServiceInstance.getDashboardUrl())
			.isNotEmpty()
			.isEqualTo(backingServiceInstance.getDashboardUrl());

		//and invalid get service instance requests are rejected
		assertInvalidGetServiceInstanceAreRejected(backingServiceInstanceId);

		//when a service key is created with params
		createServiceKey(getSkName(), brokeredServiceInstanceName());
		ServiceKey brokeredServiceKey = getServiceKey(isSyncBinding(), getSkName(), brokeredServiceInstanceName());

		//then a backing service key with params is created, whose name matches the brokered service binding id
		String backingServiceKeyName = brokeredServiceKey.getId();
		assertThat(listServiceKeys(backingServiceName, brokeredServiceName())).containsOnly(backingServiceKeyName);
		// and credentials from backing service key is returned in brokered service key
		assertThat(getServiceKeyCredentials(backingServiceKeyName, backingServiceName, brokeredServiceName())).isEqualTo(STATIC_CREDENTIALS);

		//when an attacker tries to forge osb request to create service binding from other tenant, it is properly
		// rejected
		assertInvalidForgedCreateServiceKeyOsbRequestsHandling(backingServiceInstance, "any-service-binding-id");

		//when a service key is deleted
		deleteServiceKey(getSkName(), brokeredServiceInstanceName());

		//then the backing service key is deleted
		assertThat(listServiceKeys(backingServiceName, brokeredServiceName())).isEmpty();

		// when the service instance is deleted
		deleteServiceInstance(brokeredServiceInstanceName());

		// then the backing service instance is deleted
		assertThat(listServiceInstances(brokeredServiceName())).doesNotContain(backingServiceName);

		//when invalid service id or service plan is passed in unprovisionning request, they are rejected
		assertInvalidServiceProvisionningRequestsAreRejected();

		//when a DSI is received while there are service keys, service keys are deleted
		if (isSyncBinding()) {
			/* Transiently skip  the test for async bindings, that failing to delete upper service instance, unless
			client accepts_incomplete:
			> Unable to deprovision service, caught:org.cloudfoundry.
				client.v2.ClientV2Exception: CF-AsyncRequired(10001): Service broker failed to delete service binding for instance 9c20a8f6-c154-463f-9e01-0ed52d9cad99: This service plan requires client support for asynchronous service operations.
			 */

				assertDeleteServiceInstanceDeletesServiceKeys();
		}
	}

	private void assertInvalidGetServiceInstanceAreRejected(String backingServiceInstanceId) {
		given(brokerFixture.serviceInstanceRequest(false))
			.when()
			.get(brokerFixture.createServiceInstanceUrl(), "an-invalid-id")
			.then()
			//Then the duplicate is ignored as expected
			.statusCode(HttpStatus.NOT_FOUND.value());

		//Backing service guid should be rejected. However, we can't assert it since the interceptor will handle the
		// GSI OSB request in place of OSB-cmdb
//		assertThatThrownBy(() -> getServiceInstanceParams(backingServiceInstanceId)).hasMessageContaining("CF-ServiceInstanceNotFound");
	}

	private void assertDeleteServiceInstanceDeletesServiceKeys() {
		// given a brokered service instance is created
		createServiceInstance(brokeredServiceInstanceName());
		// then a backing service instance is created in the backing service with the id as service name
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		String backingServiceName = brokeredServiceInstance.getId();

		//when a service key is created with params
		createServiceKey(getSkName(), brokeredServiceInstanceName());

		// when the service instance is deleted without unbinding
		int expectedStatusCode = isSyncInstance() ? HttpStatus.OK.value(): HttpStatus.ACCEPTED.value();
		given(brokerFixture.serviceInstanceRequest(!isSyncInstance()  || !isSyncBinding()))
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(),brokeredServiceInstance.getId())
			.then()
			.statusCode(expectedStatusCode);

		//noinspection StatementWithEmptyBody
		if (isSyncInstance()) {
			//and the backing service instance is deleted (and the previously associated service key)
			assertThat(listServiceInstances(brokeredServiceName())).doesNotContain(backingServiceName);
		} else {
			//Pending assertion for async. Need to loop/wait for async DSI completion
		}
	}


	private void assertInvalidServiceProvisionningRequestsAreRejected() {
		//When requesting an invalid create request with invalid plan id
		//then it returns a 400 Bad request
		given(brokerFixture.serviceInstanceRequest(SERVICE_ID, "invalid-plan-id", !isSyncInstance()))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "a-fake_id")
			.then()
			.statusCode(HttpStatus.BAD_REQUEST.value());

		//When requesting an invalid create request with invalid service definition id
		//then it returns a 400 Bad request
		given(brokerFixture.serviceInstanceRequest("invalid-service-id", PLAN_ID, !isSyncInstance()))
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "a-fake_id")
			.then()
			.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	private void assertInvalidForgedCreateServiceKeyOsbRequestsHandling(
		ServiceInstance unauthorizedBackendServiceInstance, String serviceKeyId) {
		//When requesting a concurrent request to the same broker with the same instance id, service definition,
		// plan and params
		given(brokerFixture.serviceKeyRequest())
			.when()
			.put(brokerFixture.createBindingUrl(), unauthorizedBackendServiceInstance.getId(),
				serviceKeyId)
			.then()
			//Then the duplicate is ignored as expected
			.statusCode(HttpStatus.BAD_REQUEST.value());
	}

}
