package com.orange.oss.osbcmdb.metadata;

import java.util.Map;

public class K8SMetadataFormatter extends BaseMetadataFormatter {


	@Override
	protected void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
		Map<String, String> labels, String prefix) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			Object entryValue = entry.getValue();
			String value = serializeNonStringValueToJson(key, entryValue);
			if (key.equals("namespace") || key.equals("instance_name") || key.equals("clusterid")|| // plain context
				key.equals("uid") // originating identity
			) {
				labels.put(prefixedKey, value);
			} else {
				annotations.put(prefixedKey, value);
			}
		}
	}

}
