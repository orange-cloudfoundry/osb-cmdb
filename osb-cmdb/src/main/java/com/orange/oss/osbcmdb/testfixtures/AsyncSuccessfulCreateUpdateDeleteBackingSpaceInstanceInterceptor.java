package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * Simulates a successful asynchronous provision/deprovision backing service requested in backing space.
 */
public class AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(
		AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor.class);

	public AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(true)
			.dashboardUrl(DASHBOARD_URL)
			.operation("create")
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(true)
			.operation("delete")
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono.just(GetLastServiceOperationResponse.builder()
			.description(this.getClass().getSimpleName())
			.operationState(OperationState.SUCCEEDED)
			.deleteOperation("delete".equals(request.getOperation()))
			.build());
	}



}
