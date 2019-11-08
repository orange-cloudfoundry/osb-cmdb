/*
 * Copyright 2002-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.util.Map;
import java.util.function.Consumer;

import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.appbroker.deployer.*;
import reactor.core.publisher.Mono;

import org.springframework.util.CollectionUtils;

public class CloudFoundryOperationsUtils {
	private final Logger logger = LoggerFactory.getLogger(CloudFoundryOperationsUtils.class);

	private final CloudFoundryOperations operations;

	public CloudFoundryOperationsUtils(CloudFoundryOperations operations) {
		this.operations = operations;
	}

	Mono<CloudFoundryOperations> getOperations(Map<String, String> properties) {
		return Mono.defer(() -> {
			if (!CollectionUtils.isEmpty(properties) && properties.containsKey(
				DeploymentProperties.TARGET_PROPERTY_KEY)) {
				return getOperationsForSpace(properties.get(DeploymentProperties.TARGET_PROPERTY_KEY));
			}
			return Mono.just(this.operations);
		});
	}

	Mono<CloudFoundryOperations> getOperationsForSpace(String space) {
		return Mono.just(this.operations)
				   .cast(DefaultCloudFoundryOperations.class)
				   .map(cfOperations -> DefaultCloudFoundryOperations.builder()
																	 .from(cfOperations)
																	 .space(space)
																	 .build());
	}

	Mono<CloudFoundryOperations> getOperationsForOrgAndSpace(String organization, String space) {
		return Mono.just(this.operations)
				   .cast(DefaultCloudFoundryOperations.class)
				   .map(cfOperations -> DefaultCloudFoundryOperations.builder()
																	 .from(cfOperations)
																	 .organization(organization)
																	 .space(space)
																	 .build());
	}

	private Mono<Void> deleteServiceKey(String serviceInstanceName, String serviceKeyName, CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services().deleteServiceKey(
			org.cloudfoundry.operations.services.DeleteServiceKeyRequest
				.builder()
				.serviceInstanceName(serviceInstanceName)
				.serviceKeyName(serviceKeyName)
				.build())
			.doOnError(exception -> logger.debug("Error deleting service key {} from instance {} with error '{}'",
				serviceKeyName, serviceInstanceName, exception.getMessage()))
			.onErrorResume(e -> Mono.empty());
	}

	Mono<CreateServiceKeyResponse> createServiceKey(CreateServiceKeyRequest request) {
		org.cloudfoundry.operations.services.CreateServiceKeyRequest createServiceKeyRequest =
			org.cloudfoundry.operations.services.CreateServiceKeyRequest
				.builder()
				.serviceInstanceName(request.getServiceInstanceName())
				.serviceKeyName(request.getServiceKeyName())
				.parameters(request.getParameters())
				.build();

		GetServiceKeyRequest getServiceKeyRequest = GetServiceKeyRequest.builder()
			.serviceInstanceName(request.getServiceInstanceName())
			.serviceKeyName(request.getServiceKeyName())
			.build();

		//No need to check for existence of the space target: service keys need to be in same space than their service instance.

		return operations.services()
			.createServiceKey(createServiceKeyRequest)
			.then(operations.services().
				getServiceKey(getServiceKeyRequest))
			.map(serviceKey -> CreateServiceKeyResponse.builder()
				.name(request.getServiceKeyName())
				.credentials(serviceKey.getCredentials())
				.build()
			);
	}

	Mono<DeleteServiceKeyResponse> deleteServiceKey(DeleteServiceKeyRequest request) {
		String serviceInstanceName = request.getServiceInstanceName();
		String serviceKeyName = request.getServiceKeyName();
		Map<String, String> deploymentProperties = request.getProperties();

		Mono<Void> requestDeleteServiceKey;
		requestDeleteServiceKey = getOperations(deploymentProperties)
			.flatMap(cfOperations -> deleteServiceKey(serviceInstanceName, serviceKeyName, cfOperations));

		return requestDeleteServiceKey
			.doOnSuccess(v -> logger.info("Successfully deleted service key {} from service instance {}", serviceKeyName, serviceInstanceName))
			.doOnError(logError(String.format("Failed to delete service key %s from service instance %s", serviceKeyName, serviceInstanceName), logger))
			.thenReturn(DeleteServiceKeyResponse.builder()
				.name(serviceKeyName)
				.build());
	}

	Consumer<Throwable> logError(String msg, Logger logger) {
		return e -> {
			if (e instanceof UnknownCloudFoundryException) {
				logger.error(msg + "\nUnknownCloudFoundryException encountered, whose payload follows:\n"
					+ ((UnknownCloudFoundryException)e).getPayload(), e);
			} else {
				logger.error(msg, e);
			}
		};
	}
}
