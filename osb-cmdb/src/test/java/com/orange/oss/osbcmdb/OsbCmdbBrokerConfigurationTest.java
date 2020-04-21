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

import com.orange.oss.osbcmdb.testfixtures.ASyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedUpdateBackingSpaceInstanceInterceptor;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OsbCmdbBrokerConfigurationTest {

	//See https://docs.spring.io/spring-boot/docs/2.1.12.RELEASE/reference/html/boot-features-developing-auto-configuration.html#boot-features-test-autoconfig
	ConditionEvaluationReportLoggingListener conditionEvaluationReportLoggingListener = new ConditionEvaluationReportLoggingListener(LogLevel.INFO);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withInitializer(conditionEvaluationReportLoggingListener)
		.withConfiguration(AutoConfigurations.of(OsbCmdbBrokerConfiguration.class))
		.withUserConfiguration(InjectMockCloudFoundryBeansConfiguration.class);


	@Configuration
	public static class InjectMockCloudFoundryBeansConfiguration {

		@Bean
		public CloudFoundryClient cloudFoundryClient() {
			return Mockito.mock(CloudFoundryClient.class, Mockito.RETURNS_SMART_NULLS);
		}

		@Bean
		public CloudFoundryOperations cloudFoundryOperations() {
			return Mockito.mock(CloudFoundryOperations.class, Mockito.RETURNS_SMART_NULLS);
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
				"spring.profiles.active=acceptanceTests"
			)
			.withPropertyValues(cloudFoundryDeploymentProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
			});
	}

	@Test
	void syncFailingCreateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncFailedCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(cloudFoundryDeploymentProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncFailedCreateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void syncFailingUpdateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncFailedUpdateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(cloudFoundryDeploymentProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncFailedUpdateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncFailingCreateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncFailedCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(cloudFoundryDeploymentProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(ASyncFailedCreateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void noInterceptorIsCreatedWithoutAcceptanceProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=cloud"
			)
			.withPropertyValues(cloudFoundryDeploymentProperties())
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ServiceInstanceInterceptor.class);
			});
	}

	private String[] cloudFoundryDeploymentProperties() {
		return new String[] {"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.local",
			"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
			"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
			"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
			"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
			"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"};
	}

}
