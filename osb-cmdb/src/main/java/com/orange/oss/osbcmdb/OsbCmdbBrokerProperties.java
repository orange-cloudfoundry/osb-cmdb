package com.orange.oss.osbcmdb;

public class OsbCmdbBrokerProperties {

	public static final String PROPERTY_PREFIX = "osbcmdb.broker";

	private boolean propagateMetadataAsCustomParam = true;

	public boolean isPropagateMetadataAsCustomParam() {
		return propagateMetadataAsCustomParam;
	}

	public void setPropagateMetadataAsCustomParam(boolean propagateMetadataAsCustomParam) {
		this.propagateMetadataAsCustomParam = propagateMetadataAsCustomParam;
	}

}
