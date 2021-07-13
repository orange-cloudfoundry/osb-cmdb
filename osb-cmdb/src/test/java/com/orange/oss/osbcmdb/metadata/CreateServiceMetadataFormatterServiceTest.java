package com.orange.oss.osbcmdb.metadata;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.KubernetesContext;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateServiceMetadataFormatterServiceTest {

	CreateServiceMetadataFormatterService createServiceMetadataFormatterService = new CreateServiceMetadataFormatterServiceImpl(
		new K8SMetadataFormatter(), new CfMetadataFormatter(new CfAnnotationValidator()));



	@Test
	void populates_expected_labels_and_annotations_for_null_profile() {
		//given
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
			.parameters(new HashMap<>())
			.build();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request);

		//then
		Map<String, String> annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id")
		);
		assertThat(annotations).isEmpty();
	}
	@Test
	void populates_expected_labels_and_annotations_for_cf_profile() {
		//given
		HashMap<String, Object> organizationAnnotations = new HashMap<>();
		organizationAnnotations.put("domain.com/org-key1", "org-value1");
		organizationAnnotations.put("orange.com/overrideable-key", "org-value2");
		HashMap<String, Object> spaceAnnotations = new HashMap<>();
		spaceAnnotations.put("domain.com/space-key1", "space-value1");
		spaceAnnotations.put("orange.com/overrideable-key", "space-value2");
		HashMap<String, Object> instanceAnnotations = new HashMap<>();
		instanceAnnotations.put("domain.com/instance-key1", "instance-value1");
		instanceAnnotations.put("orange.com/overrideable-key", "instance-value2");
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
			.parameters(new HashMap<>())
			.context(CloudFoundryContext.builder()
//				.property("organization_guid", "organization-guid-here")  // SCOSB mangles the original property name
				.property("organizationGuid", "organization-guid-here")
				.property("organization_name", "organization-name-here")
//				.property("space_guid", "space-guid-here")
				.property("spaceGuid", "space-guid-here")
				.property("space_name", "space-name-here")
				.property("instance_name", "instance-name-here")
				.property("organization_annotations", organizationAnnotations)
				.property("space_annotations", spaceAnnotations)
				.property("instance_annotations", instanceAnnotations)
				.build())
			.originatingIdentity(CloudFoundryContext.builder()
				.property("user_id", "user-id-here")
				.build())
			.apiInfoLocation("api.my-cf.org/v2/info")
			.build();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request);

		//then
		Map<String, String> annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id"),
			entry("brokered_service_context_organization_guid", "organization-guid-here"),
			entry("brokered_service_context_space_guid", "space-guid-here"),
			entry("brokered_service_originating_identity_user_id", "user-id-here"),
			entry("brokered_service_context_orange_overrideable-key", "instance-value2")
		);
		assertThat(annotations).containsOnly(
			entry("brokered_service_context_organization_name", "organization-name-here"),
			entry("brokered_service_context_space_name", "space-name-here"),
			entry("brokered_service_context_instance_name", "instance-name-here"),
			entry("brokered_service_api_info_location", "api.my-cf.org/v2/info"),
			entry("brokered_service_context_organization_annotations",
				"{\"orange.com/overrideable-key\":\"org-value2\",\"domain.com/org-key1\":\"org-value1\"}"),
			entry("brokered_service_context_space_annotations",
				"{\"orange.com/overrideable-key\":\"space-value2\",\"domain.com/space-key1\":\"space-value1\"}"),
			entry("brokered_service_context_instance_annotations",
				"{\"orange.com/overrideable-key\":\"instance-value2\",\"domain" +
					".com/instance-key1\":\"instance-value1\"}")
		);
	}

	@Test
	void rejects_orange_annotations_that_cannot_be_indexed_as_labels() {
		//given
		HashMap<String, Object> organizationAnnotations = new HashMap<>();
		HashMap<String, Object> spaceAnnotations = new HashMap<>();
		spaceAnnotations.put("orange.com/a-valid-key", "an invalid value with spaces");
		HashMap<String, Object> instanceAnnotations = new HashMap<>();
		CreateServiceInstanceRequest request = aCreateRequestWithAnnotations(organizationAnnotations, spaceAnnotations,
			instanceAnnotations);

		//when
		ServiceBrokerInvalidParametersException exception = assertThrows(ServiceBrokerInvalidParametersException.class, () -> {
			createServiceMetadataFormatterService.formatAsMetadata(request);
		});

		assertThat(exception.getMessage()).contains("due to value violation to regex");
	}

	private CreateServiceInstanceRequest aCreateRequestWithAnnotations(HashMap<String, Object> organizationAnnotations,
		HashMap<String, Object> spaceAnnotations, HashMap<String, Object> instanceAnnotations) {
		return CreateServiceInstanceRequest
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
			.parameters(new HashMap<>())
			.context(CloudFoundryContext.builder()
//				.property("organization_guid", "organization-guid-here")  // SCOSB mangles the original property name
				.property("organizationGuid", "organization-guid-here")
				.property("organization_name", "organization-name-here")
//				.property("space_guid", "space-guid-here")
				.property("spaceGuid", "space-guid-here")
				.property("space_name", "space-name-here")
				.property("instance_name", "instance-name-here")
				.property("organization_annotations", organizationAnnotations)
				.property("space_annotations", spaceAnnotations)
				.property("instance_annotations", instanceAnnotations)
				.build())
			.originatingIdentity(CloudFoundryContext.builder()
				.property("user_id", "user-id-here")
				.build())
			.apiInfoLocation("api.my-cf.org/v2/info")
			.build();
	}


	@Test
	void populates_expected_labels_and_annotations_for_kubernetes_profile() {
		//given
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
			.parameters(new HashMap<>())
			.context(KubernetesContext.builder()
				.namespace("a-namespace")
				.property("clusterid", "a-cluster-id")
				.property("instance_name", "an-instance-name")
				.build())
			.originatingIdentity(KubernetesContext.builder()
				.property("username", "a-user-name")
				.property("uid", "a-user-id")
				.property("groups", asList("admin", "dev"))
				.property("extra", singletonMap("mydata", asList("data1", "data3")))
				.build())
			.build();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request);

		//then
		Map<String, String> annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id"),
			entry("brokered_service_context_namespace", "a-namespace"),
			entry("brokered_service_context_instance_name", "an-instance-name"),
			entry("brokered_service_context_clusterid", "a-cluster-id"),
			entry("brokered_service_originating_identity_uid", "a-user-id")
		);
		assertThat(annotations).containsOnly(
			entry("brokered_service_originating_identity_username", "a-user-name"),
			entry("brokered_service_originating_identity_groups", "[\"admin\",\"dev\"]"),
			entry("brokered_service_originating_identity_extra", "{\"mydata\":[\"data1\",\"data3\"]}")
		);
	}

}