package com.orange.oss.osbcmdb.testfixtures;

import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

/**
 * Simulates a failed sync backing service requested in backing space: create/delete always succeed, update
 * always fails (synchronously)
 *
 * Only accept OSB calls when space is a backing space, i.e. not the default space
 */
public class SyncFailedUpdateBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor
	implements ServiceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(SyncFailedUpdateBackingSpaceInstanceInterceptor.class);

	public SyncFailedUpdateBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		throw new ServiceBrokerException(this.getClass().getSimpleName());
	}

}
