package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;

/**
 * Simulates a successful async binding backing service requested in backing space:
 * - rejects sync binding create/delete requests,
 * - create/delete async binding completes successfully
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class AsyncOnlyBackingServiceBindingInterceptor extends  BackingServiceBindingInterceptor {

	public AsyncOnlyBackingServiceBindingInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		if (! request.isAsyncAccepted()) {
			throw new ServiceBrokerAsyncRequiredException("AsyncOnlyBackingServiceBindingInterceptor is " +
				"expecting accept_incomplete=true (rejects osb-cmdb sync binding create requests");

		}
		return super.createServiceInstanceBinding(request);
	}

}
