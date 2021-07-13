package com.orange.oss.osbcmdb.metadata;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

public class CfMetadataFormatter extends BaseMetadataFormatter {

	public static final String BROKERED_SERVICE_CONTEXT_ORANGE = "brokered_service_context_orange_";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private CfAnnotationValidator annotationValidator;

	public CfMetadataFormatter(CfAnnotationValidator annotationValidator) {
		this.annotationValidator = annotationValidator;
	}

	@Override
	protected void setLabelsAndAnnotations(Map<String, Object> properties, Map<String, String> annotations,
		Map<String, String> labels, String prefix) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			key = restoreOriginalOsbContextKeyNames(key);
			String prefixedKey= "brokered_service_"+ prefix +"_" + key;
			Object entryValue = entry.getValue();
			String value = serializeNonStringValueToJson(key, entryValue);
			if (isContextKeyImmutableToQualifyAsALabel(key)) {
				labels.put(prefixedKey, value);
			} else {
				annotations.put(prefixedKey, value);
			}
		}
		labels.putAll(extractPrecedentedAnnotations(properties));
	}

	private Map<String, String> extractPrecedentedAnnotations(Map<String, Object> properties) {
		Map<String,String> whiteListedPrecedenceAnnotations = new HashMap<>();

		//Process in sequence from lower priority to high priority
		for (String propertyKey : asList("organization_annotations", "space_annotations", "instance_annotations")) {
			processAndInsertWhiteListedPrecedentedAnnotations(properties, propertyKey, whiteListedPrecedenceAnnotations);
		}
		return whiteListedPrecedenceAnnotations;
	}

	private void processAndInsertWhiteListedPrecedentedAnnotations(Map<String, Object> properties, String entryKey,
		Map<String, String> whiteListedPrecedenceAnnotations) {

		Object entryValue = properties.get(entryKey);
		if (entryValue == null) {
			// The OSB client isn't filling this optional CF profile annotation, proceeding normally
			return;
		}
		if (!(entryValue instanceof Map)) {
			logger.error("Unexpected type received for context property key {} expected a map, got {} Skipping",
				entryKey,	entryValue);
			//See OSB spec https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object
			//This OPTIONAL property holds an object with the annotations key/value pairs. If present, this property MUST be an object, with zero or more properties as follows:
			throw new RuntimeException("Unexpected type received for context property key " + entryKey + " expected a" +
				" map, got:" + entryValue.getClass() );
		}
		Map <String, String> annotationMap = (Map<String, String>) entryValue;
		for (Map.Entry<String, String> annotationEntry : annotationMap.entrySet()) {
			String annotationKey = annotationEntry.getKey();
			String annotationValue = annotationEntry.getValue();

			final String WHITE_LISTED_ANNOTATIONS_PREFIX = "orange.com/";
			if (annotationKey.startsWith(WHITE_LISTED_ANNOTATIONS_PREFIX)) {
				String trimmedKey = annotationKey.substring(WHITE_LISTED_ANNOTATIONS_PREFIX.length());
				String prefixedTrimmedKey=
					BROKERED_SERVICE_CONTEXT_ORANGE + trimmedKey;
				annotationValidator.validateOrangeAnnotationCanBeIndexedAsALabel(annotationKey, annotationValue, trimmedKey);
				whiteListedPrecedenceAnnotations.put(prefixedTrimmedKey, annotationValue);
			}
		}
	}

	private boolean isContextKeyImmutableToQualifyAsALabel(String key) {
		return key.contains("_guid") || key.contains("_id");
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
