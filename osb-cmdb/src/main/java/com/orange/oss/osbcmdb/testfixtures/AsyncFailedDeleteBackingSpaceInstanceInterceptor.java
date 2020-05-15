package com.orange.oss.osbcmdb.testfixtures;

import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * Simulates a failed sync backing service requested in backing space: create/update always succeed, delete
 * always fails (asynchronously)
 * <p>
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class AsyncFailedDeleteBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor
	implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(AsyncFailedDeleteBackingSpaceInstanceInterceptor.class);

	public AsyncFailedDeleteBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(true)
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono.just(GetLastServiceOperationResponse.builder()
			.operationState(OperationState.FAILED)
			.description(this.getClass().getSimpleName())
			.build());
	}

}
