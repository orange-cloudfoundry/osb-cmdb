package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class ServiceDefinitionMapper {

	private static final Logger logger = LoggerFactory.getLogger(ServiceDefinitionMapper.class);

	private final PlanMapper planMapper;
	private ServiceDefinitionMapperProperties serviceDefinitionMapperProperties;

	public ServiceDefinitionMapper(PlanMapper planMapper,
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		this.planMapper = planMapper;
		this.serviceDefinitionMapperProperties = serviceDefinitionMapperProperties;
	}

	public ServiceDefinition toServiceDefinition(ServiceResource resource,
		List<ServicePlanResource> servicePlans) {

		ServiceEntity entity = resource.getEntity();
		return ServiceDefinition.builder()
			.id(resource.getMetadata().getId())
			.name(entity.getLabel() + serviceDefinitionMapperProperties.getSuffix())
			.description(entity.getDescription())
			.tags(safeList(entity.getTags()))
			.bindable(safeBoolean(entity.getBindable()))
			.planUpdateable(safeBoolean(entity.getPlanUpdateable()))
			.bindingsRetrievable(safeBoolean(entity.getBindingsRetrievable()))
			.instancesRetrievable(safeBoolean(entity.getInstancesRetrievable()))
			.plans(planMapper.toPlans(safeList(servicePlans)))
			.metadata(toServiceMetaData(entity.getExtra()))
			.build();
	}

	private <R> List<R> safeList(List<R> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	private Boolean safeBoolean(Boolean field) {
		if (field == null) {
			return Boolean.FALSE;
		}
		return field;
	}

	private Map<String, Object> toServiceMetaData(String extraJson) {
		if (extraJson ==null) {
			return new HashMap<>();
		}
		logger.info("extraJson {}", extraJson);
		//enforce check keys can't be mapped to other java primitives: Boolean, Integers
		//potentially customizing jackson deserialization
		// See https://www.baeldung.com/jackson-map
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		Map<String, Object> metadata = fromJson(extraJson, typeRef);
		logger.info("metadata {}", metadata);
		return metadata;
	}

	private <T> T fromJson(String json, TypeReference<HashMap<String, Object>> contentType) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readerFor(contentType).readValue(json);
		}
		catch (IOException e) {
			logger.error("Unable to parse json, caught: " + e, e);
			throw new IllegalStateException(e);
		}
	}

}
