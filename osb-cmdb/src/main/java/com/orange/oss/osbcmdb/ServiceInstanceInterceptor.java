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
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;

/**
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceService}. Can't reuse it directly as SCOSB
 * autoconfiguration expects a single bean implementing the interface.
 *
 * By default, behaves like noop.
 */
public interface ServiceInstanceInterceptor  {
	Logger LOG = Loggers.getLogger(ServiceInstanceInterceptor.class);

	default boolean accept(CreateServiceInstanceRequest request) { return isScabAcceptanceTest(request.getServiceDefinition().getName());}
	default boolean accept(GetLastServiceOperationRequest request) { return isScabAcceptanceTest(request.getServiceDefinitionId());}
	default boolean accept(DeleteServiceInstanceRequest request) { return isScabAcceptanceTest(request.getServiceDefinition().getName());}
	default boolean accept(UpdateServiceInstanceRequest request) { return isScabAcceptanceTest(request.getServiceDefinition().getName());}

	default boolean isScabAcceptanceTest(String name) {
		boolean isTest = name.startsWith("backing-service");
		LOG.debug("Accept: isTest={} for {}", isTest, name);
		return isTest;
	}

	default Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(false)
			.build());
	}


	default Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(false)
			.build());
	}


	default Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(false)
			.build());
	}

	default Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return Mono.error(new UnsupportedOperationException(
			"This service broker does not support retrieving service instances. " +
				"The service broker should set 'instances_retrievable:false' in the service catalog, " +
				"or provide an implementation of the fetch instance API."));
	}

	default Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono
			.error(new UnsupportedOperationException("This service broker does not support getting the status of " +
				"an asynchronous operation. " +
				"If the service broker returns '202 Accepted' in response to a provision, update, or deprovision" +
				"request, it must also provide an implementation of the get last operation API."));
	}

}
