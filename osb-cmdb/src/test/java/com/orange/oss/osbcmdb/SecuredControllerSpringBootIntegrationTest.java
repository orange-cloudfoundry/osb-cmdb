package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring cloud security integration test.
 * Uses
 */
@Profile("offline-test-without-scab") //disable service key workflow
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    //Disable dynamic catalog
    DynamicCatalogConstants.OPT_IN_PROPERTY+"=false",
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

    "anotherKey=value"
})
public class SecuredControllerSpringBootIntegrationTest {

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

    @Test
    public void unAuthenticatedActuactorHealth_shouldSucceedWith200() throws Exception {
        mvc.perform(get("/actuator/health")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void unAuthenticatedActuactorInfo_shouldFailWith401() throws Exception {
        mvc.perform(get("/actuator/info")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @WithMockUser()
    @Test
    public void authenticatedActuactorInfo_shouldSucceedWith200() throws Exception {
        mvc.perform(get("/actuator/info")
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
