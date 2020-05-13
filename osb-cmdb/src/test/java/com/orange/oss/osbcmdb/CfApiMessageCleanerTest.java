package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CfApiMessageCleanerTest {

	@Test
	@DisplayName("Redacts url from CF API message")
	void redacts_url_from_cf_api_message() {
		//Given an exception leaking broker url
		Exception e = new org.cloudfoundry.client.v2.ClientV2Exception(504, 10001,
			"The request to the service broker timed out: " +
				"https://test-broker-app-create-instance-with-sync-backing-timeout.redacted-domain.com/v2/service_instances/99fa775a-f56d-4598-a903-9a67617274af?accepts_incomplete=true",
			"CF-HttpClientTimeout");
		//when asking
		CfApiMessageCleaner cfApiMessageCleaner = new CfApiMessageCleaner();
		//noinspection ThrowableNotThrown
		ServiceBrokerException rethrownException = assertThrows(ServiceBrokerException.class,
			() -> cfApiMessageCleaner.redactExceptionAndWrapAsServiceBrokerException(e));
		assertThat(rethrownException.getMessage()).doesNotContain("https://test-broker-app-create-instance-with-sync-backing-timeout.redacted-domain.com");
		assertThat(rethrownException.getMessage()).doesNotContain("99fa775a-f56d-4598-a903-9a67617274af");
		assertThat(rethrownException.getMessage()).doesNotContain("accepts_incomplete=true");
	}

}