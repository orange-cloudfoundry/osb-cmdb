package com.orange.oss.osbcmdb.metadata;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateServiceMetadataFormatterServiceNoOp
		implements CreateServiceMetadataFormatterService {

	@Override
	public void setMetadata(
		MetaData metaData,
		CreateServiceInstanceRequest request) {
	}

}
