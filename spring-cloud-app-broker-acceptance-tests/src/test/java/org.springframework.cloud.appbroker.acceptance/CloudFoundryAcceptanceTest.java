/*
 * Copyright 2016-2018. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {CloudFoundryClientConfiguration.class, CloudFoundryService.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
@EnableConfigurationProperties(AcceptanceTestProperties.class)
class CloudFoundryAcceptanceTest {

	private static final String SAMPLE_BROKER_APP_NAME = "sample-broker";
	private static final String SERVICE_BROKER_NAME = "sample-broker-name";
	private static final String SERVICE_NAME = "example";
	private static final String PLAN_NAME = "standard";
	private static final String SERVICE_INSTANCE_NAME = "my-service";

	@Autowired
	private CloudFoundryService cloudFoundryService;

	@Autowired
	private AcceptanceTestProperties acceptanceTestProperties;

	@BeforeEach
	void setUp(BrokerProperties brokerProperties) {
		blockingSubscribe(initializeBroker(brokerProperties.getProperties()));
	}

	@AfterEach
	void tearDown() {
		blockingSubscribe(cleanup());
	}

	private Mono<Void> initializeBroker(String... backingAppProperties) {
		return cloudFoundryService
			.getOrCreateDefaultOrganization()
			.then(cloudFoundryService.getOrCreateDefaultSpace())
			.then(cloudFoundryService.pushBrokerApp(SAMPLE_BROKER_APP_NAME, getSampleBrokerAppPath(), backingAppProperties))
			.then(cloudFoundryService.createServiceBroker(SERVICE_BROKER_NAME, SAMPLE_BROKER_APP_NAME))
			.then(cloudFoundryService.enableServiceBrokerAccess(SERVICE_NAME));
	}

	private Mono<Void> cleanup() {
		return cloudFoundryService
			.deleteServiceInstance(SERVICE_INSTANCE_NAME)
			.then(cloudFoundryService.deleteServiceBroker(SERVICE_BROKER_NAME))
			.then(cloudFoundryService.deleteApp(SAMPLE_BROKER_APP_NAME));
	}

	void setupServiceBrokerForService(String serviceName) {
		String[] properties = new String[] {
			"spring.cloud.openservicebroker.catalog.services[0].id=" + serviceName,
			"spring.cloud.openservicebroker.catalog.services[0].name=" + serviceName,
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=" + serviceName,

			"spring.cloud.appbroker.services[0].service-name=" + serviceName,
			"spring.cloud.appbroker.services[0].plan-name=standard",
			"spring.cloud.appbroker.services[0].apps[0].name=app-" + serviceName,
			"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar"};
		blockingSubscribe(
			cloudFoundryService.pushBrokerApp(serviceName, getSampleBrokerAppPath(), properties)
							   .then(cloudFoundryService.createServiceBroker(serviceName, serviceName))
							   .then(cloudFoundryService.enableServiceBrokerAccess(serviceName)));
	}

	void deleteServiceBrokerForService(String serviceName) {
		blockingSubscribe(
			cloudFoundryService.deleteServiceBroker(serviceName)
							   .then(cloudFoundryService.deleteApp(serviceName)));
	}

	void createServiceInstance() {
		createServiceInstanceWithParameters(Collections.emptyMap());
	}

	void createServiceInstance(String planName,
							   String serviceName,
							   String serviceInstanceName,
							   Map<String, Object> parameters) {
		blockingSubscribe(cloudFoundryService
			.createServiceInstance(
				planName,
				serviceName,
				serviceInstanceName,
				parameters));
	}

	void createServiceInstanceWithParameters(Map<String, Object> parameters) {
		createServiceInstance(PLAN_NAME, SERVICE_NAME, SERVICE_INSTANCE_NAME, parameters);
	}

	void updateServiceInstance(Map<String, Object> parameters) {
		blockingSubscribe(cloudFoundryService.updateServiceInstance(SERVICE_INSTANCE_NAME, parameters));
	}

	void deleteServiceInstance() {
		deleteServiceInstance(SERVICE_INSTANCE_NAME);
	}

	void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}

	Optional<ServiceInstanceSummary> getServiceInstance() {
		return getServiceInstanceMono().blockOptional();
	}

	Optional<ServiceInstanceSummary> getServiceInstance(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).blockOptional();
	}

	Mono<ServiceInstanceSummary> getServiceInstanceMono() {
		return getServiceInstanceMono(SERVICE_INSTANCE_NAME);
	}

	private Mono<ServiceInstanceSummary> getServiceInstanceMono(String serviceInstanceName) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	Optional<ApplicationSummary> getApplicationSummaryByName(String appName) {
		return cloudFoundryService
			.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	Optional<ApplicationSummary> getApplicationSummaryByNameAndSpace(String appName, String space) {
		return cloudFoundryService.getApplicationSummaryByNameAndSpace(appName, space).blockOptional();
	}

	ApplicationEnvironments getApplicationEnvironmentByName(String appName) {
		return cloudFoundryService.getApplicationEnvironmentByAppName(appName).block();
	}

	ApplicationEnvironments getApplicationEnvironmentByNameAndSpace(String appName, String space) {
		return cloudFoundryService.getApplicationEnvironmentByAppNameAndSpace(appName, space).block();
	}

	List<String> getSpaces() {
		return cloudFoundryService.getSpaces().block();
	}

	private Path getSampleBrokerAppPath() {
		return Paths.get(acceptanceTestProperties.getSampleBrokerAppPath(), "");
	}

	private <T> void blockingSubscribe(Mono<? super T> publisher) {
		CountDownLatch latch = new CountDownLatch(1);
		publisher.subscribe(System.out::println, t -> {
			t.printStackTrace();
			latch.countDown();
		}, latch::countDown);
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}