package com.orange.oss.osbcmdb;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

/**
 * Simulates a failed async backing service requested in backing space: create always fails, delete always succeeds
 * (synchronously)
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class ASyncFailedBackingSpaceInstanceInterceptor extends BaseBackingSpaceInstanceInterceptor implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(ASyncFailedBackingSpaceInstanceInterceptor.class);

	public ASyncFailedBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public boolean accept(CreateServiceInstanceRequest request) {
		return isScabAcceptanceTest(request.getContext(), request.toString());
	}

	@Override
	public boolean accept(GetLastServiceOperationRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString());
	}

	@Override
	public boolean accept(DeleteServiceInstanceRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString());
	}

	@Override
	public boolean accept(UpdateServiceInstanceRequest request) {
		return isScabAcceptanceTest(request.getContext(), request.toString());
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		provisionnedServiceInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(true)
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(false)
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono.just(GetLastServiceOperationResponse.builder()
			.operationState(OperationState.FAILED)
			.build());
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return Mono.error(new UnsupportedOperationException(
			"This service broker does not support retrieving service instances. " +
				"The service broker should set 'instances_retrievable:false' in the service catalog, " +
				"or provide an implementation of the fetch instance API."));
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		provisionnedServiceInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(false)
			.build());
	}

}
