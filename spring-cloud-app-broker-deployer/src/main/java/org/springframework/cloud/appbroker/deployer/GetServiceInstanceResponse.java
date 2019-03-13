/*
 * Copyright 2016-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer;

public class GetServiceInstanceResponse {

	private final String name;

	private final String service;

	private final String plan;

	GetServiceInstanceResponse(String name, String service, String plan) {
		this.name = name;
		this.service = service;
		this.plan = plan;
	}

	public static CreateServiceInstanceRequestBuilder builder() {
		return new CreateServiceInstanceRequestBuilder();
	}

	public String getName() {
		return name;
	}

	public String getService() {
		return service;
	}

	public String getPlan() {
		return plan;
	}

	public static class CreateServiceInstanceRequestBuilder {

		private String name;

		private String service;

		private String plan;

		CreateServiceInstanceRequestBuilder() {
		}

		public CreateServiceInstanceRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public CreateServiceInstanceRequestBuilder service(String service) {
			this.service = service;
			return this;
		}

		public CreateServiceInstanceRequestBuilder plan(String plan) {
			this.plan = plan;
			return this;
		}

		public GetServiceInstanceResponse build() {
			return new GetServiceInstanceResponse(name, service, plan);
		}
	}

}
