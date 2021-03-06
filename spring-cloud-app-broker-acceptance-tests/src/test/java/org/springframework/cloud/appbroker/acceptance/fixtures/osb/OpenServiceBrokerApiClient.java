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

package org.springframework.cloud.appbroker.acceptance.fixtures.osb;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

public class OpenServiceBrokerApiClient  {

	private static final String ORG_ID = "org-id";

	private static final String SPACE_ID = "space-id";

	private static final String APP_ID = "app-id";

	private static final String SERVICE_KEY_CLIENT_ID = "service-key-client-id";

	private String brokerUrl;

	private String planId;

	private String serviceDefinitionId;

	private String brokerUsername;

	private String brokerPassword;

	public OpenServiceBrokerApiClient(String brokerUrl, String planId,
		String serviceDefinitionId, String brokerUsername, String brokerPassword) {
		this.brokerUrl = brokerUrl;
		this.planId = planId;
		this.serviceDefinitionId = serviceDefinitionId;
		this.brokerUsername = brokerUsername;
		this.brokerPassword = brokerPassword;
	}

	public String createServiceInstanceUrl() {
		return "/service_instances/{instance_id}";
	}

	public String getLastInstanceOperationUrl() {
		return "/service_instances/{instance_id}/last_operation";
	}

	public String deleteServiceInstanceUrl() {
		String serviceDefinitionId = this.serviceDefinitionId;
		String planId = this.planId;
		return deleteServiceInstanceUrl(serviceDefinitionId, planId);
	}

	public String deleteServiceInstanceUrl(String serviceDefinitionId, String planId) {
		return "/service_instances/{instance_id}" +
			"?service_id=" + serviceDefinitionId +
			"&plan_id=" + planId;
	}

	public String createBindingUrl() {
		return "/service_instances/{instance_id}/service_bindings/{binding_id}";
	}

	public String deleteBindingUrl() {
		return "/service_instances/{instance_id}/service_bindings/{binding_id}" +
			"?service_id=" + serviceDefinitionId +
			"&plan_id=" + planId;
	}

	public RequestSpecification serviceInstanceRequest() {
		return serviceInstanceRequest(this.planId);
	}

	public RequestSpecification serviceInstanceRequest(String planId) {
		return serviceInstanceRequest(this.serviceDefinitionId, planId);
	}

	public RequestSpecification serviceInstanceRequest(String serviceDefinitionId, String planId) {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"" +
				"}\n");
	}

	public RequestSpecification serviceInstanceRequest(String serviceDefinitionId, String planId,
		Map<String, Object> params) {
		String stringParams = new JSONObject(params).toString();
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"," +
				"\"parameters\": " + stringParams +
				"}");
	}

	public RequestSpecification serviceInstanceUpgradeRequest(String serviceDefinitionId, String planId, String maintenanceVersionInfo) {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"," +
				"\"maintenance_info\": { \"version\": \""+ maintenanceVersionInfo + "\" }" +
				"}");
	}

	public RequestSpecification serviceInstanceRequestWithCfOsbContext(String context) {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"," +
				context +
				"}\n")
			.header("X-Broker-API-Originating-Identity", "cloudfoundry " +
				"eyANCiAgInVzZXJfaWQiOiAiNjgzZWE3NDgtMzA5Mi00ZmY0LWI2NTYtMzljYWNjNGQ1MzYwIg0KfQ==");
	}

	public RequestSpecification serviceInstanceRequestWithK8sOsbContext(String context) {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"," +
				context +
				"}\n")
			.header("X-Broker-API-Originating-Identity" , "kubernetes " +
				"ew0KICAidXNlcm5hbWUiOiAiZHVrZSIsDQogICJ1aWQiOiAiYzJkZGUyNDItNWNlNC0xMWU3LTk4OGMtMDAwYzI5NDZmMTRmIiwNCiAgImdyb3VwcyI6IFsgImFkbWluIiwgImRldiIgXSwNCiAgImV4dHJhIjogew0KICAgICJteWRhdGEiOiBbICJkYXRhMSIsICJkYXRhMyIgXQ0KICB9DQp9");
	}

	public RequestSpecification serviceAppBindingRequest() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"bind_resource\": {" +
				"\"app_guid\": \"" + APP_ID + "\"" +
				"}" +
				"}");
	}

	public RequestSpecification serviceAppBindingRequest(Map<String, Object> params) {
		String stringParams = new JSONObject(params).toString();
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"bind_resource\": {" +
					"\"app_guid\": \"" + APP_ID + "\"" +
				"}," +
				"\"parameters\": " + stringParams +
				"}");
	}

	//The default binding resource format used by CF, albeit yet undocumented,
	// see https://github.com/openservicebrokerapi/servicebroker/pull/704
	public RequestSpecification serviceKeyRequest() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + this.serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + this.planId + "\"," +
				"\"bind_resource\": {" +
				"\"credential_client_id\": \"" + SERVICE_KEY_CLIENT_ID + "\"" +
				"}" +
				"}");
	}

	//A OSB-API compliant request (bind resource is optional per https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#bind-resource-object
	// "bind_resource and its fields are OPTIONAL"
	public RequestSpecification serviceBindingRequestWithoutResource() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\""+
				"}");
	}

	//A OSB-API compliant request (bind resource is optional per https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#bind-resource-object
	// "bind_resource and its fields are OPTIONAL"
	public RequestSpecification serviceBindingRequestWithEmptyResource() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\","+
				"\"bind_resource\": {}" +
				"}");
	}

	public RequestSpecification serviceBrokerSpecification() {
		return with()
			.relaxedHTTPSValidation() // TODO: make this configureable
			.auth().basic(brokerUsername, brokerPassword)
			.baseUri(brokerUrl+ "/v2")
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON);
	}

	public String waitForAsyncOperationComplete(String serviceInstanceId) {
		try {
			String state;
			do {
				//noinspection BusyWait
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
				state = given(serviceInstanceRequest())
					.when()
					.get(getLastInstanceOperationUrl(), serviceInstanceId)
					.then()
					.statusCode(HttpStatus.OK.value())
					.extract().body().jsonPath().getString("state");
			} while (state.equals(OperationState.IN_PROGRESS.toString()));
			return state;
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}


}
