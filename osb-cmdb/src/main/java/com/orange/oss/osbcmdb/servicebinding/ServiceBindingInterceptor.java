package com.orange.oss.osbcmdb.servicebinding;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;

/**
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService}
 */
public interface ServiceBindingInterceptor {

	boolean accept(CreateServiceInstanceBindingRequest request);

	boolean accept(GetLastServiceBindingOperationRequest request);

	boolean accept(GetServiceInstanceBindingRequest request);

	boolean accept(DeleteServiceInstanceBindingRequest request);

	Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request);

	Mono<GetLastServiceBindingOperationResponse> getLastOperation(GetLastServiceBindingOperationRequest request);

	Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request);

	Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request);

}
