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

	private static final Logger LOG = Loggers.getLogger(BaseBackingSpaceInstanceInterceptor.class);

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

	protected boolean isScabAcceptanceTest(Context context, String requestToString) {
		CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
		if (context == null) {
			LOG.info("No context specified in request, assuming not an acceptance test with a CF compliant client, " +
				"therefore I'm not accepting the request");
			return false;
		}
		String spaceName = cloudFoundryContext.getSpaceName();
		boolean isTest = ! spaceName.equals(defaultSpaceName);
		LOG.debug("Accept: isTest={} for spaceName={} and request={}", isTest, spaceName, requestToString);
		return isTest;
	}

	protected boolean isServiceGuidPreviousProvisionnedByUs(String serviceInstanceId, String requestToString) {
		boolean isGuidPreviouslyProvisionnedByUs = provisionnedServiceInstanceGuids.contains(serviceInstanceId);
		LOG.debug("Accept: isServiceGuidPreviousProvisionnedByUs={} for serviceInstanceId={} and request={}",
			isGuidPreviouslyProvisionnedByUs, serviceInstanceId, requestToString);
		return isGuidPreviouslyProvisionnedByUs;
	}

}
