package com.orange.oss.osbcmdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration;
import org.springframework.cloud.appbroker.deployer.*;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryOperationsUtils;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationServiceImpl;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationServiceNoOp;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.context.annotation.Bean;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import reactor.core.publisher.Hooks;

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

}
