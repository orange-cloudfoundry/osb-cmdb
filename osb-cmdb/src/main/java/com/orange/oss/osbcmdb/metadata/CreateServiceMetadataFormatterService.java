package com.orange.oss.osbcmdb.metadata;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public interface CreateServiceMetadataFormatterService {

	MetaData formatAsMetadata(CreateServiceInstanceRequest request, boolean useSerializedStringForStructures);

}
