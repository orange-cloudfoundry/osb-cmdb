package com.orange.oss.osbcmdb.integration.cmdb;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.osbcmdb.integration.cmdb.fixtures.ExtendedCloudControllerStubFixture;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogConstants;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration;
import org.springframework.cloud.appbroker.autoconfigure.ServiceDefinitionMapperProperties;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.CredHubStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.integration.fixtures.TestBindingCredentialsProviderFixture;
import org.springframework.cloud.appbroker.integration.fixtures.UaaStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.WiremockServerFixture;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Checks that the DynamicCatalog autoconfiguration bean is automatically triggering.
 */
@ExtendWith(SpringExtension.class) //Junit 5 jupiter support
@ContextConfiguration(classes = {
	WiremockServerFixture.class,
	OpenServiceBrokerApiFixture.class,
	ExtendedCloudControllerStubFixture.class,
	UaaStubFixture.class,
	CredHubStubFixture.class,
	TestBindingCredentialsProviderFixture.class})
@TestPropertySource(properties = {
	"spring.cloud.appbroker.deployer.cloudfoundry.api-host=localhost",
	"spring.cloud.appbroker.deployer.cloudfoundry.api-port=8080",
	"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
	"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
	"spring.cloud.appbroker.deployer.cloudfoundry.default-org=test",
	"spring.cloud.appbroker.deployer.cloudfoundry.default-space=development",
	"spring.cloud.appbroker.deployer.cloudfoundry.secure=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //throw away wiremock across each test
class DynamicServiceAutoConfigurationComponentTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private WiremockServerFixture wiremockFixture;

	@Autowired
	private ExtendedCloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private CloudControllerStubFixture cloudFoundryFixture;


	@Autowired
	private UaaStubFixture uaaFixture;

	@AfterAll
	void tearDown() {
		wiremockFixture.stopWiremock();
	}

	@BeforeEach
	void resetWiremock() {
		wiremockFixture.resetWiremock();

		uaaFixture.stubCommonUaaRequests();
		cloudFoundryFixture.stubCommonCloudControllerRequests();
	}

	@AfterEach
	void verifyStubs() {
		wiremockFixture.verifyAllRequiredStubsUsed();
	}

	private final ApplicationContextRunner contextRunner = setUpCommonApplicationContextRunner("");

	private ApplicationContextRunner setUpCommonApplicationContextRunner(String extraProperty) {
		return new ApplicationContextRunner()
			.withPropertyValues(
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.api-host=localhost",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.api-port=8080",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.username=admin",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.password=adminpass",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.default-org=test",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.default-space=development",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.secure=false",
				"spring.cloud.appbroker.acceptance-test.cloudfoundry.client-id=osb-cmdb-acceptance-test",
				"spring.cloud.appbroker.acceptance-test.cloudfoundry.client-secret=IPN4500Bgf0fQhZrA0CBpIovYzAyhln",
				DynamicCatalogConstants.OPT_IN_PROPERTY+"=true",
				extraProperty)
			.withConfiguration(AutoConfigurations.of(
				TargetPropertiesConfiguration.class,
				CloudFoundryClientConfiguration.class,
				DynamicCatalogServiceAutoConfiguration.class
			));
	}

	@Test
	void catalogCreatedWithDetailedValidMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-detailed");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-detailed");

		this.contextRunner
			.run(context -> {
				assertThat(context).hasSingleBean(BrokeredServices.class);
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).isNotEmpty();

				assertThat(context).hasSingleBean(Catalog.class);
				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).isNotEmpty();
				ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
				assertThat(serviceDefinition.getName()).isEqualTo("db-service");
				assertThat(serviceDefinition.getDescription()).isEqualTo("My DB Service");
				assertThat(serviceDefinition.isBindable()).isTrue(); //Non default value
				assertThat(serviceDefinition.isPlanUpdateable()).isTrue(); //Non default value
				assertThat(serviceDefinition.isInstancesRetrievable()).isTrue(); //Non default value
				assertThat(serviceDefinition.getTags()).containsOnly("tag1", "tag2");
				
				Map<String, Object> metadata = serviceDefinition.getMetadata();
				assertThat(metadata)
					.isNotNull()
					.isNotEmpty()
					.containsOnly(
						entry("displayName", "displayName"),
						entry("longDescription", "longDescription")
						);
				assertThat(serviceDefinition.getPlans().size()).isEqualTo(1);
				Plan plan = serviceDefinition.getPlans().get(0);

				logger.info("schemas: {}", plan.getSchemas());
				assertThat(plan.getSchemas()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3); //$schema, type, properties
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3); //$schema, type, properties
				//noinspection unchecked
				Map<String, Object> properties = (Map<String, Object>) plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().get("properties");
				assertThat(properties.get("baz")).isNotNull(); //$schema, type, properties
				assertThat(plan.getSchemas().getServiceInstanceSchema().getUpdateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getUpdateMethodSchema().getParameters().size()).isEqualTo(3);
				assertThat(plan.getSchemas().getServiceBindingSchema().getCreateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceBindingSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3);

				assertThat(plan.getMetadata()).isNotEmpty();
				assertThat(plan.getMetadata().get("costs")).isNotNull();
			});
	}
	@Test
	void catalogCreatedWithNullMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-null-extra");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans");
		assertCatalogCreatesWithoutError();
	}

	@Test
	void catalogCreatedWithSimpleMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans");
		assertCatalogCreatesWithoutError();
	}

	@Test
	void excludesBrokersMatchingRegexp() {
		//Given a marketplace with 2 service definition
		aStubbedMarketplaceWithTwoServiceOfferings();

		//And a broker exclusion regexp configrued
		String extraProperty = ServiceDefinitionMapperProperties.PROPERTY_PREFIX
			+ ServiceDefinitionMapperProperties.EXCLUDE_BROKER_PROPERTY_KEY
			+ "=service_broker_name2";
		ApplicationContextRunner customContextRunner = setUpCommonApplicationContextRunner(extraProperty);

		//when fetching marketplace
		customContextRunner
			.run(context -> {
				//then
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).hasSize(1);

				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).hasSize(1);
			});
	}

	private void aStubbedMarketplaceWithTwoServiceOfferings() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-multiple");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-detailed", "SERVICE-ID");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-service2", "SERVICE-ID2");
	}

	@Test
	void doesNotExcludeBrokersNotMatchingRegexp() {
		//Given a marketplace with 2 service definition
		aStubbedMarketplaceWithTwoServiceOfferings();

		//And a broker exclusion regexp configrued
		String extraProperty = ServiceDefinitionMapperProperties.PROPERTY_PREFIX
			+ ServiceDefinitionMapperProperties.EXCLUDE_BROKER_PROPERTY_KEY
			+ "=non_matching_broker_name";
		ApplicationContextRunner customContextRunner = setUpCommonApplicationContextRunner(extraProperty);

		//when fetching marketplace
		customContextRunner
			.run(context -> {
				//then
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).hasSize(2);

				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).hasSize(2);
			});
	}

	private void assertCatalogCreatesWithoutError() {
		this.contextRunner
			.run(context -> {
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).isNotEmpty();

				Catalog catalog = context.getBean(Catalog.class);
				List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions();
				assertThat(serviceDefinitions).isNotEmpty();
				serviceDefinitions.stream()
					.map(ServiceDefinition::getPlans)
					.flatMap(Collection::stream)
					.forEach(this::assertPlanSerializesWithoutPollutingWithNulls);
			});
	}

	private void assertPlanSerializesWithoutPollutingWithNulls(Plan plan)  {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String serializedPlan = mapper.writeValueAsString(plan);
			logger.info("serializedPlan {}", serializedPlan);
			assertThat(serializedPlan).doesNotContain(":null");
			assertThat(serializedPlan).doesNotContain("\"parameters\": {}");
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	@Ignore
		//Not yet implemented
	void catalogFetchingFailuresThrowsException() {
		//TODO: fail if the flux contains error events (was the case when Jackson was not configured to ignore
	}


	public static class TargetPropertiesConfiguration {

		//Inspired from spring-cloud-app-broker-autoconfigure/src/main/java/org/springframework/cloud/appbroker/autoconfigure/CloudFoundryAppDeployerAutoConfiguration.java
		static final String PROPERTY_PREFIX = "spring.cloud.appbroker.acceptancetest.cloudfoundry";

		@Bean
		@ConfigurationProperties(PROPERTY_PREFIX)
		CloudFoundryTargetProperties cloudFoundryTargetProperties() {
			return new CloudFoundryTargetProperties();
		}


		//Inspired from spring-cloud-app-broker-autoconfigure/src/main/java/org/springframework/cloud/appbroker/autoconfigure/CloudFoundryAppDeployerAutoConfiguration.java
		@Bean
		@ConfigurationProperties(PROPERTY_PREFIX + ".properties")
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties() {
			return new CloudFoundryDeploymentProperties();
		}

	}

	@ConfigurationProperties(TargetPropertiesConfiguration.PROPERTY_PREFIX)
//Inspired from spring-cloud-app-broker-acceptance-tests
	public static class CloudFoundryProperties {

		static final String PROPERTY_PREFIX = "spring.cloud.appbroker.acceptancetest.cloudfoundry";

		private String apiHost;
		private Integer apiPort;
		private String defaultOrg;
		private String defaultSpace;
		private String username;
		private String password;
		private String clientId;
		private String clientSecret;
		private String identityZoneSubdomain;
		private boolean secure = true;
		private boolean skipSslValidation;

		public String getApiHost() {
			return apiHost;
		}

		public void setApiHost(String apiHost) {
			this.apiHost = parseApiHost(apiHost);
		}

		public Integer getApiPort() {
			return apiPort;
		}

		public void setApiPort(int apiPort) {
			this.apiPort = apiPort;
		}

		public String getDefaultOrg() {
			return defaultOrg;
		}

		public void setDefaultOrg(String defaultOrg) {
			this.defaultOrg = defaultOrg;
		}

		public String getDefaultSpace() {
			return defaultSpace;
		}

		public void setDefaultSpace(String defaultSpace) {
			this.defaultSpace = defaultSpace;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getIdentityZoneSubdomain() {
			return identityZoneSubdomain;
		}

		public void setIdentityZoneSubdomain(String identityZoneSubdomain) {
			this.identityZoneSubdomain = identityZoneSubdomain;
		}

		public ProxyConfiguration getProxyConfiguration() {
			return null;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public boolean isSkipSslValidation() {
			return skipSslValidation;
		}

		public void setSkipSslValidation(boolean skipSslValidation) {
			this.skipSslValidation = skipSslValidation;
		}

		private static String parseApiHost(String api) {
			final URI uri = URI.create(api);
			return uri.getHost() == null ? api : uri.getHost();
		}

	}

	@EnableConfigurationProperties(CloudFoundryProperties.class)
//Inspired from spring-cloud-app-broker-acceptance-tests
	public static class CloudFoundryClientConfiguration {

		public static final String APP_BROKER_CLIENT_SECRET = "app-broker-client-secret";
		public static final String[] APP_BROKER_CLIENT_AUTHORITIES = {
			"cloud_controller.read", "cloud_controller.write", "clients.write"
		};

		@Bean
		CloudFoundryOperations cloudFoundryOperations(
			CloudFoundryProperties properties,
			CloudFoundryClient client,
			DopplerClient dopplerClient,
			UaaClient uaaClient) {
			return DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(client)
				.dopplerClient(dopplerClient)
				.uaaClient(uaaClient)
				.organization(properties.getDefaultOrg())
				.space(properties.getDefaultSpace())
				.build();
		}

		@Bean
		CloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
			@Qualifier("userCredentials") TokenProvider tokenProvider) {
			return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
		}

		@Bean
		ConnectionContext connectionContext(
			CloudFoundryProperties properties) {
			return DefaultConnectionContext.builder()
				.apiHost(properties.getApiHost())
				.port(Optional.ofNullable(properties.getApiPort()))
				.skipSslValidation(properties.isSkipSslValidation())
				.secure(properties.isSecure())
				.build();
		}

		@Bean
		DopplerClient dopplerClient(ConnectionContext connectionContext,
			@Qualifier("userCredentials") TokenProvider tokenProvider) {
			return ReactorDopplerClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
		}

		@Bean
		UaaClient uaaClient(ConnectionContext connectionContext,
			@Qualifier("clientCredentials") TokenProvider tokenProvider) {
			return ReactorUaaClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
		}

		@Bean
		@Qualifier("userCredentials")
		@ConditionalOnProperty({
			CloudFoundryProperties.PROPERTY_PREFIX + ".username",
			CloudFoundryProperties.PROPERTY_PREFIX + ".password"
		})
		PasswordGrantTokenProvider passwordTokenProvider(
			CloudFoundryProperties properties) {
			return PasswordGrantTokenProvider.builder()
				.password(properties.getPassword())
				.username(properties.getUsername())
				.build();
		}

		@Bean
		@Qualifier("clientCredentials")
		@ConditionalOnProperty({
			CloudFoundryProperties.PROPERTY_PREFIX + ".client-id",
			CloudFoundryProperties.PROPERTY_PREFIX + ".client-secret"
		})
		ClientCredentialsGrantTokenProvider clientTokenProvider(
			CloudFoundryProperties properties) {
			return ClientCredentialsGrantTokenProvider.builder()
				.clientId(properties.getClientId())
				.clientSecret(properties.getClientSecret())
				.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
				.build();
		}

	}


}
