package com.orange.oss.osbcmdb.metadata;

public class CfMetadataFormatter extends BaseMetadataFormatter {

		@Override
	protected boolean isContextKeyImmutableToQualifyAsALabel(String key) {
		return key.contains("_guid") || key.contains("_id");
	}

	/**
	 * SCOSB renames the osb context key names while parsing them using jackson camelcase mapping,
	 * see https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/12e57955170e3cdcd2523e92b40a6cf50cecf965/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L48
	 */
	@Override
	protected String restoreOriginalOsbContextKeyNames(String key) {
		key = key.replaceFirst("Guid$", "_guid");
		return key;
	}

}
