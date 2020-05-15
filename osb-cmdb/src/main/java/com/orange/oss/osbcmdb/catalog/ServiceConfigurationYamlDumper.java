package com.orange.oss.osbcmdb.catalog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;

public class ServiceConfigurationYamlDumper {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final String CATALOG_DUMP_PATH = "/tmp/osb-cmdb-dynamicCatalog.yml";

	private static String TO_VALID_DRAFT_04 =
		"              parameters[$schema]: \"http://json-schema.org/draft-04/schema#\"\n" +
		"              parameters:\n";
	private static String FROM_DRAFT04 =
		"              parameters:\n" +
		"                $schema: \"http://json-schema.org/draft-04/schema#\"\n";

	private static String TO_VALID_DRAFT_06 =
		"              parameters[$schema]: \"http://json-schema.org/draft-06/schema#\"\n" +
		"              parameters:\n";
	private static String FROM_DRAFT06 =
		"              parameters:\n" +
		"                $schema: \"http://json-schema.org/draft-06/schema#\"\n";

	public String dumpToYamlString(Catalog catalog) throws JsonProcessingException {
		ObjectWriter objectWriter = getYamlWriter();
		ApplicationYaml yamlToDump = new ApplicationYaml(catalog);
		String yamlString = objectWriter.writeValueAsString(yamlToDump);
		return applyScOsbYamlWorkaround(yamlString);
	}

	/**
	 * Spring cloud osb has a specific syntax to support catalog config schemas
	 * See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/fe7cea3df1222d6acacdaec670852bf484d8aa60/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-full.yml#L76
	 */
	private String applyScOsbYamlWorkaround(String yamlString) {
		yamlString = yamlString.replace(FROM_DRAFT04, TO_VALID_DRAFT_04);
		yamlString = yamlString.replace(FROM_DRAFT06, TO_VALID_DRAFT_06);
		return yamlString;
	}

	public void dumpToYamlFile(Catalog catalog) throws IOException {
		String yamlString = dumpToYamlString(catalog);
		try (Writer writer = buildFileWriter()) {
			writer.write(yamlString);
		}
		logger.info("Dumped dynamic catalog to {}", CATALOG_DUMP_PATH);
	}

	private Writer buildFileWriter() throws IOException {
		return new FileWriter(CATALOG_DUMP_PATH, false);
	}

	private ObjectWriter getYamlWriter() {
		YAMLFactory yamlFactory = new YAMLFactory();
		//Disable yaml document header to make it easier to copy/paste. See https://www.baeldung.com/jackson-yaml
		yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
		//Try to make the output somewhat deterministic as to make diffs among version easier
		//still lacking sorting the array
		// https://www.stubbornjava.com/posts/creating-a-somewhat-deterministic-jackson-objectmapper
		objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
		objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
	return objectMapper.writer().withRootName("spring.cloud");
	}

	//	@JsonRootName("spring.cloud") //Seems to be ignored
	public static class ApplicationYaml {
		@JsonProperty("openservicebroker.catalog")
		private Catalog catalog;

		public ApplicationYaml(Catalog catalog) {
			this.catalog = catalog;
		}

		public Catalog getCatalog() { return catalog; }
		public void setCatalog(Catalog catalog) { this.catalog = catalog; }

	}


}
