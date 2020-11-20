/*
 * Copyright 2002-2019 the original author or authors.
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

package com.orange.oss.osbcmdb.catalog;

import java.util.List;

import com.orange.oss.osbcmdb.CloudFoundryTargetProperties;
import com.orange.oss.osbcmdb.serviceinstance.MaintenanceInfoFormatterService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicCatalogServiceAutoConfigurationTest {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCatalogServiceAutoConfigurationTest.class);

	private ConditionEvaluationReportLoggingListener initializer;

	@BeforeEach
	void setUp() {
		initializer = new ConditionEvaluationReportLoggingListener();
	}

	@Test
	void dynamicServiceLoadWhenOptInProperty() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(this.initializer)
			.withConfiguration(AutoConfigurations.of(
				ValidStubbedDynamicServiceConfig.class,
				DynamicCatalogServiceAutoConfiguration.class,
				MockedMaintenanceInfoFormatterServiceConfig.class
			))
			.withPropertyValues(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		contextRunner.run(context -> {
			Catalog catalog = context.getBean(Catalog.class);
			assertThat(catalog.getServiceDefinitions()).isNotEmpty();

			assertThat(context).hasSingleBean(Catalog.class);
		});
	}

	@Test
	void serviceDefinitionMapperPropertiesAreProperlyLoaded() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(this.initializer)
			.withConfiguration(AutoConfigurations.of(
				ValidStubbedDynamicServiceConfig.class,
				DynamicCatalogServiceAutoConfiguration.class,
				MockedMaintenanceInfoFormatterServiceConfig.class
			))
			.withPropertyValues(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true",
				ServiceDefinitionMapperProperties.PROPERTY_PREFIX
					+ServiceDefinitionMapperProperties.SUFFIX_PROPERTY_KEY+ "=suffix")
		;
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ServiceDefinitionMapperProperties.class);
			ServiceDefinitionMapperProperties serviceDefinitionMapperProperties
				= context.getBean(ServiceDefinitionMapperProperties.class);
			assertThat(serviceDefinitionMapperProperties.getSuffix()).isEqualTo("suffix");
		});
	}

	@Test
	void catalogFetchingFailuresTriggersContextFailure() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withInitializer(this.initializer)
			.withConfiguration(AutoConfigurations.of(
				DynamicCatalogServiceAutoConfiguration.class,
				ThrowingExceptionServiceDefinitionAnswerAutoConfig.class,
				MockedMaintenanceInfoFormatterServiceConfig.class))
			.withPropertyValues(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
				.hasMessageContaining("Injected exception while fetching catalog");
		});
	}

	@Test
	void emptyCatalogFetchedTriggersContextFailure() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withInitializer(this.initializer)
			.withConfiguration(AutoConfigurations.of(
				EmptyStubbedServiceDefinitionAutoConfig.class,
				DynamicCatalogServiceAutoConfiguration.class,
				MockedDynamicCatalogDependenciesAutoConfiguration.class))
			.withPropertyValues(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
				.hasMessageContaining("Unexpected empty marketplace dynamically fetched");
		});
	}



	@Configuration
	static class MockedDynamicCatalogDependenciesAutoConfiguration {
		@Bean
		CloudFoundryOperations operations() {
			return mock(CloudFoundryOperations.class);
		}
		@Bean
		CloudFoundryClient cloudFoundryClient() {
			return mock(CloudFoundryClient.class);
		}
		@Bean
		CloudFoundryTargetProperties targetProperties () {
			return mock(CloudFoundryTargetProperties.class);
		}
		@Bean
		MaintenanceInfoFormatterService maintenanceInfoFormatterService() { return mock(MaintenanceInfoFormatterService.class, RETURNS_SMART_NULLS); }
	}

	static abstract class AbtractStubbedDynamicServiceConfig {
		@Bean
		DynamicCatalogService stubbedDynamicCatalogService() {
			DynamicCatalogService dynamicCatalogService = mock(DynamicCatalogService.class);
			when(dynamicCatalogService.fetchServiceDefinitions()).thenReturn(this.serviceDefinitionsAnswer());
			return dynamicCatalogService;
		}

		protected abstract List<ServiceDefinition> serviceDefinitionsAnswer();

	}

	@Configuration
	static class MockedMaintenanceInfoFormatterServiceConfig {
		@Bean
		public MaintenanceInfoFormatterService maintenanceInfoFormatterService() {
			return new MaintenanceInfoFormatterService(null);
		}
	}

	/**
	 * Configuration designed to run before {@link DynamicCatalogServiceAutoConfiguration#dynamicCatalogService(CloudFoundryOperations, CloudFoundryClient, CloudFoundryTargetProperties, ServiceDefinitionMapper)}
	 * and inject a stubbed implementation instead, returning a valid catalog
	 */
	@Configuration
	@AutoConfigureBefore(value = DynamicCatalogServiceAutoConfiguration.class)
	static class ValidStubbedDynamicServiceConfig extends AbtractStubbedDynamicServiceConfig {
		protected List<ServiceDefinition> serviceDefinitionsAnswer() {
			String serviceName = "serviceName";
			String planName = "planName";
			return asList(ServiceDefinition
				.builder()
				.id(serviceName + "-id")
				.name(serviceName)
				.plans(Plan.builder()
					.id(planName + "-id")
					.name(planName)
					.build())
				.build()
			);
		}
	}

	/**
	 * Configuration designed to run before {@link DynamicCatalogServiceAutoConfiguration#dynamicCatalogService(CloudFoundryOperations, CloudFoundryClient, CloudFoundryTargetProperties, ServiceDefinitionMapper)}
	 * and inject a stubbed implementation instead, returning an invalid catalog
	 */
	@Configuration
	@AutoConfigureBefore(value = DynamicCatalogServiceAutoConfiguration.class)
	static class EmptyStubbedServiceDefinitionAutoConfig extends AbtractStubbedDynamicServiceConfig {
		protected List<ServiceDefinition> serviceDefinitionsAnswer() {
			return emptyList();
		}
	}

	@Configuration
	@AutoConfigureBefore(value = DynamicCatalogServiceAutoConfiguration.class)
	static class ThrowingExceptionServiceDefinitionAnswerAutoConfig  {
		@Bean
		DynamicCatalogService dynamicCatalogService() {
			DynamicCatalogService dynamicCatalogService = mock(DynamicCatalogService.class);
			when(dynamicCatalogService.fetchServiceDefinitions()).thenThrow(new RuntimeException("Injected exception " +
				"while fetching catalog "));
			return dynamicCatalogService;
		}
	}


}