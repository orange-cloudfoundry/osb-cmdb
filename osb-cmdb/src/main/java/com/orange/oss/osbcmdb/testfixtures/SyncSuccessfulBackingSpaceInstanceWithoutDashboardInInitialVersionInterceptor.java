package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

/**
 * Simulates a successful synchronous backing service requested in backing space that only returns dashboard urls
 * when a maintenance info object is received and non empty.
 * This supports testing `cf update-service --upgrade` use case
 */
public class SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(
		SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor.class);

	public SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		boolean maintenanceInfoReceived = (request.getMaintenanceInfo() != null);
		if (maintenanceInfoReceived) {
			//serve a dashboard url
			return super.createServiceInstance(request);
		} else {
			//serve no dashboard url
			super.createServiceInstance(request);
			return Mono.just(CreateServiceInstanceResponse.builder()
				.async(false)
				.dashboardUrl(null)
				.build());
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		boolean maintenanceInfoReceived = (request.getMaintenanceInfo() != null);
		//noinspection ConstantConditions
		if (maintenanceInfoReceived) {
			//serve a dashboard url
			return super.updateServiceInstance(request);
		} else {
			//serve no dashboard url
			super.updateServiceInstance(request);
			return Mono.just(UpdateServiceInstanceResponse.builder()
				.async(false)
				.dashboardUrl(null)
				.build());
		}
	}

}
