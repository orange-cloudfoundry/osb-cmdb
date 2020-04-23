package com.orange.oss.osbcmdb;

import com.orange.oss.osbcmdb.metadata.CreateServiceMetadataFormatterServiceImpl;
import com.orange.oss.osbcmdb.metadata.UpdateServiceMetadataFormatterService;
import com.orange.oss.osbcmdb.servicebinding.OsbCmdbServiceBinding;
import com.orange.oss.osbcmdb.servicebinding.ServiceBindingInterceptor;
import com.orange.oss.osbcmdb.serviceinstance.OsbCmdbServiceInstance;
import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.BackingServiceBindingInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncSuccessfulBackingSpaceInstanceInterceptor;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Profile("!offline-test-without-scab")
@Configuration
public class OsbCmdbBrokerConfiguration {


	@Bean
	@Profile("acceptanceTests & AsyncFailedDeleteBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestAsyncFailedDeleteBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new AsyncFailedDeleteBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & AsyncFailedUpdateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestAsyncFailedUpdateBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new AsyncFailedUpdateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncSuccessfulBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncSuccessfulBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & ASyncFailedCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestFailedAsyncBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncFailedCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceAsyncSuccessfulCreateBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new AsyncSuccessfulCreateDeleteBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncFailedCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncFailedCreateBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncFailedCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncFailedDeleteBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncFailedDeleteBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncFailedDeleteBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncFailedUpdateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncFailedUpdateBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncFailedUpdateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncSuccessfulUpdateBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests")
	@ConditionalOnMissingBean
	public ServiceBindingInterceptor noopServiceBindingInterceptor(CloudFoundryTargetProperties targetProperties) {
		return new BackingServiceBindingInterceptor(targetProperties.getDefaultSpace());
	}

	/**
	 * Provide a {@link OsbCmdbServiceBinding} bean
	 *
	 * @param cloudFoundryOperations the CloudFoundryOperations bean
	 * @param cloudFoundryClient the CloudFoundryClient bean
	 * @param targetProperties the CloudFoundryTargetProperties bean
	 * @param serviceBindingInterceptor an optional bean supporting testing
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

	/**
	 * Provide a {@link OsbCmdbServiceInstance} bean
	 *
	 * @param cloudFoundryOperations the CloudFoundryOperations bean
	 * @param cloudFoundryClient the CloudFoundryClient bean
	 * @param targetProperties the CloudFoundryTargetProperties bean
	 * @param serviceInstanceInterceptor an optional bean when not running acceptance tests.
	 * Otherwise, it is mandatory in order to mock backing brokers
	 * @return the bean
	 */
	@Bean
	public OsbCmdbServiceInstance osbCmdbServiceInstance(CloudFoundryOperations cloudFoundryOperations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		@Autowired(required = false)
			ServiceInstanceInterceptor serviceInstanceInterceptor,
		Environment environment) {
		String acceptanceTestsProfile = "acceptanceTests";
		if (serviceInstanceInterceptor == null && environment.acceptsProfiles(Profiles.of(acceptanceTestsProfile))) {
			throw new IllegalArgumentException("With " + acceptanceTestsProfile + " profile, at least one interceptor" +
				" profile should be defined to mock backing service broker");
		}
		return new OsbCmdbServiceInstance(cloudFoundryOperations, cloudFoundryClient,
			targetProperties.getDefaultOrg(), targetProperties.getUsername(),
			serviceInstanceInterceptor, new CreateServiceMetadataFormatterServiceImpl(),
			new UpdateServiceMetadataFormatterService());
	}

}
