package com.orange.oss.osbcmdb.metadata;

import java.util.Map;

public class CfMetadataFormatter extends BaseMetadataFormatter {

	@Override
	protected void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
		Map<String, String> labels, String prefix) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			key = restoreOriginalOsbContextKeyNames(key);
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			Object entryValue = entry.getValue();
			String value = serializeNonStringValueToJson(key, entryValue);
			if (key.contains("_guid") || key.contains("_id")) {
				labels.put(prefixedKey, value);
			} else {
				annotations.put(prefixedKey, value);
			}
		}
	}

	/**
	 * SCOSB renames the osb context key names while parsing them using jackson camelcase mapping,
	 * see https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/12e57955170e3cdcd2523e92b40a6cf50cecf965/spring-cloud-open-service-broker-core/src/main/java/org/springframework/cloud/servicebroker/model/CloudFoundryContext.java#L48
	 */
	private String restoreOriginalOsbContextKeyNames(String key) {
		key = key.replaceFirst("Guid$", "_guid");
		return key;
	}

}
