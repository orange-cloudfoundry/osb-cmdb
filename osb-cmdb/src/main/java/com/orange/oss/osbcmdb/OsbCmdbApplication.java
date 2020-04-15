package com.orange.oss.osbcmdb;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.appbroker.deployer.*;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationServiceImpl;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class OsbCmdbApplication {

    public static void main(String[] args) {
		Hooks.onOperatorDebug(); //Turn on debugging
    	SpringApplication.run(OsbCmdbApplication.class, args);
    }

	@Bean
	@ConditionalOnMissingBean
	public CreateBackingServicesMetadataTransformationService createBackingServicesMetadataTransformationService() {
		return new CreateBackingServicesMetadataTransformationServiceImpl();
	}


	@Bean
	@Profile("!offline-test-without-scab")
	public CreateServiceInstanceAppBindingWorkflow createServiceKeyWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		CredentialProviderService credentialProviderService,
		TargetService targetService,
		BackingServicesProvisionService backingServicesProvisionService) {

		return new AppDeploymentCreateServiceBindingWorkflow(brokeredServices, backingAppDeploymentService, backingServicesProvisionService, appsParametersTransformationService, servicesParametersTransformationService, credentialProviderService, targetService);
	}

	@Bean
	@Profile("!offline-test-without-scab")
	public DeleteServiceInstanceBindingWorkflow deleteServiceKeyWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		CredentialProviderService credentialProviderService,
		TargetService targetService,
		BackingServicesProvisionService backingServicesProvisionService) {

		return new AppDeploymentDeleteServiceBindingWorkflow(brokeredServices, backingAppDeploymentService, backingServicesProvisionService, appsParametersTransformationService, servicesParametersTransformationService, credentialProviderService, targetService);
	}

	/**
	 * Provide a {@link OsbCmdbServiceInstance} bean
	 *
	 * @param deploymentProperties the CloudFoundryDeploymentProperties bean
	 * @param cloudFoundryOperations the CloudFoundryOperations bean
	 * @param cloudFoundryClient the CloudFoundryClient bean
	 * @param targetProperties the CloudFoundryTargetProperties bean
	 * @param serviceInstanceInterceptor
	 * @return the bean
	 */
	@Bean
	public OsbCmdbServiceInstance osbCmdbServiceInstance(CloudFoundryDeploymentProperties deploymentProperties,
		CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		@Autowired(required = false)
		ServiceInstanceInterceptor serviceInstanceInterceptor) {
		return new OsbCmdbServiceInstance(deploymentProperties, cloudFoundryOperations, cloudFoundryClient,
			targetProperties.getDefaultOrg(), targetProperties.getDefaultSpace(), targetProperties.getUsername(),
			serviceInstanceInterceptor);
	}

	//TODO: condition that to spring profile or env var
	@Bean
	public ServiceInstanceInterceptor noopServiceInstanceInterceptor() {
		return new ServiceInstanceInterceptor() {};
	}

	//TODO: condition that to spring profile or env var
	@Bean
	@ConditionalOnMissingBean
	public ServiceBindingInterceptor noopServiceBindingInterceptor() {
		return new ServiceBindingInterceptor() {};
	}

	/**
	 * Provide a {@link OsbCmdbServiceBinding} bean
	 *
	 * @param cloudFoundryOperations the CloudFoundryOperations bean
	 * @param cloudFoundryClient the CloudFoundryClient bean
	 * @param targetProperties the CloudFoundryTargetProperties bean
	 * @param serviceBindingInterceptor
	 * @return the bean
	 */
	@Bean
	public OsbCmdbServiceBinding osbCmdbServiceBinding(
		CloudFoundryOperations cloudFoundryOperations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		@Autowired(required = false)
		ServiceBindingInterceptor serviceBindingInterceptor) {
		return new OsbCmdbServiceBinding(cloudFoundryClient, targetProperties.getDefaultOrg(),
			targetProperties.getUsername(), cloudFoundryOperations, serviceBindingInterceptor);
	}



}
