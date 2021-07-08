package com.orange.oss.osbcmdb.metadata;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.KubernetesContext;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public class AbstractMetadataFormatterService {

	private final Logger logger = Loggers.getLogger(this.getClass());


	private final CfMetadataFormatter cfMetadataFormatter = new CfMetadataFormatter();

	private final K8SMetadataFormatter k8SMetadataFormatter = new K8SMetadataFormatter();

	protected MetaData setMetadata(ServiceBrokerRequest request, String serviceInstanceId, Context context,
		boolean useSerializedStringForStructures) {

		logger.debug("Assigning meta-data request from request={} id={} context={}", request, serviceInstanceId,
			context);
		if (context instanceof KubernetesContext) {
			return k8SMetadataFormatter.setMetadata(request, serviceInstanceId, context, useSerializedStringForStructures);
		}
		else if (context instanceof CloudFoundryContext ||
			context ==null // when no context is passed, default to CloudFoundry behavior which will only set the
			// instance guid as metadata
		) {
			return cfMetadataFormatter.setMetadata(request, serviceInstanceId, context,
				useSerializedStringForStructures);
		}
		else {
			logger.warn("Unsupported OSB context type={}, skipping associated metadata", context);
			return null;
		}

	}

}
