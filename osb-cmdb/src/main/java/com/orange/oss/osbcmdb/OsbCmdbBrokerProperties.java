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

}
