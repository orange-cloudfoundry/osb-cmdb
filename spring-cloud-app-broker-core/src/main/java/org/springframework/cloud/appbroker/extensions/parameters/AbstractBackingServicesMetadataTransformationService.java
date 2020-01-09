package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public class AbstractBackingServicesMetadataTransformationService {

	protected Mono<List<BackingService>> setMetadata(List<BackingService> backingServices,
		ServiceBrokerRequest request, String serviceInstanceId,
		Context context) {

		Map<String, String> annotations = new HashMap<>();
		Map<String, String> labels = new HashMap<>();

		setBrokeredServiceGuidLabel(serviceInstanceId, labels);
		setApiInfoLocationAnnotation(request, annotations);
		setContextMetadata(context, annotations, labels);
		setOriginatingIdentityMetadata(request, annotations, labels);

		for (BackingService backingService : backingServices) {
			backingService.setAnnotations(annotations);
			backingService.setLabels(labels);
		}
		return Mono.justOrEmpty(backingServices);
	}

	private void setBrokeredServiceGuidLabel(String serviceInstanceId, Map<String, String> labels) {
		labels.put("brokered_service_instance_guid", serviceInstanceId);
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

	private void setApiInfoLocationAnnotation(ServiceBrokerRequest request, Map<String, String> annotations) {
		String apiInfoLocation = request.getApiInfoLocation();
		if (apiInfoLocation !=null) {
			annotations.put("brokered_service_api_info_location", apiInfoLocation);
		}
	}

	private void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
		Map<String, String> labels, String prefix) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			key = restoreOriginalOsbContextKeyNames(key);
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			String value = entry.getValue().toString();
			if (key.contains("_guid") || key.contains("_id")) {
				labels.put(prefixedKey, value);
			} else {
				annotations.put(prefixedKey, value);
			}
		}
	}

	/**
	 * SCOSB renames the osb context key names while parsing them using jackson camelcase mapping,
	 * see https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/12e57955170e3cdcd2523e92b40a6cf50cecf965/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L48
	 */
	private String restoreOriginalOsbContextKeyNames(String key) {
		key = key.replaceFirst("Guid$", "_guid");
		return key;
	}

}
