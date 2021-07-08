package com.orange.oss.osbcmdb.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.KubernetesContext;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CreateServiceMetadataFormatterServiceTest {

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

		CreateServiceMetadataFormatterService createServiceMetadataFormatterService = new CreateServiceMetadataFormatterServiceImpl();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request, true);

		//then
		Map<String, String> annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id")
		);
		assertThat(annotations).isEmpty();
	}
	@Test
	void populates_expected_labels_and_jsonserialized_annotations_for_cf_profile() {
		populates_expected_labels_and_annotations_for_cf_profile(true);
	}
	@Test
	void populates_expected_labels_and_structured_annotations_for_cf_profile() {
		populates_expected_labels_and_annotations_for_cf_profile(false);
	}
	void populates_expected_labels_and_annotations_for_cf_profile(boolean testJsonSerializedFlatAnnotations) {
		//given
		HashMap<String, Object> organizationAnnotations = new HashMap<>();
		organizationAnnotations.put("domain.com/org-key1", "org-value1");
		organizationAnnotations.put("domain.com/org-key2", "org-value2");
		HashMap<String, Object> spaceAnnotations = new HashMap<>();
		spaceAnnotations.put("domain.com/space-key1", "space-value1");
		spaceAnnotations.put("domain.com/space-key2", "space-value2");
		HashMap<String, Object> instanceAnnotations = new HashMap<>();
		instanceAnnotations.put("domain.com/instance-key1", "instance-value1");
		instanceAnnotations.put("domain.com/instance-key2", "instance-value2");
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

		CreateServiceMetadataFormatterService createServiceMetadataFormatterService = new CreateServiceMetadataFormatterServiceImpl();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request, testJsonSerializedFlatAnnotations);

		//then
		Map annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id"),
			entry("brokered_service_context_organization_guid", "organization-guid-here"),
			entry("brokered_service_context_space_guid", "space-guid-here"),
			entry("brokered_service_originating_identity_user_id", "user-id-here")
		);
		MapEntry<String, Object> brokered_service_context_organization_annotations;
		MapEntry<String, Object> brokered_service_context_space_annotations;
		MapEntry<String, Object> brokered_service_context_instance_annotations;

		if (testJsonSerializedFlatAnnotations) {
			brokered_service_context_organization_annotations = entry(
				"brokered_service_context_organization_annotations",
				"{\"domain.com/org-key1\":\"org-value1\",\"domain.com/org-key2\":\"org-value2\"}");
			brokered_service_context_space_annotations = entry(
				"brokered_service_context_space_annotations",
				"{\"domain.com/space-key1\":\"space-value1\",\"domain.com/space-key2\":\"space-value2\"}");
			brokered_service_context_instance_annotations = entry(
				"brokered_service_context_instance_annotations",
				"{\"domain.com/instance-key1\":\"instance-value1\",\"domain.com/instance-key2\":\"instance-value2\"}");
		} else {
			Map orgAnnotationsStructure = new HashMap();
			orgAnnotationsStructure.put("domain.com/org-key1", "org-value1");
			orgAnnotationsStructure.put("domain.com/org-key2", "org-value2");
			Map spaceAnnotationsStructure = new HashMap();
			spaceAnnotationsStructure.put("domain.com/space-key1", "space-value1");
			spaceAnnotationsStructure.put("domain.com/space-key2", "space-value2");
			Map instanceAnnotationsStructure = new HashMap();
			instanceAnnotationsStructure.put("domain.com/instance-key1", "instance-value1");
			instanceAnnotationsStructure.put("domain.com/instance-key2", "instance-value2");
			brokered_service_context_organization_annotations =
				entry("brokered_service_context_organization_annotations", orgAnnotationsStructure);
			brokered_service_context_space_annotations =
				entry("brokered_service_context_space_annotations", spaceAnnotationsStructure);
			brokered_service_context_instance_annotations =
				entry("brokered_service_context_instance_annotations", instanceAnnotationsStructure);
		}
		assertThat(annotations).containsOnly(
			entry("brokered_service_context_organization_name", "organization-name-here"),
			entry("brokered_service_context_space_name", "space-name-here"),
			entry("brokered_service_context_instance_name", "instance-name-here"),
			entry("brokered_service_api_info_location", "api.my-cf.org/v2/info"),
			brokered_service_context_organization_annotations,
			brokered_service_context_space_annotations,
			brokered_service_context_instance_annotations
		);
	}


	@Test
	void populates_expected_labels_and_jsonserialized_annotations_for_kubernetes_profile() {
		populates_expected_labels_and_annotations_for_kubernetes_profile(true);
	}
	@Test
	void populates_expected_labels_and_structured_annotations_for_kubernetes_profile() {
		populates_expected_labels_and_annotations_for_kubernetes_profile(false);
	}
	void populates_expected_labels_and_annotations_for_kubernetes_profile(boolean testJsonSerializedFlatAnnotations) {
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

		CreateServiceMetadataFormatterService createServiceMetadataFormatterService = new CreateServiceMetadataFormatterServiceImpl();

		//when
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request, testJsonSerializedFlatAnnotations);

		//then
		Map annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
		assertThat(labels).containsOnly(
			entry("brokered_service_instance_guid", "service-instance-id"),
			entry("brokered_service_context_namespace", "a-namespace"),
			entry("brokered_service_context_instance_name", "an-instance-name"),
			entry("brokered_service_context_clusterid", "a-cluster-id"),
			entry("brokered_service_originating_identity_uid", "a-user-id")
		);
		MapEntry<String, Object> brokered_service_originating_identity_groups;
		MapEntry<String, Object> brokered_service_originating_identity_extra;
		if (testJsonSerializedFlatAnnotations) {
			brokered_service_originating_identity_groups = entry(
				"brokered_service_originating_identity_groups", "[\"admin\",\"dev\"]");
			brokered_service_originating_identity_extra = entry(
				"brokered_service_originating_identity_extra", "{\"mydata\":[\"data1\",\"data3\"]}");
		} else {
			brokered_service_originating_identity_groups = entry(
				"brokered_service_originating_identity_groups", jsonDeserializeArray("[\"admin\",\"dev\"]"
				));
			brokered_service_originating_identity_extra = entry(
				"brokered_service_originating_identity_extra", jsonDeserializeMap("{\"mydata\":[\"data1\",\"data3\"]}"
				));
		}
		assertThat(annotations).containsOnly(
			entry("brokered_service_originating_identity_username", "a-user-name"),
			brokered_service_originating_identity_groups,
			brokered_service_originating_identity_extra
		);
	}

	private List<String> jsonDeserializeArray(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readerForListOf(String.class).readValue(json);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private Map<String,Object> jsonDeserializeMap(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readerForMapOf(Object.class).readValue(json);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}