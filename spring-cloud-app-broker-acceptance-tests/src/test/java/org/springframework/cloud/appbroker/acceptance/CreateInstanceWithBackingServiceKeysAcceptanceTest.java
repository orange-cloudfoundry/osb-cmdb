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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithBackingServiceKeysAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-create-service-keys";
	private static final String SK_NAME = "sk-create-service-keys";

	private static final String SUFFIX = "create-instance-with-service-keys";

	private static final String BROKERED_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;

	public static final Map<String, Object> STATIC_CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop" +
		"-binding-value");


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
		"spring.profiles.active=acceptanceTests",
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
	void deployAppsAndCreateServiceKeyssOnBindService() {
		// given a brokered service instance is created
		createServiceInstance(SI_NAME);
		// then the brokered service instance is indeed created
		ServiceInstance brokeredServiceInstance = getServiceInstance(SI_NAME);

		// and a backing service instance is created in the backing service with the id as service name
		String backingServiceName = brokeredServiceInstance.getId();
		ServiceInstance backingServiceInstance = getServiceInstance(backingServiceName, BROKERED_SERVICE_NAME);
		//and the backing service has the right type
		assertThat(backingServiceInstance.getService()).isEqualTo(BROKERED_SERVICE_NAME);

		//when a service key is created with params
		createServiceKey(SK_NAME, SI_NAME);
		ServiceKey brokeredServiceKey = getServiceKey(SK_NAME, SI_NAME);

		//then a backing service key with params is created, whose name matches the brokered service binding id
		String backingServiceKeyName = brokeredServiceKey.getId();
		assertThat(listServiceKeys(backingServiceName, BROKERED_SERVICE_NAME)).containsOnly(backingServiceKeyName);
		ServiceKey backingServiceKey = getServiceKey(backingServiceKeyName, backingServiceName, BROKERED_SERVICE_NAME);
		// and credentials from backing service key is returned in brokered service key
		assertThat(backingServiceKey.getCredentials()).isEqualTo(STATIC_CREDENTIALS);

		//when a service key is deleted
		deleteServiceKey(SK_NAME, SI_NAME);

		//then the backing service key is deleted
		assertThat(listServiceKeys(backingServiceName, BROKERED_SERVICE_NAME)).isEmpty();

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service instance is deleted
		assertThat(listServiceInstances(BROKERED_SERVICE_NAME)).doesNotContain(backingServiceName);
	}

}
