package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateBackingServicesMetadataTransformationServiceNoOp
		implements CreateBackingServicesMetadataTransformationService {

	@Override
	public Mono<List<BackingService>> transformMetadata(
		List<BackingService> backingServices,
		CreateServiceInstanceRequest request) {
		return Mono.just(backingServices);
	}

}
