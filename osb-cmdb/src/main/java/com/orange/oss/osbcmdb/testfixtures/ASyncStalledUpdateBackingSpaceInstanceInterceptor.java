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
 * Simulates a failed async backing service update requested in backing space: create/delete always succeeds sync,
 * update always hangs (asynch)
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class ASyncStalledUpdateBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor
	implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(ASyncStalledUpdateBackingSpaceInstanceInterceptor.class);

	public ASyncStalledUpdateBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(true)
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono.just(GetLastServiceOperationResponse.builder()
			.operationState(OperationState.IN_PROGRESS)
			.deleteOperation(false)
			.description(this.getClass().getSimpleName())
			.build());
	}

}
