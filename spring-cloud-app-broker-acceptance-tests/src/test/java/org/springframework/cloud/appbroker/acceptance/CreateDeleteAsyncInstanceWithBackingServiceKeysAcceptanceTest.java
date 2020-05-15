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

/**
 * Checks support for an async backing service in create and delete.
 * Async service keys are not yet supported
 */
@Tag("cmdb")
class CreateDeleteAsyncInstanceWithBackingServiceKeysAcceptanceTest extends
	CreateDeleteInstanceWithBackingServiceKeysAcceptanceTest {

	private static final String SK_NAME = "sk-async-create-service-keys";

	private static final String SUFFIX = "create-async-instance-with-service-keys";

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@Override
	public String getSkName() { return SK_NAME; } // avoid race conditions among concurrent subclasses tests

	@Test
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		"spring.profiles.active=acceptanceTests,AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor",
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
		//Same code, just different interceptor in annotation
		super.deployAppsAndCreateServiceKeysOnBindService();
	}

}
