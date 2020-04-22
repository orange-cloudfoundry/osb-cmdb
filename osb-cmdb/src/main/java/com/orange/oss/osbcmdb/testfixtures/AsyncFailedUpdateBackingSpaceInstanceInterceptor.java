package com.orange.oss.osbcmdb.testfixtures;

import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

/**
 * Simulates a failed sync backing service requested in backing space: create/delete always succeed, update
 * always fails (asynchronously)
 * <p>
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class AsyncFailedUpdateBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor
	implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(AsyncFailedUpdateBackingSpaceInstanceInterceptor.class);

	public AsyncFailedUpdateBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		provisionnedServiceInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(true)
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono.just(GetLastServiceOperationResponse.builder()
			.operationState(OperationState.FAILED)
			.build());
	}

}
