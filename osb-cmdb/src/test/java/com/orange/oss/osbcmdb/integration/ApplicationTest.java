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
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.controller.CatalogController;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OsbCmdbApplication.class})
@ActiveProfiles({"openservicebroker-catalog", "appbroker-cf"})
@Tag("scab")
class ApplicationTest {

	@Autowired(required = false)
	private CatalogController catalogController;

	@Autowired(required = false)
	private CloudFoundryOperations cloudFoundryOperations;

	@Test
	void applicationInitialized() {
		assertThat(catalogController).isNotNull();
		assertThat(cloudFoundryOperations).isNotNull();
	}

}