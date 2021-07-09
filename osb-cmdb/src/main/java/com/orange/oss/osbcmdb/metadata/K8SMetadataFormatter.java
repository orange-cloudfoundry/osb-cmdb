package com.orange.oss.osbcmdb.metadata;

public class K8SMetadataFormatter extends BaseMetadataFormatter {


	@Override
	protected boolean isContextKeyImmutableToQualifyAsALabel(String key) {
		return key.equals("namespace") || key.equals("instance_name") || key.equals("clusterid") || // plain context
			key.equals("uid"); // originating identity
	}


	/**
	 * SCOSB renames the osb context key names while parsing them using jackson camelcase mapping,
	 * see https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/12e57955170e3cdcd2523e92b40a6cf50cecf965/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L48
	 *
	 * However K8SContext does not have this problem. So impl is a nooop to avoid undesired side effects
	 */
	@Override
	protected String restoreOriginalOsbContextKeyNames(String key) {
		return key;
	}

}
