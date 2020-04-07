package com.orange.oss.osbcmdb.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("acceptanceTests")
public class TargetPropertiesConfiguration {

	//Inspired from spring-cloud-app-broker-autoconfigure/src/main/java/org/springframework/cloud/appbroker/autoconfigure/CloudFoundryAppDeployerAutoConfiguration.java
	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.acceptance-test.cloudfoundry";

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX)
	CloudFoundryTargetProperties cloudFoundryTargetProperties() {
		return new CloudFoundryTargetProperties();
	}


	//Inspired from spring-cloud-app-broker-autoconfigure/src/main/java/org/springframework/cloud/appbroker/autoconfigure/CloudFoundryAppDeployerAutoConfiguration.java
	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".properties")
	CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties() {
		return new CloudFoundryDeploymentProperties();
	}

}
