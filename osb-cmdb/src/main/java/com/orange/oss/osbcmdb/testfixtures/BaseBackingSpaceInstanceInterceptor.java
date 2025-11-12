package com.orange.oss.osbcmdb.testfixtures;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;

public class BaseBackingSpaceInstanceInterceptor {

	private final Logger LOG = Loggers.getLogger(this.getClass());

	protected String defaultSpaceName;

	/**
	 * Tracks provisionned<ol>
	 * <li>CSI guids in order to accept associated DSI and GetLastOperation calls which can't be
	 * 	 * filtered from space in Context (as OSB call has no Context)</li>
	 * <li>CSB guids in order to accept associated DSB</li>
	 * </ol>
	 */
	protected Set<String> provisionnedInstanceGuids = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Stores the params passed at creation or update of each entity (service instance or service binding). Indexed
	 * by entity guid.
	 */
	protected Map<String, Map<String, Object>> provisionnedInstanceParams = new HashMap<>();

	public BaseBackingSpaceInstanceInterceptor(
		String defaultSpaceName) {
		this.defaultSpaceName = defaultSpaceName;
	}

	protected boolean isScabAcceptanceTest(Context context, String requestToString, Class requestClass) {
		CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
		if (context == null) {
			LOG.info("No context specified in request, assuming not an acceptance test sending OSB request with a " +
				"compliant CF CC-NG client, therefore I'm not accepting the request");
			return false;
		}
		String spaceName = cloudFoundryContext.getSpaceName();
		if (spaceName == null) {
			LOG.info("No space specified in context {} Missing discriminant criteria to qualify as acceptance test " +
				"w.r.t. default space {}, returning false", cloudFoundryContext, defaultSpaceName);
			return false;
		}

		boolean isTest = ! spaceName.equals(defaultSpaceName);
		LOG.debug("Accept: isTest={} for interceptor={} spaceName={} requestClass={} request={}",
			isTest,
			this.getClass().getSimpleName(),
			spaceName,
			requestClass.getSimpleName(),
			requestToString);
		return isTest;
	}

	protected boolean isServiceGuidPreviousProvisionnedByUs(String serviceInstanceId, String requestToString,
		Class requestClass) {
		boolean isGuidPreviouslyProvisionnedByUs = provisionnedInstanceGuids.contains(serviceInstanceId);
		LOG.debug("Accept: isServiceGuidPreviousProvisionnedByUs={} for serviceInstanceId={} and " +
				"requestClass={} request={}",
			isGuidPreviouslyProvisionnedByUs, serviceInstanceId, requestClass.getSimpleName(), requestToString);
		return isGuidPreviouslyProvisionnedByUs;
	}

}
