/*
 * Copyright 2002-2019 the original author or authors.
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

package com.orange.oss.osbcmdb.integration.cmdb;

import com.orange.oss.osbcmdb.OsbCmdbApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * Cmdb's copy of SCAB CatalogTest: merely asserts that the osb client is properly fetching the catalog configured in
 * the application.yml served by the AppBrokerApplication.class.
 * Merely useful to check that when dynamic catalog is turned off, static catalog is used instead
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {OsbCmdbApplication.class},
	properties = {

	"debug=true", //Spring boot debug mode
	//osb-cmdb auth
	"spring.security.user.name=user",
	"spring.security.user.password=password",
	"osbcmdb.admin.user=admin",
	"osbcmdb.admin.password=password",
//		"spring.profiles.active=acceptanceTests",
	// DON'T SET LOGGING PROPERTIES HERE, AS THEIR ASSIGNEMENT IS HARD TO TRACE, PREFER KEEPING THEM IN LOGBACK.XML

	"osbcmdb.dynamic-catalog.enabled=false"}
	)
@ActiveProfiles({"openservicebroker-catalog", "appbroker-cf"}) //triggers loading of related application-{profile]
// .yml loading sample catalog
class StaticOsbCatalogTest {

	private String baseUrl;

	@Value("${local.server.port}")
	private String port;

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port;
	}

	@Test
	void should_retrieve_static_catalog_when_dynamic_catalog_is_disabled() {
		given()
			.auth().basic("user", "password")
			.get(baseUrl + "/v2/catalog")
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("services[0].name", equalTo("example"))
			.body("services[0].description", equalTo("A simple example"))
			.body("services[0].bindable", equalTo(true))
			.body("services[0].metadata.size()", is(0))
			.body("services[0].plan_updateable", equalTo(null))
			.body("services[0].instances_retrievable", equalTo(null))
			.body("services[0].plans[0].id", equalTo("standard-PLAN-GUID"))
			.body("services[0].plans[0].name", equalTo("standard"))
			.body("services[0].plans[0].metadata", equalTo(null))
			.body("services[0].plans[0].bindable", equalTo(true))
			.body("services[0].plans[0].free", equalTo(true))
			.body("services[0].plans[0].description", equalTo("A simple plan"));
	}

}

