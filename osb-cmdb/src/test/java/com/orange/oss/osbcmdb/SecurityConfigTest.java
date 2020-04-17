package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogConstants;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring cloud security integration test.
 * Uses spring web mvc mock support. See inspiration from https://www.baeldung.com/spring-security-integration-tests
 * Reference doc https://docs.spring.io/spring-security/site/docs/5.3.0.RELEASE/reference/html5/#running-as-a-user-in-spring-mvc-test-with-annotations
 * https://docs.spring.io/spring-security/site/docs/5.3.0.RELEASE/reference/html5/#testing-http-basic-authentication
 */
@ActiveProfiles(
	{"offline-test-without-scab" //disable service key workflow so that we can start without CF config
	})
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) // real web service is needed
@TestPropertySource(properties = {
	//Disable dynamic catalog
	DynamicCatalogConstants.OPT_IN_PROPERTY + "=false",
	//provide a default catalog for org.springframework
	// .cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration to start
	"spring.cloud.openservicebroker.catalog.services[0].id=a-fake-id",
	"spring.cloud.openservicebroker.catalog.services[0].name=name",
	"spring.cloud.openservicebroker.catalog.services[0].description=A service that deploys a backing app",
	"spring.cloud.openservicebroker.catalog.services[0].bindable=true",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=id",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].name=standard",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable=true",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].description=A simple plan",
	"spring.cloud.openservicebroker.catalog.services[0].plans[0].free=true",
	"spring.security.user.name=" + SecurityConfigTest.USER,
	"spring.security.user.password=" + SecurityConfigTest.PASSWORD,

	"osbcmdb.admin.user=" + SecurityConfigTest.ADMIN_USER,
	"osbcmdb.admin.password=" + SecurityConfigTest.ADMIN_PASSWORD,
	"anotherKey=value"
})
public class SecurityConfigTest {

	public static final String USER = "unit-test-user";

	public static final String PASSWORD = "unit-test-password";

	public static final String ADMIN_USER = "unit-test-admin-user";

	public static final String ADMIN_PASSWORD = "unit-test-admin-password";

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		mvc = MockMvcBuilders
			.webAppContextSetup(context)
			.apply(springSecurity())
			.build();
	}

	@Test
	public void unauthenticatedOsbRequest_shouldFailWith401() throws Exception {
		mvc.perform(get("/v2/catalog")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}

	@WithMockUser()
	@Test
	public void authenticatedOsbRequest_shouldSucceedWith200() throws Exception {
		mvc.perform(get("/v2/catalog")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	/**
	 * Ensure CSFR filter is disabled and does not prevents OSB PUT
	 */
    @WithMockUser()
    @Test
    public void authenticatedPostOsbRequest_shouldSucceedWith200() throws Exception {
        mvc.perform(
            put("/v2/service_instances/c594cdcf-72b2-4f24-ba51-ee8f2b179a4d?accepts_incomplete=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                    "  \"service_id\": \"a-fake-id\",\n" +
                    "  \"plan_id\": \"id\",\n" +
                    "  \"context\": {\n" +
                    "    \"platform\": \"cloudfoundry\",\n" +
                    "    \"some_field\": \"some-contextual-data\"\n" +
                    "  },\n" +
                    "  \"organization_guid\": \"org-guid-here\",\n" +
                    "  \"space_guid\": \"space-guid-here\",\n" +
                    "  \"parameters\": {\n" +
                    "    \"parameter1\": 1,\n" +
                    "    \"parameter2\": \"foo\"\n" +
                    "  }\n" +
                    "}")
        )
            .andExpect(status().isOk());
    }

	@Test
	public void basicAuthAuthenticatedOsbRequest_shouldSucceedWith200() throws Exception {
		mvc.perform(get("/v2/catalog")
			.with(httpBasic(USER, PASSWORD))
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	public void unAuthenticatedActuactorHealth_shouldSucceedWith200() throws Exception {
		mvc.perform(get("/actuator/health")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	public void unAuthenticatedSensitiveActuactorEndPoints_shouldFailWith401() throws Exception {
		String[] endpoints = {"beans", "conditions", "info", "httptrace", "loggers", "metrics", "threaddump"};
		for (String endpoint : endpoints) {
			mvc.perform(get("/actuator/" + endpoint)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
		}
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void adminAuthenticatedSensitiveActuactorEndPoints_shouldSucceedWith200() throws Exception {
		String[] endpoints = {"conditions", "info", "httptrace", "loggers", "metrics", "threaddump"};
		for (String endpoint : endpoints) {
			mvc.perform(get("/actuator/" + endpoint)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		}
	}

	@Test
	public void basicAuthAuthenticatedAdmin_to_ActuactorInfo_shouldSucceedWith200() throws Exception {
		mvc.perform(get("/actuator/info")
			.with(httpBasic(ADMIN_USER, ADMIN_PASSWORD))
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	public void basicAuthAuthenticatedOsbUser_to_ActuactorInfo_shouldSucceedWith401() throws Exception {
		mvc.perform(get("/actuator/info")
			.with(httpBasic(USER, PASSWORD))
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	//Ensure SC-OSB starts and serves OSB endpoints
	@TestConfiguration
	public static class FakeOsbHandlerConfig {

		@Bean
		public ServiceInstanceService serviceInstanceService() {
			return new ServiceInstanceService() {
				@Override
				public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
					return Mono.just(CreateServiceInstanceResponse.builder()
						.build());
				}

				@Override
				public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
					return Mono.just(DeleteServiceInstanceResponse.builder().build());
				}
			};
		}

	}

}
