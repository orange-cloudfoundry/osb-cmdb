package com.orange.oss.osbcmdb;

public class OsbCmdbBrokerProperties {

	public static final String PROPERTY_PREFIX = "osbcmdb.broker";

	/**
	 * Set to false if a service broker rejects CSI/USI requests with unknown param.
	 * This is however a flag global to all brokered services for now.
	 */
	private boolean propagateMetadataAsCustomParam = true;

	/**
	 * Mostly used to true.
	 * Set to false in order to support acceptance tests for custom metadata param that need to
	 * fetch it.
	 */
	private boolean hideMetadataCustomParamInGetServiceInstanceEndpoint = true;


	/**
	 * Turn on to inspect X-Api-Info-Location to detect configuration mistakes from operator
	 */
	private boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader = false;

	/**
	 * Expected X-Api-Info-Location when {@link #rejectRequestsWithNonMatchingXApiInfoLocationHeader} is set
	 * e.g."api.example.com/v2/info"
	 */
	private String expectedXApiInfoLocationHeader = null;


	public boolean isHideMetadataCustomParamInGetServiceInstanceEndpoint() {
		return hideMetadataCustomParamInGetServiceInstanceEndpoint;
	}

	public boolean isPropagateMetadataAsCustomParam() {
		return propagateMetadataAsCustomParam;
	}

	public void setHideMetadataCustomParamInGetServiceInstanceEndpoint(
		boolean hideMetadataCustomParamInGetServiceInstanceEndpoint) {
		this.hideMetadataCustomParamInGetServiceInstanceEndpoint = hideMetadataCustomParamInGetServiceInstanceEndpoint;
	}

	public void setPropagateMetadataAsCustomParam(boolean propagateMetadataAsCustomParam) {
		this.propagateMetadataAsCustomParam = propagateMetadataAsCustomParam;
	}

	public String getExpectedXApiInfoLocationHeader() {
		return expectedXApiInfoLocationHeader;
	}

	public void setExpectedXApiInfoLocationHeader(String expectedXApiInfoLocationHeader) {
		this.expectedXApiInfoLocationHeader = expectedXApiInfoLocationHeader;
	}

	public boolean isRejectRequestsWithNonMatchingXApiInfoLocationHeader() {
		return rejectRequestsWithNonMatchingXApiInfoLocationHeader;
	}

	public void setRejectRequestsWithNonMatchingXApiInfoLocationHeader(
		boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader) {
		this.rejectRequestsWithNonMatchingXApiInfoLocationHeader = rejectRequestsWithNonMatchingXApiInfoLocationHeader;
	}

}
