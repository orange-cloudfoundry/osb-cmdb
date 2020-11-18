package com.orange.oss.osbcmdb.testfixtures;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

/**
 * This service rejects any provision/update/delete which is requested with accept_incomplete=true
 */
public class SyncOnlyBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(SyncOnlyBackingSpaceInstanceInterceptor.class);

	public SyncOnlyBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		if (request.isAsyncAccepted()) {
			throw new ServiceBrokerAsyncRequiredException("SyncOnlyBackingSpaceInstanceInterceptor is " +
				"expecting accept_incomplete=false and abusingly returning 422 error code in this case");
		}
		return super.createServiceInstance(request);
	}

}
