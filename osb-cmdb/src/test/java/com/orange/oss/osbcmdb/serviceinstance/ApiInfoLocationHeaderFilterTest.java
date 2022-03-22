package com.orange.oss.osbcmdb.serviceinstance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiInfoLocationHeaderFilterTest {

	@Test
	void only_enforces_when_configured() {
		assert_enforcement(false, false, "non-empty-expected-header", "/v2/catalog");
		assert_enforcement(false, false, "", "/v2/catalog");
		assert_enforcement(false, true, "", "/v2/catalog");
		assert_enforcement(true, true, "non-empty-expected-header", "/v2/catalog");
	}

	@Test
	void does_not_enforce_non_osb_uris() {
		assert_enforcement(false, "/actuator/health");
		assert_enforcement(true, "/v2/catalog");
	}

	@Test
	void enforces_expected_api_info_url() {
		assert_uri_rejection(false, "api.domain.org/v2/info", "api.domain.org/v2/info");
		assert_uri_rejection(false, "api.domain.org/v2/info", "api.domain.org/V2/info");  //should be case insensitive
		assert_uri_rejection(true, "api.domain.org/v2/info", "unrelated-platform.domain.org/v2/info");
		assert_uri_rejection(true, "api.domain.org/v2/info", null); //a K8S client or custom client
	}

	@Test
	void always_accepts_backing_cloudfoundry_api_info_url() {
		//given filter is configured to accept local cloudfoundry
		ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter = new ApiInfoLocationHeaderFilter(
			"a-distinct-api.domain.com/v2/info",
			true, "api.domain.org");

		//When queried with local cloudfoundry v2/info
		boolean actualShouldAcceptXApiInfoLocation = apiInfoLocationHeaderFilter.shouldAcceptXApiInfoLocation(
			"api.domain.org/v2/info", "a-distinct-api.domain.com/v2/info");
		//then it is accepted
		assertThat(actualShouldAcceptXApiInfoLocation).isEqualTo(true);
	}

	@Test
	void formats_json_error_response_when_rejecting() {
		//given a filter
		ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter = new ApiInfoLocationHeaderFilter(
			"a-distinct-api.domain.com/v2/info",
			true, "api.domain.org");

		//When a rejected header is received
		String jsonErrorResponse = apiInfoLocationHeaderFilter.formatRejectedXApiInfoLocation("unrelated-api.domain.com",
			"a-distinct-api.domain.com/v2/info");
		//then an error response is returned
		assertThat(jsonErrorResponse).isEqualTo("{\"description\":\" Request not received from white listed osb-cmdb client. Please, double check configuration mistakes. Expecting X-Api-Info-Location http header value:a-distinct-api.domain.com/v2/info but got: unrelated-api.domain.com\"}");

		//and when a malicious header is received
		jsonErrorResponse = apiInfoLocationHeaderFilter.formatRejectedXApiInfoLocation("malicious header with various \" ' quotes ",
			"a-distinct-api.domain.com/v2/info");
		//then an error response is returned with quotes being escaped by Json serializer
		assertThat(jsonErrorResponse).isEqualTo("{\"description\":\" Request not received from white listed osb-cmdb client. Please, double check configuration mistakes. Expecting X-Api-Info-Location http header value:a-distinct-api.domain.com/v2/info but got: malicious header with various \\\" ' quotes \"}");
	}

	private void assert_uri_rejection(boolean shouldReject, String expectedXApiInfoLocation, String receivedxApiInfoLocation) {
		ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter = new ApiInfoLocationHeaderFilter(
			expectedXApiInfoLocation,
			true, "api.redacted-domain.org");
		boolean actualShouldAcceptXApiInfoLocation = apiInfoLocationHeaderFilter.shouldAcceptXApiInfoLocation(receivedxApiInfoLocation, expectedXApiInfoLocation);
		assertThat(actualShouldAcceptXApiInfoLocation).isEqualTo(! shouldReject);
	}


	private void assert_enforcement(boolean shouldEnforce, String inspectedUri) {
		ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter = new ApiInfoLocationHeaderFilter(
			"non-empty-expected-header",
			true, "api.redacted-domain.org");
		assertThat(apiInfoLocationHeaderFilter.shouldEnforceXApiInfoLocation(inspectedUri)).isEqualTo(shouldEnforce);
	}

	private void assert_enforcement(boolean shouldEnforce,
		boolean rejectRequestsWithNonMatchingXApiInfoLocationHeaderFlag, String expectedXApiInfoLocationHeaderFlag,
		String incomingRequestUri) {
		ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter = new ApiInfoLocationHeaderFilter(
			expectedXApiInfoLocationHeaderFlag,
			rejectRequestsWithNonMatchingXApiInfoLocationHeaderFlag, "api.redacted-domain.org");
		assertThat(apiInfoLocationHeaderFilter.shouldEnforceXApiInfoLocation(incomingRequestUri)).isEqualTo(shouldEnforce);
	}

}