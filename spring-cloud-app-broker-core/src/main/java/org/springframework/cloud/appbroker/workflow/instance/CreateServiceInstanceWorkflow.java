/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.workflow.instance;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import reactor.core.publisher.Mono;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import reactor.util.Logger;
import reactor.util.Loggers;

public class CreateServiceInstanceWorkflow extends ServiceInstanceWorkflow {
	private final Logger log = Loggers.getLogger(CreateServiceInstanceWorkflow.class);

	private BackingAppDeploymentService deploymentService;
	private ParametersTransformer parametersTransformer;

	public CreateServiceInstanceWorkflow(BrokeredServices brokeredServices,
										 BackingAppDeploymentService deploymentService,
										 ParametersTransformer parametersTransformer) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.parametersTransformer = parametersTransformer;
	}

	public Mono<String> create(CreateServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlanId())
			.flatMap(backingApps ->
				Mono.fromCallable(() -> {
					parametersTransformer.transform(backingApps, request.getParameters());
					return backingApps;
				}))
			.flatMap(deploymentService::deploy)
			.doOnRequest(l -> log.info("Deploying applications {}", brokeredServices))
			.doOnSuccess(d -> log.info("Finished deploying applications {}", brokeredServices))
			.doOnError(e -> log.info("Error deploying applications {} with error {}", brokeredServices, e));
	}
}
