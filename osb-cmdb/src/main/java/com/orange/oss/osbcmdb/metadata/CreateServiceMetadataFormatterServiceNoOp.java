package com.orange.oss.osbcmdb.metadata;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateServiceMetadataFormatterServiceNoOp
		implements CreateServiceMetadataFormatterService {

	@Override
	public MetaData formatAsMetadata(
		CreateServiceInstanceRequest request, boolean useSerializedStringForStructures) {
		return null;
	}

}
