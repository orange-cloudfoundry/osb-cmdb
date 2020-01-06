package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;

public class AbstractBackingServicesMetadataTransformationService {

	protected Mono<List<BackingService>> setMetadata(List<BackingService> backingServices,
			Map<String, Object> contextProperties) {
		Map<String, String> annotations = new HashMap<>();
		Map<String, String> labels = new HashMap<>();
		for (Map.Entry<String, Object> entry : contextProperties.entrySet()) {
			annotations.put(entry.getKey(), entry.getValue().toString());
		}
		for (BackingService backingService : backingServices) {
			backingService.setAnnotations(annotations);
			backingService.setLabels(labels);
		}
		return Mono.justOrEmpty(backingServices);
	}

}
