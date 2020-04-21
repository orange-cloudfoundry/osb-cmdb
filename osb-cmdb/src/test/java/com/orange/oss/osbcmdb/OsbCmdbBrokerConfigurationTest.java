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

package com.orange.oss.osbcmdb;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OsbCmdbBrokerConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OsbCmdbBrokerConfiguration.class))
		.withUserConfiguration(InjectMockCloudFoundryBeansConfiguration.class)
		.withSystemProperties("debug:true");

	@Configuration
	public static class InjectMockCloudFoundryBeansConfiguration {

		@Bean
		public CloudFoundryOperations cloudFoundryOperations() {
			return Mockito.mock(CloudFoundryOperations.class, Mockito.RETURNS_SMART_NULLS);
		}
		@Bean
		public CloudFoundryClient cloudFoundryClient() {
			return Mockito.mock(CloudFoundryClient.class, Mockito.RETURNS_SMART_NULLS);
		}
		@Bean
		public CloudFoundryTargetProperties targetProperties() {
			return Mockito.mock(CloudFoundryTargetProperties.class, Mockito.RETURNS_SMART_NULLS);
		}
	}

	@Test
	void singleInterceptorIsCreatedWithAcceptanceProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
			});
	}

	@Test
	void syncFailingInterceptorIsCreatedWithAcceptanceProfileAndFailingProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncFailedBackingSpaceInstanceInterceptor",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(ASyncFailedBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void noInterceptorIsCreatedWithoutAcceptanceProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=cloud",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
			)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ServiceInstanceInterceptor.class);
			});
	}


}
