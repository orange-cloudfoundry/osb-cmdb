package com.orange.oss.osbcmdb;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;

/**
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService}
 */
public interface ServiceBindingInterceptor {

	boolean accept(CreateServiceInstanceBindingRequest request);

	boolean accept(DeleteServiceInstanceBindingRequest request);

	Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request);

	Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request);

}
