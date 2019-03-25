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

package org.springframework.cloud.appbroker.workflow.binding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.core.credential.CredHubCredentialOperations;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.CredentialSummary;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CredHubPersistingDeleteServiceInstanceBindingWorkflowTest {

	@Mock
	private CredHubOperations credHubOperations;

	@Mock
	private CredHubCredentialOperations credHubCredentialOperations;

	private CredHubPersistingDeleteServiceInstanceBindingWorkflow workflow;

	@BeforeEach
	void setUp() {
		this.workflow = new CredHubPersistingDeleteServiceInstanceBindingWorkflow(credHubOperations, "test-app-name");
	}

	@Test
	void deleteCredentialsFromCredHubWhenFound() {
		CredentialName credentialName = ServiceInstanceCredentialName.builder()
			.serviceBrokerName("test-app-name")
			.serviceOfferingName("foo-definition-id")
			.serviceBindingId("foo-binding-id")
			.credentialName("credentials-json")
			.build();

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder =
			DeleteServiceInstanceBindingResponse.builder();

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubCredentialOperations.findByName(credentialName))
			.willReturn(Collections.singletonList(new CredentialSummary(credentialName)));

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.expectNext(responseBuilder)
			.verifyComplete();

		verify(this.credHubCredentialOperations).deleteByName(eq(credentialName));
		verifyNoMoreInteractions(this.credHubCredentialOperations);
	}

	@Test
	void deleteCredentialsFromCredHubWhenNotFound() {
		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder =
			DeleteServiceInstanceBindingResponse.builder();

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubCredentialOperations.findByName(any()))
			.willReturn(Collections.emptyList());

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.expectNext(responseBuilder)
			.verifyComplete();

		verifyNoMoreInteractions(this.credHubCredentialOperations);
	}
}