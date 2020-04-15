package com.orange.oss.osbcmdb;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

/**
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceService}. Can't reuse it directly as SCOSB
 * autoconfiguration expects a single bean implementing the interface.
 */
public interface ServiceInstanceInterceptor {

	boolean accept(CreateServiceInstanceRequest request);

	boolean accept(GetLastServiceOperationRequest request);

	boolean accept(DeleteServiceInstanceRequest request);

	boolean accept(UpdateServiceInstanceRequest request);

	Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request);

	Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request);

	Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request);

	Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request);

	Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request);

}
