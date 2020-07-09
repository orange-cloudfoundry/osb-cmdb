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

import com.orange.oss.osbcmdb.serviceinstance.MaintenanceInfoFormatterService;
import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncSuccessfulBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncTimeoutCreateBackingSpaceInstanceInterceptor;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.servicebroker.autoconfigure.web.MaintenanceInfo;
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
	void contextFailsWithOnlyAcceptanceProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void syncFailingCreateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncFailedCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncFailedCreateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void syncSuccessfulBackingSpaceInstanceInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncSuccessfulBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncSuccessfulBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void syncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor.class);
			});
	}

	@Test
	void syncTimeoutCreateBackingSpaceInstanceInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncTimeoutCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncTimeoutCreateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void aSyncStalledCreateBackingSpaceInstanceInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncStalledCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(ASyncStalledCreateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void aSyncStalledDeleteBackingSpaceInstanceInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncStalledDeleteBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(ASyncStalledDeleteBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void aSyncStalledUpdateBackingSpaceInstanceInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncStalledUpdateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(ASyncStalledUpdateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void syncFailingUpdateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncFailedUpdateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncFailedUpdateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncFailingUpdateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,AsyncFailedUpdateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(AsyncFailedUpdateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncSuccessfulUpdateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncSuccessfulCreateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void syncFailingDeleteInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,SyncFailedDeleteBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(SyncFailedDeleteBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncFailingDeleteInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,AsyncFailedDeleteBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(ServiceInstanceInterceptor.class);
				assertThat(context)
					.getBean(ServiceInstanceInterceptor.class)
					.isInstanceOf(AsyncFailedDeleteBackingSpaceInstanceInterceptor.class);
			});
	}

	@Test
	void asyncFailingCreateInterceptorIsCreatedWithAssociatedProfile() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=acceptanceTests,ASyncFailedCreateBackingSpaceInstanceInterceptor"
			)
			.withPropertyValues(requiredProperties())
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
			.withPropertyValues(requiredProperties())
			.run((context) -> assertThat(context).doesNotHaveBean(ServiceInstanceInterceptor.class));
	}

	@Test
	@DisplayName("custom metadata is enabled by default")
	void customMetadataAsParamFlagDefaultsToTrue() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=cloud"
			)
			.withPropertyValues(requiredProperties())
			.run((context) -> {
				assertThat(context).hasSingleBean(OsbCmdbBrokerProperties.class);
				OsbCmdbBrokerProperties osbCmdbBrokerProperties = context.getBean(OsbCmdbBrokerProperties.class);
				assertThat(osbCmdbBrokerProperties.isPropagateMetadataAsCustomParam()).isTrue();
			});
	}
	@Test
	@DisplayName("custom metadata option can be opted out")
	void customMetadataAsParamFlagOptOut() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=cloud"
			)
			.withPropertyValues(requiredProperties())
			.withPropertyValues(new String[]{"osbcmdb.broker.propagateMetadataAsCustomParam=false"})
			.run((context) -> {
				assertThat(context).hasSingleBean(OsbCmdbBrokerProperties.class);
				OsbCmdbBrokerProperties osbCmdbBrokerProperties = context.getBean(OsbCmdbBrokerProperties.class);
				assertThat(osbCmdbBrokerProperties.isPropagateMetadataAsCustomParam()).isFalse();
			});
	}

	@Test
	@DisplayName("maintenanceInfo can be opted in")
	void maintenanceInfoOptIn() {
		this.contextRunner
			.withPropertyValues(
				"spring.profiles.active=cloud"
			)
			.withPropertyValues(requiredProperties())
			.withPropertyValues(new String[]{
				"osbcmdb.maintenance_info.version=1.0.0",
				"osbcmdb.maintenance_info.description=a description"})
			.run((context) -> {
				assertThat(context).hasSingleBean(MaintenanceInfo.class);
				MaintenanceInfo maintenanceInfo = context.getBean(MaintenanceInfo.class);
				assertThat(maintenanceInfo.getVersion()).isEqualTo("1.0.0");
				assertThat(maintenanceInfo.getDescription()).isEqualTo("a description");
				assertThat(context).hasSingleBean(MaintenanceInfoFormatterService.class);
				org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo mappedMaintenanceInfo = context.getBean(MaintenanceInfoFormatterService.class).getOsbCmdbMaintenanceInfo();
				assertThat(mappedMaintenanceInfo.getVersion()).isEqualTo("1.0.0");
				assertThat(mappedMaintenanceInfo.getDescription()).isEqualTo("a description");

			});
	}

	private String[] requiredProperties() {
		return new String[] {
			//cloudfoundry properties
			"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.local",
			"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
			"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
			"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
			"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
			"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
		};
	}

}
