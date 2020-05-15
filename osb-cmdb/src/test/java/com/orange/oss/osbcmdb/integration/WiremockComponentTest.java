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

package com.orange.oss.osbcmdb.integration;

import com.orange.oss.osbcmdb.OsbCmdbApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import  com.orange.oss.osbcmdb.integration.fixtures.CloudControllerStubFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.CredHubStubFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.OpenServiceBrokerApiFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.UaaStubFixture;
import  com.orange.oss.osbcmdb.integration.fixtures.WiremockServerFixture;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {OsbCmdbApplication.class,
		WiremockServerFixture.class,
		OpenServiceBrokerApiFixture.class,
		CloudControllerStubFixture.class,
		UaaStubFixture.class,
		CredHubStubFixture.class
	},
	properties = {
		"spring.cloud.appbroker.deployer.cloudfoundry.api-host=localhost",
		"spring.cloud.appbroker.deployer.cloudfoundry.api-port=8080",
		"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
		"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-org=test",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-space=development",
		"spring.cloud.appbroker.deployer.cloudfoundry.secure=false",


		"debug=true", //Spring boot debug mode
		//osb-cmdb auth
		"spring.security.user.name=user",
		"spring.security.user.password=password",
		"osbcmdb.admin.user=admin",
		"osbcmdb.admin.password=password",
//		"spring.profiles.active=acceptanceTests",
		// DON'T SET LOGGING PROPERTIES HERE, AS THEIR ASSIGNEMENT IS HARD TO TRACE, PREFER KEEPING THEM IN LOGBACK.XML

		"osbcmdb.dynamic-catalog.enabled=false"

	}
)
//triggers loading of related application-{profile].yml which define catalog
@ActiveProfiles("openservicebroker-catalog")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WiremockComponentTest {

	@Autowired
	private WiremockServerFixture wiremockFixture;

	@Autowired
	private CloudControllerStubFixture cloudFoundryFixture;

	@Autowired
	private UaaStubFixture uaaFixture;

	@AfterAll
	void tearDown() {
		wiremockFixture.stopWiremock();
	}

	@BeforeEach
	void resetWiremock() {
		wiremockFixture.resetWiremock();

		uaaFixture.stubCommonUaaRequests();
		cloudFoundryFixture.stubCommonCloudControllerRequests();
	}

	@AfterEach
	void verifyStubs() {
		wiremockFixture.verifyAllRequiredStubsUsed();
	}

}
