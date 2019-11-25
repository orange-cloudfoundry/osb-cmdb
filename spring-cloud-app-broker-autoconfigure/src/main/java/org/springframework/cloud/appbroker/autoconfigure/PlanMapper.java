package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.Plan;

public class PlanMapper {

	private static final Logger logger = LoggerFactory.getLogger(PlanMapper.class);

	private final PlanMapperProperties properties;

	public PlanMapper(PlanMapperProperties properties) {
		this.properties = properties;
	}

	public List<Plan> toPlans(List<ServicePlanResource> servicePlans) {
		return servicePlans.stream()
			.map(this::toPlan)
			.collect(Collectors.toList());
	}

	private Plan toPlan(ServicePlanResource resource) {
		ServicePlanEntity entity = ResourceUtils.getEntity(resource);
		String jsonDump = toJson(entity);

		logger.info("plan json {}", jsonDump);
		Plan plan = fromJson(jsonDump, Plan.class);
		logger.info("plan entity {}", plan);
		return plan;
	}

	// Inspired from https://stackoverflow.com/questions/40631558/restructure-json-before-deserializing-with-jackson
	public static class CustomPlanDeserializer extends StdDeserializer<Plan> {

		private final ObjectMapper objectMapper = newMapperIgnoringUnknownProperties();

		public CustomPlanDeserializer(Class<?> vc) {
			super(vc);
		}

		@Override
		public Plan deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			final ObjectNode node = p.readValueAsTree();
			migrate(node);
			return objectMapper.treeToValue(node, Plan.class);
		}

		private void migrate(ObjectNode containerNode) throws IOException {
			JsonNode extraNode = containerNode.remove("extra");
			if (extraNode == null) {
				return;
			}
			if (!(extraNode instanceof TextNode)) {
				logger.warn("Ignoring unexpected extra node {} within {}", extraNode, containerNode);
				return;
			}
			TextNode extraTextNode = (TextNode) extraNode;
			String extraSerializedJson = extraTextNode.textValue();
			if (extraSerializedJson == null || extraSerializedJson.isEmpty()) {return;}
			JsonNode parsedExtraContent = objectMapper.readTree(extraSerializedJson);
			containerNode.set("metadata", parsedExtraContent);
		}
	}


	public <T> T fromJson(String json, Class<T> contentType) {
		try {
			ObjectMapper mapper = newMapperIgnoringUnknownProperties();
			SimpleModule module = new SimpleModule();
			module.addDeserializer(Plan.class, new CustomPlanDeserializer(Plan.class));
			mapper.registerModule(module);
			return mapper.readerFor(contentType).readValue(json);
		}
		catch (IOException e) {
			logger.error("Unable to parse json, caught: " + e, e);
			throw new IllegalStateException(e);
		}
	}

	private static ObjectMapper newMapperIgnoringUnknownProperties() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	private String toJson(Object object) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

}
