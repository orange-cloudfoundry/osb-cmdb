package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;

/**
 * Simulates a successful synchronous backing service requested in backing space.
 *
 * Note: doesn't add behavior w.r.t. base class, only here to have explicit naming when used.
 */
public class AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor.class);

	public AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
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
			.async(true)
			.build());
	}

}
