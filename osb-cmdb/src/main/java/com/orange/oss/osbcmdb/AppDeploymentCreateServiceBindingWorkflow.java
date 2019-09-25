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

package com.orange.oss.osbcmdb;

import org.springframework.cloud.appbroker.deployer.*;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@Order(0)
public class AppDeploymentCreateServiceBindingWorkflow
	extends AppDeploymentInstanceWorkflow
	implements CreateServiceInstanceAppBindingWorkflow {

	private final Logger log = Loggers.getLogger(AppDeploymentCreateServiceBindingWorkflow.class);

	private final BackingAppDeploymentService deploymentService;
	private final BackingServicesProvisionService backingServicesProvisionService;
	private final BackingApplicationsParametersTransformationService appsParametersTransformationService;
	private final BackingServicesParametersTransformationService servicesParametersTransformationService;
	private final CredentialProviderService credentialProviderService;
	private final TargetService targetService;

	public AppDeploymentCreateServiceBindingWorkflow(BrokeredServices brokeredServices,
													 BackingAppDeploymentService deploymentService,
													 BackingServicesProvisionService backingServicesProvisionService,
													 BackingApplicationsParametersTransformationService appsParametersTransformationService,
													 BackingServicesParametersTransformationService servicesParametersTransformationService,
													 CredentialProviderService credentialProviderService,
													 TargetService targetService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.backingServicesProvisionService = backingServicesProvisionService;
		this.appsParametersTransformationService = appsParametersTransformationService;
		this.servicesParametersTransformationService = servicesParametersTransformationService;
		this.credentialProviderService = credentialProviderService;
		this.targetService = targetService;
	}

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceBindingRequest request) {
		//Only accept binding request matching one registered backing service
		return accept(request.getServiceDefinition(), request.getPlan());
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder> buildResponse(CreateServiceInstanceBindingRequest request, CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {

		return createBackingServiceKey(request).   //create service key, returning credentials
			last(). //only keep last one (should never happen)
			map(responseBuilder::credentials); //store it in builder
	}

	private Flux<Map<String, Object>> createBackingServiceKey(CreateServiceInstanceBindingRequest request) {
		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingServices ->
				targetService.addToBackingServices(backingServices,
					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
					request.getServiceInstanceId()))
			.flatMap(backingServices ->
				servicesParametersTransformationService.transformParameters(backingServices,
					request.getParameters()))
			.flatMap(AppDeploymentCreateServiceBindingWorkflow::getBackingServiceKeys)
			.flatMap(backingServiceKeys -> setServiceKeyName(backingServiceKeys, request.getBindingId()))
			.flatMapMany(backingServicesProvisionService::createServiceKeys)
			.doOnRequest(l -> log.debug("Creating backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished creating backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error("Error creating backing services keysfor {}/{} with error '{}'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exceptionMessageOrToString(exception)));
	}

	private static Mono<? extends BackingServiceKeys> getBackingServiceKeys(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.flatMap(backingService -> {
				BackingServiceKey backingServiceKey = BackingServiceKey.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.name(backingService.getName())
					//TODO also set the binding params
					.build();
				return Mono.just(backingServiceKey);
			})
			.collectList()
			.flatMap(backingServiceKeys -> Mono.just(new BackingServiceKeys(backingServiceKeys)));
	}




	/// ---- to delete ---------------
//	public Mono<Void> create(CreateServiceInstanceRequest request, CreateServiceInstanceResponse response) {
////		return createBackingServices(request)
////			.thenMany(deployBackingApplications(request))
////			.then();
//	}

	private String exceptionMessageOrToString(Throwable exception) {
		return exception.getMessage() == null ? exception.toString() : exception.getMessage();
	}


//	private Flux<String> createBackingServices(CreateServiceInstanceRequest request) {
//		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
//			.flatMap(backingServices ->
//				targetService.addToBackingServices(backingServices,
//					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
//					request.getServiceInstanceId()))
//			.flatMap(backingServices ->
//				servicesParametersTransformationService.transformParameters(backingServices,
//					request.getParameters()))
//			.flatMap(backingServices -> addGuidToServiceInstanceName(backingServices, request.getServiceInstanceId()))
//				.flatMapMany(backingServicesProvisionService::createServiceInstance)
//			.doOnRequest(l -> log.debug("Creating backing services for {}/{}",
//				request.getServiceDefinition().getName(), request.getPlan().getName()))
//			.doOnComplete(() -> log.debug("Finished creating backing services for {}/{}",
//				request.getServiceDefinition().getName(), request.getPlan().getName()))
//			.doOnError(exception -> log.error("Error creating backing services for {}/{} with error '{}'",
//				request.getServiceDefinition().getName(), request.getPlan().getName(), exceptionMessageOrToString(exception)));
//	}

	// Unused, just for inspiration above
//	public Mono<List<BackingService>> transformParameters(List<BackingService> backingServices,
//														  Map<String, Object> parameters) {
//		return Flux.fromIterable(backingServices)
//				.flatMap(backingService -> {
//					List<ParametersTransformerSpec> specs = getTransformerSpecsForService(backingService);
//
//					return Flux.fromIterable(specs)
//							.flatMap(spec -> {
//								ParametersTransformer<BackingService> transformer = locator.getByName(spec.getName(), spec.getArgs());
//								return transformer.transform(backingService, parameters);
//							})
//							.then(Mono.just(backingService));
//				})
//				.collectList();
//	}


//	private Flux<String> deployBackingApplications(CreateServiceInstanceRequest request) {
//		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlan())
//			.flatMap(backingApps ->
//				targetService.addToBackingApplications(backingApps,
//					getTargetForService(request.getServiceDefinition(),
//						request.getPlan()) , request.getServiceInstanceId()))
//			.flatMap(backingApps ->
//				appsParametersTransformationService.transformParameters(backingApps,
//					request.getParameters()))
//			.flatMap(backingApps ->
//				credentialProviderService.addCredentials(backingApps,
//					request.getServiceInstanceId()))
//			.flatMapMany(backingApps -> deploymentService.deploy(backingApps, request.getServiceInstanceId()))
//			.doOnRequest(l -> log.debug("Deploying backing applications for {}/{}",
//				request.getServiceDefinition().getName(), request.getPlan().getName()))
//			.doOnComplete(() -> log.debug("Finished deploying backing applications for {}/{}",
//				request.getServiceDefinition().getName(), request.getPlan().getName()))
//			.doOnError(exception -> log.error("Error deploying backing applications for {}/{} with error '{}'",
//				request.getServiceDefinition().getName(), request.getPlan().getName(), exceptionMessageOrToString(exception)));
//	}


}
