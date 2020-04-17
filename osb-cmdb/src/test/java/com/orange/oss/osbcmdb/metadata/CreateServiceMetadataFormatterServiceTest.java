package com.orange.oss.osbcmdb.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.KubernetesContext;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CreateServiceMetadataFormatterServiceTest {

	@Test
	void populates_expected_labels_and_annotations_for_null_profile() {
		//given
		MetaData metaData1 = MetaData
			.builder()
			.build();
		List<MetaData> backingServices = singletonList(metaData1);

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
		createServiceMetadataFormatterService.setMetadata(backingServices, request);

		//then
		MetaData metaData = backingServices.get(0);
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
		MetaData metaData1 = MetaData
			.builder()
			.build();
		List<MetaData> backingServices = singletonList(metaData1);

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
				.build())
			.originatingIdentity(CloudFoundryContext.builder()
				.property("user_id", "user-id-here")
				.build())
			.apiInfoLocation("api.my-cf.org/v2/info")
			.build();

		CreateServiceMetadataFormatterService createServiceMetadataFormatterService = new CreateServiceMetadataFormatterServiceImpl();

		//when
		createServiceMetadataFormatterService.setMetadata(backingServices, request);

		//then
		MetaData metaData = backingServices.get(0);
		Map<String, String> annotations = metaData.getAnnotations();
		Map<String, String> labels = metaData.getLabels();
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


	@Test
	void populates_expected_labels_and_annotations_for_kubernetes_profile() {
		//given
		MetaData metaData1 = MetaData
			.builder()
			.build();
		List<MetaData> backingServices = singletonList(metaData1);

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
		createServiceMetadataFormatterService.setMetadata(backingServices, request);

		//then
		MetaData metaData = backingServices.get(0);
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