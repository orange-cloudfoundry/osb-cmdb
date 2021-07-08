package com.orange.oss.osbcmdb.metadata;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public abstract class BaseMetadataFormatter {

	public static final String BROKERED_SERVICE_INSTANCE_GUID = "brokered_service_instance_guid";

	/**
	 * Support JSON formatting of non String values in OSB context
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	protected abstract boolean isContextKeyImmutableToQualifyAsALabel(String key);

	protected abstract String restoreOriginalOsbContextKeyNames(String key);

	protected String serializeNonStringValueToJson(String key, Object entryValue) {
		String value;
		if (entryValue instanceof String) {
			value = (String) entryValue;
		} else {
			//Serialize to Json non-string values
			try {
				value = objectMapper.writeValueAsString(entryValue);
			}
			catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Unable to serialize metadata key=" + key + " value=" + entryValue +
					" as json:" + entryValue, e);
			}
		}
		return value;
	}

	protected void setLabelsAndAnnotations(Map<String, Object> properties, Map annotations,
		Map<String, String> labels, String prefix, boolean useSerializedStringForStructures) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			key = restoreOriginalOsbContextKeyNames(key);
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			Object objectValue = entry.getValue();
			String jsonSerializedStringValue = serializeNonStringValueToJson(key, objectValue);
			if (isContextKeyImmutableToQualifyAsALabel(key)) {
				labels.put(prefixedKey, jsonSerializedStringValue);
			} else {
				if (useSerializedStringForStructures) {
					annotations.put(prefixedKey, jsonSerializedStringValue);
				} else {
					annotations.put(prefixedKey, objectValue);
				}
			}
		}
	}

	protected MetaData setMetadata(ServiceBrokerRequest request, String serviceInstanceId, Context context,
		boolean useSerializedStringForStructures) {

		Map<String, String> annotations = new HashMap<>();
		Map<String, String> labels = new HashMap<>();

		setBrokeredServiceGuidLabel(serviceInstanceId, labels);
		setApiInfoLocationAnnotation(request, annotations);
		setContextMetadata(context, annotations, labels, useSerializedStringForStructures);
		setOriginatingIdentityMetadata(request, annotations, labels, useSerializedStringForStructures);

		return MetaData.builder()
			.annotations(annotations)
			.labels(labels)
			.build();
	}

	private void setApiInfoLocationAnnotation(ServiceBrokerRequest request, Map<String, String> annotations) {
		String apiInfoLocation = request.getApiInfoLocation();
		if (apiInfoLocation !=null) {
			annotations.put("brokered_service_api_info_location", apiInfoLocation);
		}
	}

	private void setBrokeredServiceGuidLabel(String serviceInstanceId, Map<String, String> labels) {
		labels.put(BROKERED_SERVICE_INSTANCE_GUID, serviceInstanceId);
	}

	private void setContextMetadata(Context context, Map<String, String> annotations, Map<String, String> labels,
		boolean useSerializedStringForStructures) {
		Map<String, Object> contextProperties =
			context == null ? new HashMap<>() : context.getProperties();
		setLabelsAndAnnotations(contextProperties, annotations, labels, "context", useSerializedStringForStructures);
	}

	private void setOriginatingIdentityMetadata(ServiceBrokerRequest request, Map<String, String> annotations,
		Map<String, String> labels, boolean useSerializedStringForStructures) {
		Context originatingIdentity = request.getOriginatingIdentity();
		Map<String, Object> originatingIdentityProperties =
			originatingIdentity == null ? new HashMap<>() : originatingIdentity.getProperties();
		setLabelsAndAnnotations(originatingIdentityProperties, annotations, labels, "originating_identity",
			useSerializedStringForStructures);
	}

}
