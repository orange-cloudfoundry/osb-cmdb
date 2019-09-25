package com.orange.oss.osbcmdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.appbroker.deployer.*;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class OsbCmdbApplication {

    public static void main(String[] args) {
		Hooks.onOperatorDebug(); //Turn on debugging
    	SpringApplication.run(OsbCmdbApplication.class, args);
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
