package com.orange.oss.osbcmdb.serviceinstance;

import java.time.Duration;

import com.orange.oss.osbcmdb.metadata.CreateServiceMetadataFormatterServiceImpl;
import com.orange.oss.osbcmdb.metadata.UpdateServiceMetadataFormatterService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.organizations.OrganizationQuota;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Skeletton unit test. Mostly incomplete for now.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OsbCmdbServiceInstanceTest {

	private static final long DEFAULT_COMPLETION_DURATION = 5;

	@Mock
	private org.cloudfoundry.client.v2.organizations.Organizations clientOrganizations;

	@Mock
	private ServiceInstances clientServiceInstances;

	@Mock
	private org.cloudfoundry.client.v2.spaces.Spaces clientSpaces;

	@Mock
	private CloudFoundryClient cloudFoundryClient;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@Mock
	private Organizations operationsOrganizations;

	@Mock
	private Services operationsServices;

	@Mock
	private Spaces operationsSpaces;

	private OsbCmdbServiceInstance osbCmdbServiceInstance;

	@Test
	void serializesStateToJson() {
		String json = osbCmdbServiceInstance.toJson(new OsbCmdbServiceInstance.CmdbOperationState("guid",
			OsbCmdbServiceInstance.OsbOperation.CREATE));
		assertThat(json).isEqualTo("{\"backingCfServiceInstanceGuid\":\"guid\",\"operationType\":\"CREATE\"}");
	}

	@Test
	void deserializesStateFromJson() {
		String json = "{\"backingCfServiceInstanceGuid\":\"guid\",\"operationType\":\"CREATE\"}";
		assertThat(osbCmdbServiceInstance.fromJson(json)).isEqualTo(new OsbCmdbServiceInstance.CmdbOperationState("guid",
			OsbCmdbServiceInstance.OsbOperation.CREATE));
	}

	@Test
	@Disabled("need work to deal with CF client mock")
	void createServiceInstanceWithTarget() {
		given(operationsOrganizations
			.get(
				OrganizationInfoRequest
					.builder()
					.name("default-org")
					.build()))
			.willReturn(Mono.just(
				OrganizationDetail
					.builder()
					.id("default-org-id")
					.name("default-org")
					.quota(OrganizationQuota
						.builder()
						.id("quota-id")
						.instanceMemoryLimit(0)
						.organizationId("default-org-id")
						.name("quota")
						.paidServicePlans(false)
						.totalMemoryLimit(0)
						.totalRoutes(0)
						.totalServiceInstances(0)
						.build())
					.build()));

		given(clientOrganizations
			.listSpaces(ListOrganizationSpacesRequest
				.builder()
				.name("service-instance-id")
				.organizationId("default-org-id")
				.page(1)
				.build()))
			.willReturn(Mono.empty());

		given(clientSpaces
			.create(CreateSpaceRequest
				.builder()
				.organizationId("default-org-id")
				.name("service-instance-id")
				.build()))
			.willReturn(Mono.empty());

		given(operationsServices
			.createInstance(
				org.cloudfoundry.operations.services.CreateServiceInstanceRequest
					.builder()
					.serviceInstanceName("service-instance-name")
					.serviceName("db-service")
					.planName("standard")
					.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
					.parameters(emptyMap())
					.build()))
			.willReturn(Mono.empty());

		CreateServiceInstanceRequest request =
			CreateServiceInstanceRequest
				.builder()
				.serviceDefinition(ServiceDefinition.builder()
					.name("db-service").build())
				.serviceInstanceId("service-instance-id")
				.plan(Plan.builder()
					.name("standard").build())
				.parameters(emptyMap())
				.context(CloudFoundryContext.builder()
					.instanceName("service-instance-name")
					.build())
				.build();

		StepVerifier.create(
			osbCmdbServiceInstance.createServiceInstance(request))
			.assertNext(response -> {
				assertThat(response.isInstanceExisted()).isFalse();
				assertThat(response.getDashboardUrl()).isNull();
				assertThat(response.isAsync()).isFalse();
			})
			.verifyComplete();
	}

	@BeforeEach
	void setUp() {
		String defaultOrg = "default-org";
		String defaultSpace = "default-space";


		given(cloudFoundryOperations.spaces()).willReturn(operationsSpaces);
		given(cloudFoundryOperations.services()).willReturn(operationsServices);
		given(cloudFoundryOperations.organizations()).willReturn(operationsOrganizations);
		given(cloudFoundryClient.serviceInstances()).willReturn(clientServiceInstances);
		given(cloudFoundryClient.spaces()).willReturn(clientSpaces);
		given(cloudFoundryClient.organizations()).willReturn(clientOrganizations);

		osbCmdbServiceInstance = new OsbCmdbServiceInstance(cloudFoundryOperations,
			cloudFoundryClient, defaultOrg, "userName", null,
			new CreateServiceMetadataFormatterServiceImpl(), new UpdateServiceMetadataFormatterService(), true, true);
	}

}