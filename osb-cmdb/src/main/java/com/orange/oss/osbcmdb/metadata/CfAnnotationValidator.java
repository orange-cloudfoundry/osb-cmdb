package com.orange.oss.osbcmdb.metadata;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;

import static com.orange.oss.osbcmdb.metadata.CfMetadataFormatter.BROKERED_SERVICE_CONTEXT_ORANGE;

/**
 * This class attempts to catch most obvious incompatibilities between user defined annotations and accepted range in
 * labels. The full implementation on Cf CC_NG side is too costly for us to replicated
 * See https://github.com/cloudfoundry/cloud_controller_ng/blob/4a23a419862ef9b7272f411a4435e33fc7305f30/app/messages/metadata_validator_helper.rb#L6
 */
public class CfAnnotationValidator {

		//X{n,m}: X, at least n but not more than m times
	public static final String LABEL_KEY_REGEX = "[a-z0-9A-Z\\-_\\.]{1,63}";
	public static final String LABEL_VALUE_REGEX = "[a-z0-9A-Z\\-_\\.]{0,63}";

	public void validateOrangeAnnotationCanBeIndexedAsALabel(String userDefinedAnnotationKey,
		String userDefinedAnnotationValue,
		String trimmedKey) {
		//See https://docs.cloudfoundry.org/adminguide/metadata.html#reqs
		String wrappedKey = BROKERED_SERVICE_CONTEXT_ORANGE + trimmedKey;
		if (!trimmedKey.matches(LABEL_KEY_REGEX)) {
			throw new ServiceBrokerInvalidParametersException("Annotation key \"" + userDefinedAnnotationKey + "\" " +
				"can not be indexed in osb-cmdb as a label \"" + wrappedKey + "\" due" +
				" to violations to regex " +
				":" + LABEL_KEY_REGEX + " (please check maxsize for annotation key =" + (63- BROKERED_SERVICE_CONTEXT_ORANGE.length()) +
				" chars)");
		}
		if (!userDefinedAnnotationValue.matches(LABEL_VALUE_REGEX)) {
			throw new ServiceBrokerInvalidParametersException("Annotation key \"" + userDefinedAnnotationKey + "\" " +
				"with value \"" + userDefinedAnnotationValue + "\" " +
				"can not be indexed in osb-cmdb as a label \"" + wrappedKey + "\" due to value violation to regex " +
				":" + LABEL_VALUE_REGEX);
		}
	}

}
