/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import org.cloudfoundry.client.v2.MaintenanceInfo;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that scenario of `cf update-service --upgrade` upgrades a service instance v1 without dashboard (and without
 * MI) into a service instance v2 with a dashboard
 *
 * Inspired from SCAB UpdateInstanceWithNewServiceAcceptanceTest
 */
@Tag("cmdb")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsyncUgradeInstanceWithBackingServiceAcceptanceTest extends CmdbCloudFoundryAcceptanceTest {

	private static final int FIRST_TEST = 1;
	private static final int SECOND_TEST = 2;

	private static final Logger LOG = LoggerFactory.getLogger(
		AsyncUgradeInstanceWithBackingServiceAcceptanceTest.class);

	private static final String SUFFIX = "upgrade-instance";

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	/**
	 * Maintain state before tearDown
	 */
	private String backingServiceName = null;


	@Test
	@Tag("last")
	@Order(SECOND_TEST)
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		// control backing service response
		"spring.profiles.active=acceptanceTests,SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor",
		//cf java client wire traces
		"logging.level.cloudfoundry-client.wire=debug",
		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"logging.level.com.orange.oss.osbcmdb.catalog=debug",

		//We can't enable dynamic catalog to bump maintenance info, otherwise the catalog is fetched from the default
		//org of the hosting CF
		"osbcmdb.dynamic-catalog.enabled=false",

		//therefore we simulate the maintenance info bump: with a manual override the brokered service catalog
		"spring.cloud.openservicebroker.catalog.services[0].plans[0].maintenance_info.version=2.1.1",
		"spring.cloud.openservicebroker.catalog.services[0].plans[0].maintenance_info.description=COAB adds dashboard" +
			" url\nOsb-cmdb displays dashboard url",

		// as well as manual override the backing service catalog
		"spring.cloud.openservicebroker.catalog.services[1].plans[0].maintenance_info.version=1.0.1",
		"spring.cloud.openservicebroker.catalog.services[1].plans[0].maintenance_info.description=COAB adds dashboard url"

	})
	@DisplayName("Initial service instance is upgraded to with backing service broker v2")
	void upgradesServiceInstanceToV2() {
		//given an update of osb-cmdb deployment to bump maintenance info
		//and update of the backing service plan1 with some maintenance info
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		//osb client sees the brokered service as not yet upgraded
		assertThat(brokeredServiceInstance.getDashboardUrl()).isNull();
		MaintenanceInfo brokeredServiceInstanceMaintenanceInfo = brokeredServiceInstance.getMaintenanceInfo();
		assertThat(brokeredServiceInstanceMaintenanceInfo).isNotNull();
		assertThat(brokeredServiceInstanceMaintenanceInfo.getVersion())
			.withFailMessage("before upgrade, description should be null")
			.isNull();
		assertThat(brokeredServiceInstanceMaintenanceInfo.getVersion())
			.withFailMessage("before upgrade, version should be null")
			.isNull();
		//TODO: assert that in catalog, Plan MaintenanceInfo is updated and thus visible to OSB users.

		//When requesting to upgrade the existing service instance, with the merged maintenance info
		upgradeService(brokeredServiceInstanceName(), "2.1.1");

		//then the brokered service instance now includes a dashboard url
		ServiceInstance upgradedBrokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		// and the service instance is not upgradeable anymore, osb users see it was upgraded to last version.
		MaintenanceInfo upgradedBrokeredServiceInstanceMI = upgradedBrokeredServiceInstance.getMaintenanceInfo();
		assertThat(upgradedBrokeredServiceInstanceMI).isNotNull();
		assertThat(upgradedBrokeredServiceInstanceMI.getVersion()).isEqualTo("2.1.1");
		assertThat(upgradedBrokeredServiceInstance.getDashboardUrl()).isNotBlank();
	}

	@Test
	@Tag("first")
	@Order(FIRST_TEST)
	@AppBrokerTestProperties({
		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
		// control backing service response
		"spring.profiles.active=acceptanceTests,SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor",
		//cf java client wire traces
		"logging.level.cloudfoundry-client.wire=debug",
//		"logging.level.cloudfoundry-client.wire=trace",
		"logging.level.cloudfoundry-client.operations=debug",
		"logging.level.cloudfoundry-client.request=debug",
		"logging.level.cloudfoundry-client.response=debug",
		"logging.level.okhttp3=debug",

		"logging.level.com.orange.oss.osbcmdb=debug",
		"osbcmdb.dynamic-catalog.enabled=false",
	})
	@DisplayName("Initial service instance is created with backing service broker v1")
	void initialServiceInstanceV1() {
		//given an osb-cmdb instance configured to not bump maintenance info of backing services.
		//given a backing service plan1 configured without maintenance info

		//given a backend service broker configured to not return dashboard urls in default (v1) version
		// given a brokered service instance is created with a backend service instance (a plan 1) without maintenance
		createServiceInstance(brokeredServiceInstanceName());
		ServiceInstance brokeredServiceInstance = getServiceInstance(brokeredServiceInstanceName());
		//then no dashboard is set on the brokered service instance
		assertThat(brokeredServiceInstance.getDashboardUrl()).isNull();
		//and no MI is assigned
		MaintenanceInfo maintenanceInfo = getServiceInstanceEntity(brokeredServiceInstance.getId())
			.getMaintenanceInfo();
		assertThat(maintenanceInfo == null ||
			(maintenanceInfo.getDescription() == null && maintenanceInfo.getVersion() == null)).isTrue()
		.as("expecting no maintenance info (none in backend catalog and no configured bump in osbcmdb)");
	}

	@Override
	@BeforeEach
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		if (testInfo.getTags().contains("first")) {
			super.setUp(testInfo, brokerProperties);
		}
		else {
			initializeBrokerFixture();
			setUpForBrokerUpdate(brokerProperties);
		}
	}

	@Override
	@AfterEach
	public void tearDown(TestInfo testInfo) {
		if (testInfo.getTags().contains("last")) {
			super.tearDown(testInfo);
		}
	}

}
