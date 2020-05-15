package com.orange.oss.osbcmdb.testfixtures;

import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

/**
 * Simulates a hanging sync backing service requested in backing space: create always hangs for 2 mins (CC API
 * default timeout being 60s), delete always succeeds
 * (synchronously)
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class SyncTimeoutCreateBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor
	implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(SyncTimeoutCreateBackingSpaceInstanceInterceptor.class);

	public SyncTimeoutCreateBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		LOG.info("Sleeping for 2 mins...");
		try {
			//noinspection BlockingMethodInNonBlockingContext
			Thread.sleep(2*60*1000);
		}
		catch (InterruptedException e) {
			LOG.error("2 mins sleep interrupted", e);
		}
		LOG.info("Sleeping for 2 mins...Done");
		return super.createServiceInstance(request);
	}


}
