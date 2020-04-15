package com.orange.oss.osbcmdb;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;


/**
 * Base class to accept Backing services requested in backing space. Only accept OSB calls when space is a
 * backing space, i.e. not the default space
 */
public class BaseBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(BackingSpaceInstanceInterceptor.class);

	protected String defaultSpaceName;

	/**
	 * Tracks provisionned CSI guids in order to accept associated DSI and GetLastOperation calls which can't be
	 * filtered from space in Context (as OSB call has no Context)
	 */
	protected Set<String> provisionnedServiceInstanceGuids = Collections.synchronizedSet(new HashSet<>());

	public BaseBackingSpaceInstanceInterceptor(
		String defaultSpaceName) {
		this.defaultSpaceName = defaultSpaceName;
	}

	protected boolean isScabAcceptanceTest(Context context) {
		CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
		String spaceName = cloudFoundryContext.getSpaceName();
		boolean isTest = ! spaceName.equals(defaultSpaceName);
		LOG.debug("Accept: isTest={} for spaceName={}", isTest, spaceName);
		return isTest;
	}

	protected boolean isServiceGuidPreviousProvisionnedByUs(String serviceInstanceId) {
		return provisionnedServiceInstanceGuids.contains(serviceInstanceId);
	}

}
