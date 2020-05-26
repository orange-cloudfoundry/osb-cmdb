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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("cmdb")
class CreateInstanceCustomParamAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final String SK_NAME = "sk-create-service-keys";

	private static final String SUFFIX = "create-instance-with-custom-params";

	public static final Map<String, Object> STATIC_CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop" +
		"-binding-value");

	public static final String X_OSB_CMDB_CUSTOM_KEY_NAME = "x-osb-cmdb";

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
		"osbcmdb.broker.hideMetadataCustomParamInGetServiceInstanceEndpoint=false",
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
	void deployAppsAndCreateServiceKeysOnBindService() {
		// given a brokered service instance is created with some params
		Map<String, Object> parameters = Collections.singletonMap("a-key", "a-value");
		createServiceInstance(brokeredServiceInstanceName(), parameters);
		// then the brokered service instance is indeed successfully created
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		assertThat(brokeredServiceInstance.getStatus()).isEqualTo("succeeded");
		//And the brokered service instance returns the same params provisionned
		Map<String, Object> brokeredServiceInstanceParams = getServiceInstanceParams(brokeredServiceInstance.getId());
		assertThat(brokeredServiceInstanceParams).containsAllEntriesOf(parameters);
		//And a custom param propagates metadata to brokered service
		assertThat(brokeredServiceInstanceParams).containsKey(X_OSB_CMDB_CUSTOM_KEY_NAME);
		Map<String, Object> customParamValue = (Map<String, Object>) brokeredServiceInstanceParams
			.get(X_OSB_CMDB_CUSTOM_KEY_NAME);
		assertThat(customParamValue).containsKey("annotations");
		assertThat(customParamValue).containsKey("labels");
		Map<String, Object> annotations = (Map<String, Object>) customParamValue.get("annotations");
		Map<String, Object> labels = (Map<String, Object>) customParamValue.get("labels");
		assertThat(annotations).isNotEmpty();
		assertThat(labels).isNotEmpty();
	}

	@AfterEach
	public void tearDown(TestInfo testInfo) {
		deleteServiceInstance(brokeredServiceInstanceName());
		super.tearDown(testInfo);
	}

}
