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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.cloudfoundry.util.DelayTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryProperties;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.cloud.appbroker.acceptance.fixtures.uaa.UaaService;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.APP_BROKER_CLIENT_AUTHORITIES;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.APP_BROKER_CLIENT_SECRET;

@SpringBootTest(classes = {
	CloudFoundryClientConfiguration.class,
	CloudFoundryService.class,
	UaaService.class,
	HealthListener.class,
	RestTemplate.class
})
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
@EnableConfigurationProperties(AcceptanceTestProperties.class)
abstract class CloudFoundryAcceptanceTest {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryAcceptanceTest.class);

	protected static final String BACKING_SERVICE_PLAN_ID = UUID.randomUUID().toString();

	protected static final String SERVICE_ID = UUID.randomUUID().toString();

	protected static final String PLAN_ID = UUID.randomUUID().toString();
	protected static final String PLAN2_ID = UUID.randomUUID().toString();

	protected static final String BACKING_SERVICE_ID = UUID.randomUUID().toString();

	protected static final String PLAN_NAME = "standard";
	protected static final String PLAN2_NAME = "standard2";

	protected static final String BACKING_APP_PATH = "classpath:backing-app.jar";

	@Autowired
	protected CloudFoundryService cloudFoundryService;

	@Autowired
	private UaaService uaaService;

	@Autowired
	protected CloudFoundryProperties cloudFoundryProperties;


	@Autowired
	private AcceptanceTestProperties acceptanceTestProperties;

	private final WebClient webClient = getSslIgnoringWebClient();

	protected void assertServiceInstanceHasAttachedNonEmptyMetadata(String backingServiceInstanceId) {
		List<ServiceInstanceResource> matchingListedSIs = listServiceInstanceMetadataByLabel(
			"backing_service_instance_guid=" + backingServiceInstanceId)
			.filter(sir -> backingServiceInstanceId.equals(sir.getId()))
			.collectList().block();
		assertThat(matchingListedSIs)
			.withFailMessage("expecting single matching backing service from labels" + matchingListedSIs)
			.hasSize(1);
		//noinspection ConstantConditions
		Metadata metadata = matchingListedSIs.get(0).getMetadata();
		assertThat(metadata.getLabels()).isNotEmpty();
		assertThat(metadata.getAnnotations()).isNotEmpty();
	}

	protected ServiceInstanceEntity getServiceInstanceEntity(String serviceInstanceId) {
		return cloudFoundryService.getServiceInstanceEntity(serviceInstanceId).block();
	}

	protected ServiceInstance pollServiceInstanceUntilNotInProgress(String serviceInstanceName,
		int maxPollDurationMs)
		throws InterruptedException {
		ServiceInstance brokeredServiceInstance;
		int retry=0;
		long pollStartTime = currentTimeMillis();
		do {
			brokeredServiceInstance = getServiceInstance(serviceInstanceName);
			if (retry >0) {
				LOG.debug("Sleeping {}s in retry {}", 5, retry);
				//noinspection BusyWait
				Thread.sleep(5*1000);
			}
			retry++;
		} while (
			brokeredServiceInstance.getStatus().equals("in progress") &&
			timehasElapsedLessThan(maxPollDurationMs, pollStartTime)
		);
		assertThat(brokeredServiceInstance.getStatus())
			.withFailMessage("still in progress after retrying " + retry + " times and " + (currentTimeMillis() - pollStartTime)/1000 + " seconds")
			.isNotEqualTo("in progress");
		return brokeredServiceInstance;
	}

	protected abstract String testSuffix();

	protected abstract String appServiceName();

	protected abstract String backingServiceName();

	protected String testBrokerAppName() {
		return "test-broker-app-" + testSuffix();
	}

	protected String serviceBrokerName() {
		return "test-broker-" + testSuffix();
	}

	private String brokerClientId() {
		return appServiceName();
	}

	@BeforeEach
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		try {
			Hooks.onOperatorDebug(); // get human readeable reactor stack traces
			List<String> appBrokerProperties = getAppBrokerProperties(brokerProperties);
			blockingSubscribe(initializeBroker(appBrokerProperties, true));
		}
		finally {
			//This is useful to set a debugger breakpoint
			LOG.debug("broker initialization completed");
		}
	}

	void setUpForBrokerUpdate(BrokerProperties brokerProperties) {
		List<String> appBrokerProperties = getAppBrokerProperties(brokerProperties);
		blockingSubscribe(updateBroker(appBrokerProperties));
	}

	protected List<String> getAppBrokerProperties(BrokerProperties brokerProperties) {
		String[] openServiceBrokerProperties = {
			"spring.cloud.openservicebroker.catalog.services[0].id=" + SERVICE_ID,
			"spring.cloud.openservicebroker.catalog.services[0].name=" + appServiceName(),
			"spring.cloud.openservicebroker.catalog.services[0].description=An osb-cmdb service that deploys a " +
				"backing service",
			"spring.cloud.openservicebroker.catalog.services[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plan_updateable=true",
			"spring.cloud.openservicebroker.catalog.services[0].allow_context_updates=true",
			"spring.cloud.openservicebroker.catalog.services[0].instances_retrievable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=" + PLAN_ID,
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].name=" + PLAN_NAME,
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].description=A simple plan",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].free=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[1].id=" + PLAN2_ID,
			"spring.cloud.openservicebroker.catalog.services[0].plans[1].name=" + PLAN2_NAME,
			"spring.cloud.openservicebroker.catalog.services[0].plans[1].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[1].description=A 2nd simple plan",
			"spring.cloud.openservicebroker.catalog.services[0].plans[1].free=true",
		"spring.cloud.openservicebroker.catalog.services[1].id=" + BACKING_SERVICE_ID,
			"spring.cloud.openservicebroker.catalog.services[1].name=" + backingServiceName(),
			"spring.cloud.openservicebroker.catalog.services[1].description=A backing service",
			"spring.cloud.openservicebroker.catalog.services[1].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].id=" + BACKING_SERVICE_PLAN_ID,
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].name=standard",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].description=A simple plan",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].free=true"
		};

		List<String> appBrokerProperties = new ArrayList<>();
		appBrokerProperties.addAll(Arrays.asList(openServiceBrokerProperties));
		appBrokerProperties.addAll(brokerProperties.getProperties());
		LOG.debug("Configuring broker with properties {}", appBrokerProperties);
		return appBrokerProperties;
	}

	@BeforeEach
	void configureJsonPath() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jacksonJsonProvider = new JacksonJsonProvider();

			private final MappingProvider jacksonMappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jacksonJsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return jacksonMappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	@AfterEach
	public void tearDown(TestInfo testInfo) {
		cloudFoundryService.logAndVerifyRecentAppLogs(testBrokerAppName(), true).block();

		cleanUpDefaultSpace();
	}

	protected void cleanUpDefaultSpace() {
		blockingSubscribe(cloudFoundryService.getOrCreateDefaultOrganization()
			.map(OrganizationSummary::getId)
			.flatMap(orgId -> cloudFoundryService.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap(spaceId -> cleanup(orgId, spaceId)))
			.onErrorResume(e -> Mono.empty())
			.doOnRequest(l -> LOG.debug("START cleaning up org and space"))
			.doOnSuccess(l -> LOG.debug("FINISHED cleaning up org and space"))
		);
	}

	protected Mono<Void> initializeBroker(List<String> appBrokerProperties, boolean ignoreBrokerRegistrationErrors) {
		return cloudFoundryService
			.getOrCreateDefaultOrganization()
			.map(OrganizationSummary::getId)
			.flatMap(orgId -> cloudFoundryService
				.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap(spaceId -> cleanup(orgId, spaceId)
					.then(uaaService.createClient(
						brokerClientId(),
						APP_BROKER_CLIENT_SECRET,
						APP_BROKER_CLIENT_AUTHORITIES))
					.then(cloudFoundryService.associateAppBrokerClientWithOrgAndSpace(brokerClientId(), orgId, spaceId))
					.then(cloudFoundryService
						.pushBrokerApp(testBrokerAppName(), getTestBrokerAppPath(), brokerClientId(),
							appBrokerProperties))
					.then(cloudFoundryService.createServiceBroker(serviceBrokerName(), testBrokerAppName(), ignoreBrokerRegistrationErrors))
					.then(cloudFoundryService.enableServiceBrokerAccess(appServiceName()))
					.then(cloudFoundryService.enableServiceBrokerAccess(backingServiceName()))))
			.doOnRequest(l -> LOG.debug("START creating default org/space/pushing broker app/create broker/enable " +
					"broker access"))
			.doOnSuccess(l -> LOG.debug("FINISHED default org/space/pushing broker app/create broker/enable broker access"));
	}

	private boolean timehasElapsedLessThan(int MAX_POLL_DURATION_MS, long pollStartTime) {
		return currentTimeMillis() - pollStartTime < MAX_POLL_DURATION_MS;
	}

	private Mono<Void> updateBroker(List<String> appBrokerProperties) {
		return cloudFoundryService
			.updateBrokerApp(testBrokerAppName(), brokerClientId(), appBrokerProperties)
			.then(cloudFoundryService.updateServiceBroker(serviceBrokerName(), testBrokerAppName()));
	}

	private Mono<Void> cleanup(String orgId, String spaceId) {
		return
			cloudFoundryService.deleteServiceBroker(serviceBrokerName())
			.then(cloudFoundryService.deleteApp(testBrokerAppName()))
			.then(cloudFoundryService.removeAppBrokerClientFromOrgAndSpace(brokerClientId(), orgId, spaceId))
			.onErrorResume(e -> Mono.empty());
	}

	protected void createServiceInstance(String serviceInstanceName) {
		createServiceInstance(serviceInstanceName, Collections.emptyMap());
	}

	protected void createServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		createServiceInstance(appServiceName(), PLAN_NAME, serviceInstanceName, parameters);
	}

	protected void createServiceInstance(String serviceName,
		String planName,
		String serviceInstanceName,
		Map<String, Object> parameters) {
		cloudFoundryService.createServiceInstance(planName, serviceName, serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Create service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	protected ServiceInstance createServiceInstanceWithoutAsserts(String serviceInstanceName,
		Duration completionTimeout) {
		return createServiceInstanceWithoutAsserts(appServiceName(), PLAN_NAME, serviceInstanceName, emptyMap(),
			completionTimeout);
	}

	protected ServiceInstance createServiceInstanceWithoutAsserts(String serviceName,
		String planName,
		String serviceInstanceName,
		Map<String, Object> parameters, Duration completionTimeout) {
		try {
			return cloudFoundryService.createServiceInstance(planName, serviceName, serviceInstanceName, parameters,
				completionTimeout)
				.then(getServiceInstanceMono(serviceInstanceName))
				.block();
		}
		catch (DelayTimeoutException e) {
			LOG.info("createServiceInstance() completed with {}", e.toString());
			return cloudFoundryService.getServiceInstance(serviceInstanceName).block();
		}
	}

	protected void deleteServiceInstanceWithoutAsserts(String serviceInstanceName, Duration completionTimeout) {
		try {
			cloudFoundryService.deleteServiceInstance(serviceInstanceName, completionTimeout)
				.block();
		}
		catch (DelayTimeoutException e) {
			LOG.info("deleteServiceInstance() completed with {}", e.toString());
		}
	}

	protected void createServiceKey(String serviceKeyName, String serviceInstanceName) {
		createServiceKey(serviceKeyName, serviceInstanceName, Collections.emptyMap());
	}

	protected void createServiceKey(String serviceKeyName, String serviceInstanceName, Map<String, Object> parameters) {
		cloudFoundryService.createServiceKey(serviceKeyName, serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Create service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	public void updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		cloudFoundryService.updateServiceInstance(serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Update service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	public ServiceInstance updateServiceInstanceWithoutAsserts(String serviceInstanceName, Map<String, Object> parameters) {
		return cloudFoundryService.updateServiceInstance(serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.block();
	}

	protected void updateServiceInstance(String serviceInstanceName, String planName) {
		Duration completionTimeout = Duration.ofMinutes(5);
		cloudFoundryService.updateServiceInstance(serviceInstanceName, planName, completionTimeout)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Update service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	protected void upgradeService(String serviceInstanceName, String version) {
		cloudFoundryService.syncUpgradeServiceInstance(serviceInstanceName, version).block();
	}

	protected void updateServiceInstanceWithoutAsserts(String serviceInstanceName, String planName,
		Duration completionTimeout) {
		try {
			cloudFoundryService.updateServiceInstance(serviceInstanceName, planName, completionTimeout)
				.block();
		}
		catch (DelayTimeoutException e) {
			LOG.info("deleteServiceInstance() completed with {}", e.toString());
		}
	}

	protected void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}
	protected void deleteServiceInstanceWithoutCatchingException(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstanceWithoutCatchingException(serviceInstanceName));
	}
	protected void purgeServiceInstance(String serviceInstanceName) {
		LOG.info("Purging service instance with name {}", serviceInstanceName);
		cloudFoundryService.purgeServiceInstance(serviceInstanceName).block();
	}
	protected void purgeServiceInstance(String serviceInstanceName, String spaceName) {
		cloudFoundryService.purgeServiceInstance(serviceInstanceName, spaceName).block();
	}

	protected void deleteServiceKey(String serviceKeyName, String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceKey(serviceInstanceName, serviceKeyName));
	}

	protected List<String> listServiceInstances(String space) {
		return cloudFoundryService.listServiceInstances(space)
			.map(ServiceInstanceSummary::getName)
			.collectList()
			.block();
	}
	protected List<String> listServiceInstances() {
		return cloudFoundryService.listServiceInstances()
			.map(ServiceInstanceSummary::getName)
			.collectList()
			.block();
	}
	protected List<String> listServiceKeys(String serviceInstanceName, String space) {
		return cloudFoundryService.listServiceKeys(serviceInstanceName, space)
			.map(ServiceKey::getName)
			.collectList()
			.block();
	}

	protected ServiceInstance getServiceInstance(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).block();
	}

	protected Map<String, Object> getServiceInstanceParams(String serviceInstanceGuid) {
		return cloudFoundryService.getServiceInstanceParams(serviceInstanceGuid).block();
	}

	protected ServiceKey getServiceKey(String serviceKeyName, String serviceInstanceName, String space) {
		return getServiceKeyMono(serviceInstanceName, serviceKeyName, space).block();
	}

	protected ServiceKey getServiceKey(String serviceKeyName, String serviceInstanceName) {
		return getServiceKeyMono(serviceInstanceName, serviceKeyName).block();
	}

	protected ServiceInstance getServiceInstance(String serviceInstanceName, String space) {
		try {
			return cloudFoundryService.getServiceInstance(serviceInstanceName, space).block();
		}
		catch (IllegalArgumentException e) {
			LOG.debug("Unable to get si name=[{}] in space=[{}] caught {}", serviceInstanceName, space, e, e);
			throw e;
		}
	}

	protected ServiceInstance pollServiceInstanceIfMissing(String serviceInstanceName, String spaceName, int maxPollDurationMs)
		throws InterruptedException {
		ServiceInstance serviceInstance = null;
		int retry = 0;
		long pollStartTime = currentTimeMillis();
		do {
			try {
				serviceInstance = getServiceInstance(serviceInstanceName, spaceName);
			}
			catch (IllegalArgumentException e) {
				LOG.debug("Unable to get si name=[{}] in space=[{}] caught {}", serviceInstanceName, spaceName, e, e);
				String message = e.getMessage();
				if (message != null && message.contains("does not exist")) {
					retry++;
					int sleepTimeSecs = retry;
					LOG.debug("Sleeping {}s in retry #{}", sleepTimeSecs, retry);
					//noinspection BusyWait
					Thread.sleep(sleepTimeSecs * 1000);
				} else {
					throw e;
				}
			}
		} while (
			serviceInstance == null &&
				timehasElapsedLessThan(maxPollDurationMs, pollStartTime)
		);
		assertThat(serviceInstance)
			.withFailMessage(
				"still in missing after retrying " + retry + " times and " + (currentTimeMillis() - pollStartTime) / 1000 + " seconds")
			.isNotNull();
		return serviceInstance;
	}


	protected Flux<ServiceInstanceResource> listServiceInstanceMetadataByLabel(String labelSelector) {
		return cloudFoundryService.listServiceInstanceMetadataByLabel(labelSelector);
	}

	protected String getServiceInstanceGuid(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName)
			.map(ServiceInstance::getId)
			.block();
	}

	private Mono<ServiceInstance> getServiceInstanceMono(String serviceInstanceName) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	private Mono<ServiceKey> getServiceKeyMono(String serviceInstanceName, String serviceInstanceKeyName,
		String space) {
		return cloudFoundryService.getServiceKey(serviceInstanceName, serviceInstanceKeyName, space);
	}

	private Mono<ServiceKey> getServiceKeyMono(String serviceInstanceName, String serviceInstanceKeyName) {
		return cloudFoundryService.getServiceKey(serviceInstanceName, serviceInstanceKeyName);
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName) {
		return cloudFoundryService
			.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName, String space) {
		return cloudFoundryService.getApplication(appName, space).blockOptional();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName) {
		return cloudFoundryService.getApplicationEnvironment(appName).block();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName, String space) {
		return cloudFoundryService.getApplicationEnvironment(appName, space).block();
	}

	protected DocumentContext getSpringAppJson(String appName) {
		ApplicationEnvironments env = getApplicationEnvironment(appName);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	protected DocumentContext getSpringAppJson(String appName, String space) {
		ApplicationEnvironments env = getApplicationEnvironment(appName, space);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	protected List<String> getSpaces() {
		return cloudFoundryService.getSpaces().block();
	}

	protected Optional<GetClientResponse> getUaaClient(String clientId) {
		return uaaService.getUaaClient(clientId)
			.blockOptional();
	}

	protected void createDomain(String domain) {
		cloudFoundryService.createDomain(domain).block();
	}

	protected void deleteDomain(String domain) {
		cloudFoundryService.deleteDomain(domain).block();
	}

	private Path getTestBrokerAppPath() {
		return Paths.get(acceptanceTestProperties.getBrokerAppPath(), "");
	}

	protected <T> void blockingSubscribe(Mono<? super T> publisher) {
		publisher.block();
		//Not clear with SCAB did not use block(), maybe because of static code analysis offenses.
		//Side effect was that exception thrown were swallowed
		/*
		CountDownLatch latch = new CountDownLatch(1);
		publisher.subscribe(System.out::println, t -> {
			if (LOG.isDebugEnabled()) {
				LOG.debug("error subscribing to publisher", t);
			}
			latch.countDown();
		}, latch::countDown);
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		 */
	}

	protected Mono<String> manageApps(String serviceInstanceName, String operation) {
		//noinspection ReactiveStreamsNullableInLambdaInTransform
		return cloudFoundryService
			.getServiceInstance(serviceInstanceName)
			.map(ServiceInstance::getId)
			.flatMap(serviceInstanceId ->
				cloudFoundryService
					.getApplicationRoute(testBrokerAppName())
					.flatMap(appRoute ->
						webClient.get()
							.uri(URI.create(appRoute + "/" + operation + "/" + serviceInstanceId))
							.exchange()
							.flatMap(clientResponse -> clientResponse.toEntity(String.class))
							.map(HttpEntity::getBody)));
	}

	private WebClient getSslIgnoringWebClient() {
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient
				.create()
				.secure(t -> {
					try {
						t.sslContext(SslContextBuilder
							.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE)
							.build());
					}
					catch (SSLException e) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("problem ignoring SSL in WebClient", e);
						}
					}
				})))
			.build();
	}

	protected Mono<List<ApplicationDetail>> getApplications(String app1, String app2) {
		return Flux.merge(cloudFoundryService.getApplication(app1),
			cloudFoundryService.getApplication(app2))
			.parallel()
			.runOn(Schedulers.parallel())
			.sequential()
			.collectList();
	}

}
