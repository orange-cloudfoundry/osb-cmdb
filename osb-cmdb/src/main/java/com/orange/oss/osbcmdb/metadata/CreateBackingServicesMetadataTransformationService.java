package com.orange.oss.osbcmdb.metadata;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public interface CreateBackingServicesMetadataTransformationService {

	Mono<List<MetaData>> transformMetadata(List<MetaData> metaData,
		CreateServiceInstanceRequest request);

}
