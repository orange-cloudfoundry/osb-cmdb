package com.orange.oss.osbcmdb.metadata;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateServiceMetadataFormatterServiceNoOp
		implements CreateServiceMetadataFormatterService {

	@Override
	public Mono<List<MetaData>> setMetadata(
		List<MetaData> metaData,
		CreateServiceInstanceRequest request) {
		return Mono.just(metaData);
	}

}
