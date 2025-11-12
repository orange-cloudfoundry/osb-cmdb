package com.orange.oss.osbcmdb.testfixtures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
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

	public static final String CREATE = "create";

	public static final String DELETE = "delete";

	private static final Logger LOG = LoggerFactory.getLogger(AsyncSuccessfulCreateBackingServiceBindingInterceptor.class);

	
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
		CreateServiceInstanceAppBindingResponse response = CreateServiceInstanceAppBindingResponse.builder()
			.async(true)
			.operation(CREATE)
			.build();
		LOG.info("Returning async response: CreateServiceInstanceAppBindingResponse={}", response);
		return Mono.just(response);
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
			.operation(DELETE)
			.build());
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
		GetLastServiceBindingOperationRequest request) {

		if (DELETE.equals(request.getOperation())) {
			//Clean up guid (although a leak has no impact for the interceptor just used once)
			provisionnedInstanceGuids.remove(request.getServiceInstanceId());
		}
		return Mono.just(GetLastServiceBindingOperationResponse.builder()
			.description(this.getClass().getSimpleName())
			.operationState(OperationState.SUCCEEDED)
			.deleteOperation(DELETE.equals(request.getOperation()))
			.build());
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		return Mono.just(GetServiceInstanceAppBindingResponse.builder()
			.credentials(CREDENTIALS)
			.build());
	}
}
