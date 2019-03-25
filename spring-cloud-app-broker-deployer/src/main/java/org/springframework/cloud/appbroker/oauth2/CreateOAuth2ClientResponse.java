/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.oauth2;

import java.util.List;
import java.util.Objects;

public class CreateOAuth2ClientResponse {

	private final String clientId;
	private final String clientName;
	private final List<String> scopes;
	private final List<String> authorities;
	private final List<String> grantTypes;

	CreateOAuth2ClientResponse(String clientId, String clientName,
							   List<String> scopes, List<String> authorities,
							   List<String> grantTypes) {

		this.clientId = clientId;
		this.clientName = clientName;
		this.scopes = scopes;
		this.authorities = authorities;
		this.grantTypes = grantTypes;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientName() {
		return clientName;
	}

	public List<String> getScopes() {
		return scopes;
	}

	public List<String> getAuthorities() {
		return authorities;
	}

	public List<String> getGrantTypes() {
		return grantTypes;
	}

	public static CreateOAuth2ClientResponseBuilder builder() {
		return new CreateOAuth2ClientResponseBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CreateOAuth2ClientResponse)) {
			return false;
		}
		CreateOAuth2ClientResponse that = (CreateOAuth2ClientResponse) o;
		return Objects.equals(clientId, that.clientId) &&
			Objects.equals(clientName, that.clientName) &&
			Objects.equals(scopes, that.scopes) &&
			Objects.equals(authorities, that.authorities) &&
			Objects.equals(grantTypes, that.grantTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientId, clientName, scopes, authorities, grantTypes);
	}

	@Override
	public String toString() {
		return "CreateOAuth2ClientResponse{" +
			"clientId='" + clientId + '\'' +
			", clientName='" + clientName + '\'' +
			", scopes=" + scopes +
			", authorities=" + authorities +
			", grantTypes=" + grantTypes +
			'}';
	}

	public static class CreateOAuth2ClientResponseBuilder {
		private String clientId;
		private String clientName;
		private List<String> scopes;
		private List<String> authorities;
		private List<String> grantTypes;

		CreateOAuth2ClientResponseBuilder() {
		}

		public CreateOAuth2ClientResponseBuilder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder clientName(String name) {
			this.clientName = name;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder scopes(List<String> scopes) {
			this.scopes = scopes;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder authorities(List<String> authorities) {
			this.authorities = authorities;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder grantTypes(List<String> grantTypes) {
			this.grantTypes = grantTypes;
			return this;
		}

		public CreateOAuth2ClientResponse build() {
			return new CreateOAuth2ClientResponse(clientId, clientName, scopes, authorities, grantTypes);
		}
	}
}
