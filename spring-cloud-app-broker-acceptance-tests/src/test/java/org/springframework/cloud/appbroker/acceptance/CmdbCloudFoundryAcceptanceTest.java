package org.springframework.cloud.appbroker.acceptance;

public abstract class CmdbCloudFoundryAcceptanceTest extends CloudFoundryAcceptanceTest {

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
