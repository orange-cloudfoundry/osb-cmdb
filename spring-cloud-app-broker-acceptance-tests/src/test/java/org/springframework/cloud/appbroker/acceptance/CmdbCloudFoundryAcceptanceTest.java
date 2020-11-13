package org.springframework.cloud.appbroker.acceptance;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.acceptance.fixtures.osb.OpenServiceBrokerApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService.BROKER_PASSWORD;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService.BROKER_USERNAME;

public abstract class CmdbCloudFoundryAcceptanceTest extends CloudFoundryAcceptanceTest {

	public static final String X_OSB_CMDB_CUSTOM_KEY_NAME = "x-osb-cmdb";

	public static final String BROKERED_SERVICE_CLIENT_NAME = "brokered_service_client_name";

	//TODO: encapsulate field with getter once we have finished copy/paste from component tests
	protected OpenServiceBrokerApiClient brokerFixture;

	private static final Logger LOG = LoggerFactory.getLogger(CmdbCloudFoundryAcceptanceTest.class);

	public void assertCustomParams(Map<String, Object> brokeredServiceInstanceParams) {
		assertThat(brokeredServiceInstanceParams).containsKey(X_OSB_CMDB_CUSTOM_KEY_NAME);
		//noinspection unchecked
		Map<String, Object> customParamValue = (Map<String, Object>) brokeredServiceInstanceParams
			.get(X_OSB_CMDB_CUSTOM_KEY_NAME);
		assertThat(customParamValue).containsKey("annotations");
		assertThat(customParamValue).containsKey("labels");
		//noinspection unchecked
		Map<String, Object> annotations = (Map<String, Object>) customParamValue.get("annotations");
		//noinspection unchecked
		Map<String, Object> labels = (Map<String, Object>) customParamValue.get("labels");
		assertThat(annotations).isNotEmpty();
		assertThat(labels).isNotEmpty();
		assertThat(annotations).contains(entry(BROKERED_SERVICE_CLIENT_NAME, cloudFoundryProperties.getDefaultOrg()));

	}


	@BeforeEach
	@Override
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		super.setUp(testInfo, brokerProperties);
		initializeBrokerFixture();
	}

	protected void initializeBrokerFixture() {
		try {
			String httpsBrokerUrl = cloudFoundryService.getApplicationRoute(testBrokerAppName()).block();
			brokerFixture = new OpenServiceBrokerApiClient(httpsBrokerUrl, PLAN_ID, SERVICE_ID, BROKER_USERNAME, BROKER_PASSWORD);
		}
		catch (Exception e) {
			LOG.error("Failed to initialize osb broker client {}", e.toString(), e);
			throw e;
		}
	}

	protected String brokeredServiceInstanceName() {
		return 	"si-" + testSuffix();
	}

	protected String brokeredServiceName() { return "bsn-" + testSuffix(); }

	// preserve scab mandatory method name for now
	@Override
	protected String backingServiceName() {
		return "cmdb-dont-use-scab-backing-service" + testSuffix();
	}

	// preserve scab mandatory method name for now, but don't use it in our tests as its name isn't relevant to cmdb
	@Override
	protected String appServiceName() {
		return brokeredServiceName();
	}

}
