/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

import java.util.Map;

public class DeleteServiceInstanceRequest {

	private final String serviceInstanceName;
	private final Map<String, String> properties;

	DeleteServiceInstanceRequest(String serviceInstanceName, Map<String, String> properties) {
		this.serviceInstanceName = serviceInstanceName;
		this.properties = properties;
	}

	public static DeleteServiceInstanceRequestBuilder builder() {
		return new DeleteServiceInstanceRequestBuilder();
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static class DeleteServiceInstanceRequestBuilder {

		private String serviceInstanceName;
		private Map<String, String> properties;

		DeleteServiceInstanceRequestBuilder() {
		}

		public DeleteServiceInstanceRequestBuilder serviceInstanceName(String name) {
			this.serviceInstanceName = name;
			return this;
		}

		public DeleteServiceInstanceRequestBuilder properties(Map<String, String> properties) {
			this.properties = properties;
			return this;
		}

		public DeleteServiceInstanceRequest build() {
			return new DeleteServiceInstanceRequest(serviceInstanceName, properties);
		}

	}
}
