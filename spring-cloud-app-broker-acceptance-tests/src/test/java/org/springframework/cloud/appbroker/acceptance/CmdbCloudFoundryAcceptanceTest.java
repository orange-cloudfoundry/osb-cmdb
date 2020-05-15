package org.springframework.cloud.appbroker.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.acceptance.fixtures.osb.OpenServiceBrokerApiClient;

import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService.BROKER_PASSWORD;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService.BROKER_USERNAME;

public abstract class CmdbCloudFoundryAcceptanceTest extends CloudFoundryAcceptanceTest {

	//TODO: encapsulate field with getter once we have finished copy/paste from component tests
	protected OpenServiceBrokerApiClient brokerFixture;

	private static final Logger LOG = LoggerFactory.getLogger(CmdbCloudFoundryAcceptanceTest.class);


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
