package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceConfigurationYamlDumperTest extends SampleServicesBuilderBaseTest {

	@Test
	void dumpsBrokeredServicesToYamlStringAndDisk() throws IOException {
		//given
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql", "10mb", "20mb"),
				buildServiceDefinition("noop", "default")))
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("mysql", "10mb"))
			.service(buildBrokeredService("mysql", "20mb"))
			.service(buildBrokeredService("noop", "default"))
			.build();

		//when
		ServiceConfigurationYamlDumper serviceConfigurationYamlDumper = new ServiceConfigurationYamlDumper();
		String applicationYml = serviceConfigurationYamlDumper.dumpToYamlString(catalog, brokeredServices);

		//then
		String expectedYaml
			= "---\n" +
			"spring.cloud:\n" +
			"  appbroker.services:\n" +
			"  - serviceName: \"mysql\"\n" +
			"    planName: \"10mb\"\n" +
			"    apps: null\n" +
			"    services:\n" +
			"    - serviceInstanceName: \"mysql\"\n" +
			"      name: \"mysql\"\n" +
			"      plan: \"10mb\"\n" +
			"      parameters: {}\n" +
			"      properties: {}\n" +
			"      parametersTransformers: []\n" +
			"      rebindOnUpdate: false\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceDefinition\"\n" +
			"  - serviceName: \"mysql\"\n" +
			"    planName: \"20mb\"\n" +
			"    apps: null\n" +
			"    services:\n" +
			"    - serviceInstanceName: \"mysql\"\n" +
			"      name: \"mysql\"\n" +
			"      plan: \"20mb\"\n" +
			"      parameters: {}\n" +
			"      properties: {}\n" +
			"      parametersTransformers: []\n" +
			"      rebindOnUpdate: false\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceDefinition\"\n" +
			"  - serviceName: \"noop\"\n" +
			"    planName: \"default\"\n" +
			"    apps: null\n" +
			"    services:\n" +
			"    - serviceInstanceName: \"noop\"\n" +
			"      name: \"noop\"\n" +
			"      plan: \"default\"\n" +
			"      parameters: {}\n" +
			"      properties: {}\n" +
			"      parametersTransformers: []\n" +
			"      rebindOnUpdate: false\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceDefinition\"\n" +
			"  openservicebroker.catalog:\n" +
			"    services:\n" +
			"    - id: \"mysql-id\"\n" +
			"      name: \"mysql\"\n" +
			"      description: \"description mysql\"\n" +
			"      bindable: false\n" +
			"      plans:\n" +
			"      - id: \"10mb-id\"\n" +
			"        name: \"10mb\"\n" +
			"        description: \"description 10mb\"\n" +
			"        free: true\n" +
			"      - id: \"20mb-id\"\n" +
			"        name: \"20mb\"\n" +
			"        description: \"description 20mb\"\n" +
			"        free: true\n" +
			"    - id: \"noop-id\"\n" +
			"      name: \"noop\"\n" +
			"      description: \"description noop\"\n" +
			"      bindable: false\n" +
			"      plans:\n" +
			"      - id: \"default-id\"\n" +
			"        name: \"default\"\n" +
			"        description: \"description default\"\n" +
			"        free: true\n";
		assertThat(applicationYml).isEqualTo(expectedYaml);

		//and when
		serviceConfigurationYamlDumper.dumpToYamlFile(catalog, brokeredServices);

		//then
		String readYamlFromFile = new String(
			Files.readAllBytes(
				FileSystems.getDefault().getPath(
					ServiceConfigurationYamlDumper.CATALOG_DUMP_PATH)));
		assertThat(readYamlFromFile).isEqualTo(expectedYaml);
	}


}