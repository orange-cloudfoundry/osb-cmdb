package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public interface CreateBackingServicesMetadataTransformationService {

	Mono<List<BackingService>> transformMetadata(List<BackingService> backingServices,
			CreateServiceInstanceRequest request);

}
