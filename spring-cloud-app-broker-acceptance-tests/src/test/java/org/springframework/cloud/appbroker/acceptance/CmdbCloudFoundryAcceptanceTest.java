package org.springframework.cloud.appbroker.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import org.springframework.cloud.appbroker.acceptance.fixtures.osb.OpenServiceBrokerApiClient;

public abstract class CmdbCloudFoundryAcceptanceTest extends CloudFoundryAcceptanceTest {

	//TODO: encapsulate field with getter once we have finished copy/paste from component tests
	protected OpenServiceBrokerApiClient brokerFixture;

	@BeforeEach
	@Override
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		super.setUp(testInfo, brokerProperties);
		initializeBrokerFixture();
	}

	protected void initializeBrokerFixture() {
		String brokerName = serviceBrokerName();
		String httpsBrokerUrl = cloudFoundryService.getApplicationRoute(brokerName).block();
		brokerFixture = new OpenServiceBrokerApiClient(httpsBrokerUrl, PLAN_ID, SERVICE_ID);
	}

	protected String brokeredServiceInstanceName() {
		return 	"si-" + testSuffix();
	}

	protected String brokeredServiceName() { return "bsn-" + testSuffix(); }

	// preserve scab mandatory method name for now
	@Override
	protected String backingServiceName() {
		return "cmdb-dont-use-scab-backing-service";
	}

	// preserve scab mandatory method name for now, but don't use it in our tests as its name isn't relevant to cmdb
	@Override
	protected String appServiceName() {
		return brokeredServiceName();
	}

}
