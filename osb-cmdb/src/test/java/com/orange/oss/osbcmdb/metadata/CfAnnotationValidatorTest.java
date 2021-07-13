package com.orange.oss.osbcmdb.metadata;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;

import static com.orange.oss.osbcmdb.metadata.CfMetadataFormatter.BROKERED_SERVICE_CONTEXT_ORANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CfAnnotationValidatorTest {

	CfAnnotationValidator validator = new CfAnnotationValidator();

	@Test
	void accepts_valid_key_values() {
		//given
		String userDefinedAnnotationKey = "orange.com/a-key_name.suffix";
		String value = "a-valid-key";
		String wrappedLabelKey = BROKERED_SERVICE_CONTEXT_ORANGE + "a-key_name.suffix";

		//when
		validator.validateOrangeAnnotationCanBeIndexedAsALabel(userDefinedAnnotationKey, value, wrappedLabelKey);

		//then no exeption is thrown
	}

	@Test
	void rejects_invalid_key() {
		//given
		String userDefinedAnnotationKey = "orange.com/key-with-chars-incompatible-with-labels";
		String value = "a key with spaces";
		String wrappedLabelKey = BROKERED_SERVICE_CONTEXT_ORANGE + "key-with-chars-incompatible-with-labels";

		//when
		ServiceBrokerInvalidParametersException exception = assertThrows(ServiceBrokerInvalidParametersException.class, () -> {
			validator.validateOrangeAnnotationCanBeIndexedAsALabel(userDefinedAnnotationKey, value,
				wrappedLabelKey);
		});

		//then
		String expectedMessage = "Service broker parameters are invalid: Annotation key \"orange.com/key-with-chars-incompatible-with-labels\" can not be indexed in osb-cmdb as a label \"brokered_service_context_orange_key-with-chars-incompatible-with-labels\" due to violations to regex :[a-z0-9A-Z\\-_\\.]{1,63} (please check maxsize for annotation key =31 chars)";
		assertThat(exception.getMessage()).isEqualTo(expectedMessage);
	}

	@Test
	void rejects_invalid_value() {
		//given
		String userDefinedAnnotationKey = "orange.com/key";
		String wrappedLabelKey = BROKERED_SERVICE_CONTEXT_ORANGE + "key";
		String value = "a key with spaces";

		//when
		ServiceBrokerInvalidParametersException exception = assertThrows(ServiceBrokerInvalidParametersException.class, () -> {
			validator.validateOrangeAnnotationCanBeIndexedAsALabel(userDefinedAnnotationKey, value,
				wrappedLabelKey);
		});

		//then
		String expectedMessage = "Service broker parameters are invalid: Annotation key \"orange.com/key\" with value \"a key with spaces\" can not be indexed in osb-cmdb as a label \"brokered_service_context_orange_key\" due to violations to regex :[a-z0-9A-Z\\-_\\.]{0,63}";
		assertThat(exception.getMessage()).isEqualTo(expectedMessage);
	}

}