package com.orange.oss.osbcmdb;

import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;

/**
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService}
 *
 * By default, behaves like noop
 */
public interface ServiceBindingInterceptor  {
	Logger LOG = Loggers.getLogger(ServiceInstanceInterceptor.class);

	Map<String, Object> CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop-binding-value");

	default boolean accept(CreateServiceInstanceBindingRequest request) { return isScabAcceptanceTest(request.getServiceDefinition().getName());}
	default boolean accept(DeleteServiceInstanceBindingRequest request) { return isScabAcceptanceTest(request.getServiceDefinition().getName());}

	default boolean isScabAcceptanceTest(String name) {
		boolean isTest = name.startsWith("backing-service");
		LOG.debug("Accept: isTest={} for {}", isTest, name);
		return isTest;
	}



	default Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
			.credentials(CREDENTIALS)
			.build());
	}

	default Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
	}

}
