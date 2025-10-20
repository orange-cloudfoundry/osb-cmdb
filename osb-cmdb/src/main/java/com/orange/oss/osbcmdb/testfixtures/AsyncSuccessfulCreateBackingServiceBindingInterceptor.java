package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * Simulates a successful async binding backing service requested in backing space:
 * - rejects sync binding create/delete requests,
 * - create/delete async binding completes successfully
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class AsyncSuccessfulCreateBackingServiceBindingInterceptor extends  BackingServiceBindingInterceptor {

	public AsyncSuccessfulCreateBackingServiceBindingInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		if (! request.isAsyncAccepted()) {
			throw new ServiceBrokerAsyncRequiredException("AsyncSuccessfulCreateBackingServiceBindingInterceptor is " +
				"expecting accept_incomplete=true ");
		}
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
			.async(true)
			.operation("create")
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		if (! request.isAsyncAccepted()) {
			throw new ServiceBrokerAsyncRequiredException("AsyncSuccessfulCreateBackingServiceBindingInterceptor is " +
				"expecting accept_incomplete=true ");
		}
		return Mono.just(DeleteServiceInstanceBindingResponse.builder()
			.async(true)
			.operation("delete")
			.build());
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
		GetLastServiceBindingOperationRequest request) {
		return Mono.just(GetLastServiceBindingOperationResponse.builder()
			.description(this.getClass().getSimpleName())
			.operationState(OperationState.SUCCEEDED)
			.deleteOperation("delete".equals(request.getOperation()))
			.build());
	}


}
