package com.orange.oss.osbcmdb;

import com.orange.oss.osbcmdb.metadata.CfAnnotationValidator;
import com.orange.oss.osbcmdb.metadata.CfMetadataFormatter;
import com.orange.oss.osbcmdb.metadata.CreateServiceMetadataFormatterServiceImpl;
import com.orange.oss.osbcmdb.metadata.K8SMetadataFormatter;
import com.orange.oss.osbcmdb.metadata.UpdateServiceMetadataFormatterService;
import com.orange.oss.osbcmdb.servicebinding.OsbCmdbServiceBinding;
import com.orange.oss.osbcmdb.servicebinding.ServiceBindingInterceptor;
import com.orange.oss.osbcmdb.serviceinstance.ApiInfoLocationHeaderFilter;
import com.orange.oss.osbcmdb.serviceinstance.MaintenanceInfoFormatterService;
import com.orange.oss.osbcmdb.serviceinstance.OsbCmdbServiceInstance;
import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncOnlyBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.ASyncStalledUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.AsyncSuccessfulUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.BackingServiceBindingInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedCreateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedDeleteBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncFailedUpdateBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncOnlyBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncSuccessfulBackingSpaceInstanceInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor;
import com.orange.oss.osbcmdb.testfixtures.SyncTimeoutCreateBackingSpaceInstanceInterceptor;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.servicebroker.autoconfigure.web.MaintenanceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Profile("!offline-test-without-scab")
@Configuration
@EnableConfigurationProperties
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
	@Profile("acceptanceTests & SyncOnlyBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncOnlyBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncOnlyBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & ASyncOnlyBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestASyncOnlyBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncOnlyBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncSuccessfulBackingSpaceInstanceWithoutDashboardInInitialVersionInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & ASyncFailedCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestFailedAsyncBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncFailedCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & ASyncStalledCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestASyncStalledCreateBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncStalledCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceAsyncSuccessfulCreateBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new AsyncSuccessfulCreateUpdateDeleteBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncFailedCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncFailedCreateBackingServiceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncFailedCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & SyncTimeoutCreateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestSyncTimeoutCreateBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new SyncTimeoutCreateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
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
	@Profile("acceptanceTests & ASyncStalledDeleteBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestASyncStalledDeleteBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncStalledDeleteBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
	}

	@Bean
	@Profile("acceptanceTests & ASyncStalledUpdateBackingSpaceInstanceInterceptor")
	public ServiceInstanceInterceptor acceptanceTestASyncStalledUpdateBackingSpaceInstanceInterceptor(
		CloudFoundryTargetProperties targetProperties) {
		return new ASyncStalledUpdateBackingSpaceInstanceInterceptor(targetProperties.getDefaultSpace());
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

	@Bean
	@ConfigurationProperties(prefix = OsbCmdbBrokerProperties.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public OsbCmdbBrokerProperties osbCmdbBrokerProperties() {
		return new OsbCmdbBrokerProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = MaintenanceInfoFormatterService.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public MaintenanceInfo osbCmdbMaintenanceInfo() {
		return new MaintenanceInfo();
	}

	@Bean
	public MaintenanceInfoFormatterService maintenanceInfoFormatterService(
		@Autowired(required = false)
			MaintenanceInfo osbCmdbMaintenanceInfo) {
		boolean missingOrEmptyMIConfig = osbCmdbMaintenanceInfo == null ||
			(osbCmdbMaintenanceInfo.getVersion() == null && osbCmdbMaintenanceInfo.getDescription() == null);
		return new MaintenanceInfoFormatterService(missingOrEmptyMIConfig ? null: osbCmdbMaintenanceInfo.toModel());
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
		Environment environment,
		OsbCmdbBrokerProperties osbCmdbBrokerProperties,
		MaintenanceInfoFormatterService maintenanceInfoFormatterService) {

		String acceptanceTestsProfile = "acceptanceTests";
		if (serviceInstanceInterceptor == null && environment.acceptsProfiles(Profiles.of(acceptanceTestsProfile))) {
			throw new IllegalArgumentException("With " + acceptanceTestsProfile + " profile, at least one interceptor" +
				" profile should be defined to mock backing service broker");
		}
		CfAnnotationValidator annotationValidator = new CfAnnotationValidator();
		return new OsbCmdbServiceInstance(cloudFoundryOperations, cloudFoundryClient,
			targetProperties.getDefaultOrg(), targetProperties.getUsername(),
			serviceInstanceInterceptor, new CreateServiceMetadataFormatterServiceImpl(new K8SMetadataFormatter(),
			new CfMetadataFormatter(annotationValidator)),
			new UpdateServiceMetadataFormatterService(new K8SMetadataFormatter(),
				new CfMetadataFormatter(annotationValidator)), osbCmdbBrokerProperties.isPropagateMetadataAsCustomParam(),
			osbCmdbBrokerProperties.isHideMetadataCustomParamInGetServiceInstanceEndpoint(),
			maintenanceInfoFormatterService);
	}

	@Bean
	public ApiInfoLocationHeaderFilter apiInfoLocationHeaderFilter(OsbCmdbBrokerProperties osbCmdbBrokerProperties, CloudFoundryTargetProperties targetProperties) {

		String apiHost = targetProperties.getApiHost();
		if (! osbCmdbBrokerProperties.isWhiteListOsbCmdbCloudFoundryXApiInfoLocationHeader()) {
			apiHost = "";
		}
		return new ApiInfoLocationHeaderFilter(osbCmdbBrokerProperties.getExpectedXApiInfoLocationHeader(),
			osbCmdbBrokerProperties.isRejectRequestsWithNonMatchingXApiInfoLocationHeader(), apiHost);
	}



}
