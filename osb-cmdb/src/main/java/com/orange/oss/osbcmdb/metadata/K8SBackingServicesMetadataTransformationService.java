package com.orange.oss.osbcmdb.metadata;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class K8SBackingServicesMetadataTransformationService extends BaseBackingServicesMetadataTransformationService {

	private ObjectMapper objectMapper = new ObjectMapper();


	@Override
	protected void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
		Map<String, String> labels, String prefix) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			String value;
			if (entry.getValue() instanceof String) {
				value = (String) entry.getValue();
			} else {
				//Serialize to Json non-string values
				try {
					value = objectMapper.writeValueAsString(entry.getValue());
				}
				catch (JsonProcessingException e) {
					throw new IllegalArgumentException("Unable to serialize metadata key=" + key  + " value=" + entry.getValue() +
						" as json:" + entry.getValue(), e);
				}
			}
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
