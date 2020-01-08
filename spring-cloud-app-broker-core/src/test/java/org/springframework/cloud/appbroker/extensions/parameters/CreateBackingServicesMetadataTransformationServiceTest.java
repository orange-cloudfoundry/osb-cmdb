package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

class CreateBackingServicesMetadataTransformationServiceTest {

	@Test
	void populates_expected_labels_and_annotations() {
		//given
		BackingServices backingServices = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service")
				.plan("a-plan")
				.serviceInstanceName("my-service-instance")
				.build())
			.build();

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest
			.builder()
			.serviceInstanceId("service-instance-id")
			.serviceDefinitionId("p-mysql" + "-id")
			.planId("10mb" + "-id")
			.serviceDefinition(ServiceDefinition
				.builder()
				.id("p-mysql" + "-id")
				.name("p-mysql")
				.plans(Plan.builder()
					.id("10mb" + "-id")
					.name("10mb")
					.build())
				.build())
			.plan(Plan.builder()
				.id("10mb" + "-id")
				.name("10mb")
				.build())
			.parameters(null == null ? new HashMap<>() : null)
			.context(CloudFoundryContext.builder()
//				.property("organization_guid", "organization-guid-here")  // SCOSB mangles the original property name
				.property("organizationGuid", "organization-guid-here")
				.property("organization_name", "organization-name-here")
//				.property("space_guid", "space-guid-here")
				.property("spaceGuid", "space-guid-here")
				.property("space_name", "space-name-here")
				.property("instance_name", "instance-name-here")
				.build())
			.originatingIdentity(CloudFoundryContext.builder()
				.property("user_id", "user-id-here")
				.build())
			.apiInfoLocation("api.my-cf.org/v2/info")
			.build();

		CreateBackingServicesMetadataTransformationService createBackingServicesMetadataTransformationService = new CreateBackingServicesMetadataTransformationServiceImpl();

		//when
		createBackingServicesMetadataTransformationService.transformMetadata(backingServices, request);

		//then
		BackingService backingService = backingServices.get(0);
		Map<String, String> annotations = backingService.getAnnotations();
		Map<String, String> labels = backingService.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id"),
			entry("brokered_service_context_organization_guid", "organization-guid-here"),
			entry("brokered_service_context_space_guid", "space-guid-here"),
			entry("brokered_service_originating_identity_user_id", "user-id-here")
		);
		assertThat(annotations).containsOnly(
			entry("brokered_service_context_organization_name", "organization-name-here"),
			entry("brokered_service_context_space_name", "space-name-here"),
			entry("brokered_service_context_instance_name", "instance-name-here"),
			entry("brokered_service_api_info_location", "api.my-cf.org/v2/info")
		);
	}

}