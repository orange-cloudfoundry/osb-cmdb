package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogConstants;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Launches osb-cmdb without the CF client and SCAB part.
 * Tests application.yml defaults and its overriding in application-default
 */
@ActiveProfiles(
    {"offline-test-without-scab", //disable service key workflow so that we can start without CF config
        "default" //simulate default profile being enabled (the case when running in production in paas-templates)
    })
@ExtendWith(SpringExtension.class)
@SpringBootTest( //Spring boot level test since we rely on springboot support of application.yml
    properties = {
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

        //Define mandatory users to access OSB and actuactor endpoints
        "spring.security.user.name="+ SecurityConfigTest.USER,
        "spring.security.user.password="+ SecurityConfigTest.PASSWORD,
        "osbcmdb.admin.user=" + SecurityConfigTest.ADMIN_USER,
        "osbcmdb.admin.password=" + SecurityConfigTest.ADMIN_PASSWORD,

        "anotherKey=value"})
public class ApplicationConfigurationIntegrationTest {

    @Value("${osbcmdb.sample-noop-test-property}")
    private String samplePropertyInjected;

    @Test
    void paas_templates_overrides_default_cmdb_config_in_application_default_yml_Overrides_application_yml() {
        assertThat(samplePropertyInjected).isEqualTo("set in application-default.yml");
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
