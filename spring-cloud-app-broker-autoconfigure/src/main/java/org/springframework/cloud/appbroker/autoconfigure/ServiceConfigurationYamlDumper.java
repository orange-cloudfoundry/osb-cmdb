package org.springframework.cloud.appbroker.autoconfigure;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;

public class ServiceConfigurationYamlDumper {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final String CATALOG_DUMP_PATH = "/tmp/osb-cmdb-dynamicCatalog.yml";

	public String dumpToYamlString(Catalog catalog, BrokeredServices brokeredServices) throws JsonProcessingException {
		ObjectWriter objectWriter = getYamlWriter();
		ApplicationYaml yamlToDump = new ApplicationYaml(catalog, brokeredServices);
		return objectWriter.writeValueAsString(yamlToDump);
	}

	public void dumpToYamlFile(Catalog catalog, BrokeredServices brokeredServices) throws IOException {
		ObjectWriter objectWriter = getYamlWriter();
		ApplicationYaml yamlToDump = new ApplicationYaml(catalog, brokeredServices);
		Writer writer= buildFileWriter();
		objectWriter.writeValue(writer, yamlToDump);
		logger.info("Dumped dynamic catalog to {}", CATALOG_DUMP_PATH);
	}

	private Writer buildFileWriter() throws IOException {
		return new FileWriter(CATALOG_DUMP_PATH, false);
	}

	private ObjectWriter getYamlWriter() {
		YAMLFactory yamlFactory = new YAMLFactory();
		ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
		return objectMapper.writer().withRootName("spring.cloud");
	}

	//	@JsonRootName("spring.cloud") //Seems to be ignored
	public static class ApplicationYaml {
		@JsonProperty("appbroker.services")
		private BrokeredServices brokeredServices;
		@JsonProperty("openservicebroker.catalog")
		private Catalog catalog;

		public ApplicationYaml(Catalog catalog,
			BrokeredServices brokeredServices) {
			this.brokeredServices = brokeredServices;
			this.catalog = catalog;
		}

		public BrokeredServices getBrokeredServices() { return brokeredServices; }
		public void setBrokeredServices(BrokeredServices brokeredServices) { this.brokeredServices = brokeredServices; }
		public Catalog getCatalog() { return catalog; }
		public void setCatalog(Catalog catalog) { this.catalog = catalog; }

	}


}
