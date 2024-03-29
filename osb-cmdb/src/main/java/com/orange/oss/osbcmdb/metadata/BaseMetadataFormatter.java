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

	protected abstract void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
			Map<String, String> labels, String prefix);

	protected MetaData setMetadata(ServiceBrokerRequest request, String serviceInstanceId, Context context) {

		Map<String, String> annotations = new HashMap<>();
		Map<String, String> labels = new HashMap<>();

		setBrokeredServiceGuidLabel(serviceInstanceId, labels);
		setApiInfoLocationAnnotation(request, annotations);
		setContextMetadata(context, annotations, labels);
		setOriginatingIdentityMetadata(request, annotations, labels);

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

	private void setContextMetadata(Context context, Map<String, String> annotations, Map<String, String> labels) {
		Map<String, Object> contextProperties =
			context == null ? new HashMap<>() : context.getProperties();
		setLabelsAndAnnotations(contextProperties, annotations, labels, "context");
	}

	private void setOriginatingIdentityMetadata(ServiceBrokerRequest request, Map<String, String> annotations,
		Map<String, String> labels) {
		Context originatingIdentity = request.getOriginatingIdentity();
		Map<String, Object> originatingIdentityProperties =
			originatingIdentity == null ? new HashMap<>() : originatingIdentity.getProperties();
		setLabelsAndAnnotations(originatingIdentityProperties, annotations, labels, "originating_identity");
	}

}
