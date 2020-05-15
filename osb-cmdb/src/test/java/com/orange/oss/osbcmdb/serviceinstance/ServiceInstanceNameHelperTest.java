package com.orange.oss.osbcmdb.serviceinstance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceInstanceNameHelperTest {

	@Test
	void truncates_names_to_50_chars() {
		String longName = "0123456789" +
				"0123456789" +
				"0123456789" +
				"0123456789" +
				"0123456789" +
				"0123456789";
		String expectedName = "0123456789" +
			"0123456789" +
			"0123456789" +
			"0123456789" +
			"0123456789";
		assertThat(ServiceInstanceNameHelper.truncateNameToCfMaxSize(longName)).isEqualTo(expectedName);
	}

}