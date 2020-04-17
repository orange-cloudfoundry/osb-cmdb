package com.orange.oss.osbcmdb.metadata;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateBackingServicesMetadataTransformationServiceNoOp
		implements CreateBackingServicesMetadataTransformationService {

	@Override
	public Mono<List<MetaData>> transformMetadata(
		List<MetaData> metaData,
		CreateServiceInstanceRequest request) {
		return Mono.just(metaData);
	}

}
