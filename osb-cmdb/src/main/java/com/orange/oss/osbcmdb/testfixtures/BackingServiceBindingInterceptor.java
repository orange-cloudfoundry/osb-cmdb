package com.orange.oss.osbcmdb.testfixtures;

import java.util.Collections;
import java.util.Map;

import com.orange.oss.osbcmdb.servicebinding.ServiceBindingInterceptor;
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
public class BackingServiceBindingInterceptor extends BaseBackingSpaceInstanceInterceptor implements
	ServiceBindingInterceptor {
	public static final Logger LOG = Loggers.getLogger(BackingServiceBindingInterceptor.class);

	public static final Map<String, Object> CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop-binding-value");

	public BackingServiceBindingInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public boolean accept(CreateServiceInstanceBindingRequest request) { return isScabAcceptanceTest(request.getContext(),
		request.toString());}
	@Override
	public boolean accept(DeleteServiceInstanceBindingRequest request) { return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(),
		request.toString());}


	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
			.credentials(CREDENTIALS)
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
	}

}
