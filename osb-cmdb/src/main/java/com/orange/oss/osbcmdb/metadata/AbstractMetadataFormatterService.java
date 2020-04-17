package com.orange.oss.osbcmdb.metadata;

import java.util.List;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.KubernetesContext;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public class AbstractMetadataFormatterService {

	private final Logger logger = Loggers.getLogger(this.getClass());


	private CfMetadataFormatter cfMetadataFormatter = new CfMetadataFormatter();

	private K8SMetadataFormatter k8SMetadataFormatter = new K8SMetadataFormatter();

	protected Mono<List<MetaData>> setMetadata(List<MetaData> metaData,
		ServiceBrokerRequest request, String serviceInstanceId,
		Context context) {

		logger.debug("Assigning meta-data request from request={} id={} context={}", request, serviceInstanceId,
			context);
		if (context instanceof KubernetesContext) {
			return k8SMetadataFormatter.setMetadata(metaData, request,
				serviceInstanceId, context);
		}
		else if (context instanceof CloudFoundryContext ||
			context ==null // when no context is passed, default to CloudFoundry behavior which will only set the
			// instance guid as metadata
		) {
			return cfMetadataFormatter.setMetadata(metaData, request,
				serviceInstanceId, context);
		}
		else {
			logger.warn("Unsupported OSB context type={}, skipping associated metadata", context);
			return Mono.justOrEmpty(metaData);
		}

	}

}
