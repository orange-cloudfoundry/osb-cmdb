package com.orange.oss.osbcmdb.testfixtures;

import java.util.Collections;
import java.util.Map;

import com.orange.oss.osbcmdb.servicebinding.ServiceBindingInterceptor;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
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
 * Supports intercepting OSB service provisionning calls, mainly for acceptance test purposes. Reuses prototypes from
 * {@link org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService}
 * <p>
 * By default, behaves like SYNC-ONLY noop
 */
public class BackingServiceBindingInterceptor extends BaseBackingSpaceInstanceInterceptor implements
	ServiceBindingInterceptor {

	public static final Map<String, Object> CREDENTIALS = Collections
		.singletonMap("noop-binding-key", "noop-binding-value");

	public static final Logger LOG = Loggers.getLogger(BackingServiceBindingInterceptor.class);

	public BackingServiceBindingInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public boolean accept(CreateServiceInstanceBindingRequest request) {
		return isScabAcceptanceTest(request.getContext(), request.toString(), request.getClass());
	}

	@Override
	public boolean accept(GetLastServiceBindingOperationRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString(),
			request.getClass());
	}

	@Override
	public boolean accept(GetServiceInstanceBindingRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString(),
			request.getClass());
	}

	@Override
	public boolean accept(DeleteServiceInstanceBindingRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString(),
			request.getClass());
	}


	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		CreateServiceInstanceAppBindingResponse response = CreateServiceInstanceAppBindingResponse.builder()
			.async(false)
			.credentials(CREDENTIALS)
			.build();
		LOG.info("Retuning {}", response);
		return Mono.just(response);
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
		GetLastServiceBindingOperationRequest request) {
		return Mono.error(new ServiceBrokerException("interceptor returnes sync responses, unexpected " +
			"getLastOperation request"));
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		return Mono.error(new ServiceBrokerException("interceptor returned sync binding, unexpected " +
			"getServiceInstanceBinding request"));
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		DeleteServiceInstanceBindingResponse response = DeleteServiceInstanceBindingResponse.builder()
			.async(false)
			.build();
		LOG.info("Retuning {}", response);
		return Mono.just(response);
	}

}
