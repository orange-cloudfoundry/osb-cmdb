/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.MaintenanceInfo;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationManagerRequest;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationManagerResponse;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationUserRequest;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationUserResponse;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationManagerRequest;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationUserRequest;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersResponse;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperRequest;
import org.cloudfoundry.client.v3.serviceinstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v3.serviceinstances.ListServiceInstancesResponse;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.domains.CreateDomainRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.CreateOrganizationRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.serviceadmin.UpdateServiceBrokerRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceKeyRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.cloudfoundry.util.ExceptionUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.cloudfoundry.util.tuple.TupleUtils.function;

@Service
public class CloudFoundryService {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryService.class);

	private static final String DEPLOYER_PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.cloudfoundry.";

	private static final int EXPECTED_PROPERTY_PARTS = 2;

	public static final String BROKER_USERNAME = "user";

	public static final String BROKER_PASSWORD = "password";

	private final CloudFoundryClient cloudFoundryClient;

	private final CloudFoundryOperations cloudFoundryOperations;

	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryService(CloudFoundryClient cloudFoundryClient,
		CloudFoundryOperations cloudFoundryOperations,
		CloudFoundryProperties cloudFoundryProperties) {
		this.cloudFoundryClient = cloudFoundryClient;
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.cloudFoundryProperties = cloudFoundryProperties;
	}

	public Mono<Void> enableServiceBrokerAccess(String serviceName) {
		return cloudFoundryOperations.serviceAdmin()
			.enableServiceAccess(EnableServiceAccessRequest.builder()
				.serviceName(serviceName)
				.build())
			.doOnSuccess(item -> LOG.info("Enabled access to service " + serviceName))
			.doOnError(error -> LOG.error("Error enabling access to service " + serviceName + ": " + error));
	}

	public Mono<Void> createServiceBroker(String brokerName, String testBrokerAppName,
		boolean ignoreBrokerRegistrationErrors) {
		return getApplicationRoute(testBrokerAppName)
			.flatMap(url -> {
				Mono<Void> registerBrokerMono = cloudFoundryOperations.serviceAdmin()
					.create(CreateServiceBrokerRequest.builder()
						.name(brokerName)
						.username(BROKER_USERNAME)
						.password(BROKER_PASSWORD)
						.url(url)
						.build())
					.doOnSuccess(item -> LOG.info("Created service broker " + brokerName));
				if (ignoreBrokerRegistrationErrors) {
					return registerBrokerMono
						.doOnError(error -> LOG.error("Error creating service broker " + brokerName + ": " + error));
				} else {
					return registerBrokerMono;
				}
			});
	}

	public Mono<Void> updateServiceBroker(String brokerName, String testBrokerAppName) {
		return getApplicationRoute(testBrokerAppName)
			.flatMap(url -> cloudFoundryOperations.serviceAdmin()
				.update(UpdateServiceBrokerRequest.builder()
					.name(brokerName)
					.username(BROKER_USERNAME)
					.password(BROKER_PASSWORD)
					.url(url)
					.build())
				.doOnSuccess(item -> LOG.info("Updating service broker " + brokerName))
				.doOnError(error -> LOG.error("Error updating service broker " + brokerName + ": " + error)));
	}

	public Mono<String> getApplicationRoute(String appName) {
		return cloudFoundryOperations.applications()
			.get(GetApplicationRequest.builder()
				.name(appName)
				.build())
			.doOnSuccess(item -> LOG.info("Got route for app " + appName))
			.doOnError(error -> LOG.error("Error getting route for app " + appName + ": " + error))
			.map(ApplicationDetail::getUrls)
			.flatMapMany(Flux::fromIterable)
			.next()
			.map(url -> "https://" + url);
	}

	public Mono<Void> pushBrokerApp(String appName, Path appPath, String brokerClientId,
		List<String> appBrokerProperties) {
		return cloudFoundryOperations.applications()
			.pushManifest(PushApplicationManifestRequest.builder()
				.manifest(ApplicationManifest.builder()
					.environmentVariables(appBrokerDeployerEnvironmentVariables(brokerClientId))
					.putAllEnvironmentVariables(propertiesToEnvironment(appBrokerProperties))
					.name(appName)
					.path(appPath)
					.memory(1024)
					.build())
				.build())
			.doOnSuccess(item -> LOG.info("Pushed broker app " + appName))
			.doOnError(error -> LOG.error("Error pushing broker app " + appName + ": " + error));
	}

	public Mono<Void> logAndVerifyRecentAppLogs(String appName, final boolean assertNoErrorLog) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Dumping recent logs for broker {}", appName);
		}
		return cloudFoundryOperations.applications().logs(LogsRequest.builder().name(appName).recent(true).build())
			.map(LogMessage::toString)
			.doOnNext(l  -> LOG.debug("{}", l))
			.doOnComplete(()  -> LOG.debug("log stream completed"))
			.doOnError(error -> LOG.debug("Error getting logs for app " + appName + " : " + error))
			.onErrorResume(e -> {
				LOG.error("Unable to log and assert broker logs {}", e.toString());
				return Mono.empty();
			})
			.filter(logString -> logString.contains(" ERROR "))
			.collectList()
			.doOnNext(errorLogs -> {
				if (assertNoErrorLog) {
					//Trying to fail on ERROR logs such as
					//2020-04-23 07:38:42.279 ERROR 7 --- [nio-8080-exec-6] c.o.o.o.s.OsbCmdbServiceInstance         : Unexpected si state after delete delete full si is ServiceInstance{applications=[], id=c9c8595e-1f39-4406-810f-5a8f5edb6a56, name=d94467fd-d8ac-4b36-9863-10c85578c695, plan=standard, service=app-service-delete-instance-with-async-backing-failure, type=managed_service_instance, dashboardUrl=null, description=A service that deploys a backing app, documentationUrl=null, lastOperation=delete, message=, startedAt=2020-04-23T07:38:41Z, status=in progress, tags=[], updatedAt=2020-04-23T07:38:41Z}, messageType=OUT, sourceInstance=0, sourceType=APP/PROC/WEB, timestamp=1587627522280180256}
					assertThat(errorLogs)
						.withFailMessage("Expecting no ERROR log entry in broker recent logs, but got:" + errorLogs)
						.isEmpty();
				}
			})
			.then();
	}

	public Mono<Void> updateBrokerApp(String appName, String brokerClientId, List<String> appBrokerProperties) {
		return cloudFoundryOperations.applications()
			.get(GetApplicationRequest.builder().name(appName).build())
			.map(ApplicationDetail::getId)
			.flatMap(applicationId ->
				cloudFoundryClient
					.applicationsV2()
					.update(UpdateApplicationRequest
						.builder()
						.applicationId(applicationId)
						.putAllEnvironmentJsons(appBrokerDeployerEnvironmentVariables(brokerClientId))
						.putAllEnvironmentJsons(propertiesToEnvironment(appBrokerProperties))
						.name(appName)
						.memory(1024)
						.build())
					.thenReturn(applicationId))
			.then(cloudFoundryOperations.applications()
				.restart(RestartApplicationRequest.builder().name(appName).build()))
			.doOnSuccess(item -> LOG.info("Updated broker app " + appName))
			.doOnError(error -> LOG.error("Error updating broker app " + appName + ": " + error))
			.then();
	}

	public Mono<Void> deleteApp(String appName) {
		return cloudFoundryOperations.applications()
			.delete(DeleteApplicationRequest.builder()
				.name(appName)
				.deleteRoutes(true)
				.build())
			.doOnSuccess(item -> LOG.info("Deleted app " + appName))
			.doOnError(error -> LOG.warn("Error deleting app " + appName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceBroker(String brokerName) {
		return cloudFoundryOperations.serviceAdmin()
			.delete(DeleteServiceBrokerRequest.builder()
				.name(brokerName)
				.build())
			.doOnSuccess(item -> LOG.info("Deleted service broker " + brokerName))
			.doOnError(error -> LOG.warn("Error deleting service broker " + brokerName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName) {
		return deleteServiceInstance(serviceInstanceName, Duration.ofSeconds(5*60));
	}
	public Mono<Void> deleteServiceInstanceWithoutCatchingException(String serviceInstanceName) {
		return deleteServiceInstanceWithoutCatchingException(serviceInstanceName, Duration.ofSeconds(5*60));
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName, Duration completionTimeout) {
		return getServiceInstance(serviceInstanceName)
			.flatMap(si -> cloudFoundryOperations.services()
				.deleteInstance(DeleteServiceInstanceRequest.builder()
					.name(si.getName())
					.completionTimeout(completionTimeout)
					.build())
				.doOnSuccess(item -> LOG.info("Deleted service instance " + serviceInstanceName))
				.doOnError(
					error -> LOG.error("Error deleting service instance " + serviceInstanceName + ": " + error))
				.onErrorResume(e -> Mono.empty()))
			.doOnError(error -> LOG.warn("Error getting service instance " + serviceInstanceName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceInstanceWithoutCatchingException(String serviceInstanceName, Duration completionTimeout) {
		return getServiceInstance(serviceInstanceName)
			.flatMap(si -> cloudFoundryOperations.services()
				.deleteInstance(DeleteServiceInstanceRequest.builder()
					.name(si.getName())
					.completionTimeout(completionTimeout)
					.build())
				.doOnSuccess(item -> LOG.info("Deleted service instance " + serviceInstanceName))
				.doOnError(
					error -> LOG.error("Error deleting service instance " + serviceInstanceName + ": " + error)));
	}

	public Mono<Void> purgeServiceInstance(String serviceInstanceName) {
		return getServiceInstance(serviceInstanceName)
			.flatMap(si -> cloudFoundryClient.serviceInstances()
				.delete(org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest.builder()
					.serviceInstanceId(si.getId())
					.purge(true)
					.build())
				.then()
				.doOnSuccess(item -> LOG.info("Purging service instance " + serviceInstanceName))
				.doOnError(
					error -> LOG.error("Error Purging service instance " + serviceInstanceName + ": " + error))
				.onErrorResume(e -> Mono.empty()))
			.doOnError(error -> LOG.warn("Error getting service instance " + serviceInstanceName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> purgeServiceInstance(String serviceInstanceName, String spaceName) {
		return getServiceInstance(serviceInstanceName, spaceName)
			.flatMap(si -> cloudFoundryClient.serviceInstances()
				.delete(org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest.builder()
					.serviceInstanceId(si.getId())
					.purge(true)
					.build())
				.then()
				.doOnSuccess(item -> LOG.info("Deleted service instance " + serviceInstanceName))
				.doOnError(
					error -> LOG.error("Error deleting service instance " + serviceInstanceName + ": " + error))
				.onErrorResume(e -> Mono.empty()))
			.doOnError(error -> LOG.warn("Error getting service instance " + serviceInstanceName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> createServiceInstance(String planName,
		String serviceName,
		String serviceInstanceName,
		Map<String, Object> parameters) {
		return createServiceInstance(planName, serviceName, serviceInstanceName, parameters, Duration.ofSeconds(30));
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	protected Mono<String> getSpacedIdFromTargettedOperationsInternals() {
		DefaultCloudFoundryOperations spacedTargetedOperationsInternals = (DefaultCloudFoundryOperations) cloudFoundryOperations;
		return spacedTargetedOperationsInternals.getSpaceId();
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	private static Mono<ServiceResource> getSpaceService(CloudFoundryClient cloudFoundryClient, String spaceId, String service) {
		return requestListServices(cloudFoundryClient, spaceId, service)
			.single()
			.onErrorResume(NoSuchElementException.class, t -> ExceptionUtils.illegalArgument("Service %s does not exist", service));
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	private static Flux<ServiceResource> requestListServices(CloudFoundryClient cloudFoundryClient, String spaceId, String serviceName) {
		return PaginationUtils
			.requestClientV2Resources(page -> cloudFoundryClient.spaces()
				.listServices(ListSpaceServicesRequest.builder()
					.label(serviceName)
					.page(page)
					.spaceId(spaceId)
					.build()));
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	private static Mono<String> getServiceIdByName(CloudFoundryClient cloudFoundryClient, String spaceId, String service) {
		return getSpaceService(cloudFoundryClient, spaceId, service)
			.map(ResourceUtils::getId);
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	private static Mono<String> getServicePlanIdByName(CloudFoundryClient cloudFoundryClient, String serviceId, String plan) {
		return requestListServicePlans(cloudFoundryClient, serviceId)
			.filter(resource -> plan.equals(ResourceUtils.getEntity(resource).getName()))
			.single()
			.map(ResourceUtils::getId)
			.onErrorResume(
				NoSuchElementException.class, t -> ExceptionUtils.illegalArgument("Service plan %s does not exist", plan));
	}

	//Duplicated from cloudfoundry-operations-4.8.0.RELEASE.jar!/org/cloudfoundry/operations/services/DefaultServices.class
	private static Flux<ServicePlanResource> requestListServicePlans(CloudFoundryClient cloudFoundryClient, String serviceId) {
		return PaginationUtils
			.requestClientV2Resources(page -> cloudFoundryClient.servicePlans()
				.list(ListServicePlansRequest.builder()
					.page(page)
					.serviceId(serviceId)
					.build()));
	}

	/**
	 * Low level CF java client variant of createService which takes an acceptsIncomplete
	 * Had to duplicate many methods from cf-java-client because they're private static
	 * Following CF java client, a PR to make these methods public could reduce duplication
	 */
	public Mono<Void> createServiceInstanceLowLevel(
		String servicePlanName,
		String serviceName,
		Map<String, Object> parameters, Boolean acceptsIncomplete) {
		return Mono
			.zip(Mono.just(this.cloudFoundryClient), getSpacedIdFromTargettedOperationsInternals())
			.flatMap(function((cloudFoundryClient, spaceId) -> Mono.zip(
				Mono.just(cloudFoundryClient),
				Mono.just(spaceId),
				getServiceIdByName(cloudFoundryClient, spaceId, serviceName)
			)))
			.flatMap(function((cloudFoundryClient, spaceId, serviceId) -> Mono.zip(
				Mono.just(cloudFoundryClient),
				Mono.just(spaceId),
				getServicePlanIdByName(cloudFoundryClient, serviceId, servicePlanName)
			)))
			.flatMap( t -> cloudFoundryClient.serviceInstances()
				.create(org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest.builder()
					.name("a-random-service-instance-for-tests-" + new Random().nextInt(100))
					.servicePlanId(t.getT3())
					.acceptsIncomplete(acceptsIncomplete)
					.spaceId(t.getT2())
					.parameters(parameters)
					.build()))
			.doOnSuccess(item -> LOG.info("Created service instance " + item.getEntity()))
			.doOnError(error -> LOG.error("Error creating service instance: " + error))
			.then();

	}


	public Mono<Void> createServiceInstance(String planName, String serviceName, String serviceInstanceName,
		Map<String, Object> parameters, Duration completionTimeout) {
		return cloudFoundryOperations.services()
			.createInstance(CreateServiceInstanceRequest.builder()
				.planName(planName)
				.serviceName(serviceName)
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.completionTimeout(completionTimeout)
				.build())
			.doOnSuccess(item -> LOG.info("Created service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error creating service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<Void> createServiceKey(String serviceKeyName, String serviceInstanceName,
			Map<String, Object> parameters) {
		return cloudFoundryOperations.services()
			.createServiceKey(CreateServiceKeyRequest.builder()
				.serviceKeyName(serviceKeyName)
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess(item -> LOG.info("Created service key " + serviceKeyName + " for instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error creating service key " + serviceKeyName
				+ " for instance " + serviceInstanceName + ": " +  error));
	}

	public Mono<Void> deleteServiceKey(String serviceInstanceName, String serviceKeyName) {
		return cloudFoundryOperations.services()
			.deleteServiceKey(DeleteServiceKeyRequest.builder()
				.serviceKeyName(serviceKeyName)
				.serviceInstanceName(serviceInstanceName)
				.build())
			.doOnSuccess(item -> LOG.info("Deleted service key " + serviceKeyName + " for instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error Deleting service key " + serviceKeyName
				+ " for instance " + serviceInstanceName + ": " +  error));
	}

	public Mono<Void> updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		return cloudFoundryOperations.services()
			.updateInstance(UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess(item -> LOG.info("Updated service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error updating service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<Void> syncUpgradeServiceInstance(final String serviceInstanceName, final String version) {
		return cloudFoundryOperations.services()
			.updateInstance(UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.maintenanceInfo(MaintenanceInfo.builder()
					.version(version)
					.build())
				.build())
			.doOnSuccess(item -> LOG.info("Upgrated service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error upgrating service instance " + serviceInstanceName + ": " + error));
	}



	public Mono<Void> updateServiceInstance(String serviceInstanceName, String planName, Duration completionTimeout) {
		return cloudFoundryOperations.services()
			.updateInstance(UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.planName(planName)
				.completionTimeout(completionTimeout)
				.build())
			.doOnSuccess(item -> LOG.info("Updated service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error updating service instance " + serviceInstanceName + ": " + error));
	}

	public Flux<ServiceInstanceSummary> listServiceInstances() {
		return listServiceInstances(this.cloudFoundryOperations);
	}

	public Flux<ServiceInstanceSummary> listServiceInstances(String space) {
		return listServiceInstances(createOperationsForSpace(space));
	}

	public Flux<ServiceInstanceSummary> listServiceInstances(CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services().listInstances();
	}

	public Flux<ServiceKey> listServiceKeys(String serviceInstanceName, String space) {
		return createOperationsForSpace(space).services().listServiceKeys(
			ListServiceKeysRequest.builder()
			.serviceInstanceName(serviceInstanceName).build());
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName) {
		return getServiceInstance(cloudFoundryOperations, serviceInstanceName);
	}

	public Mono<ServiceKey> getServiceKey(String serviceInstanceName, String serviceKeyName) {
		return getServiceKey(cloudFoundryOperations, serviceInstanceName, serviceKeyName);
	}

	public Mono<ServiceKey> getServiceKey(String serviceInstanceName, String serviceKeyName, String space) {
		return getServiceKey(createOperationsForSpace(space), serviceInstanceName, serviceKeyName);
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName, String space) {
		return getServiceInstance(createOperationsForSpace(space), serviceInstanceName);
	}

	private Mono<ServiceInstance> getServiceInstance(CloudFoundryOperations operations,
		String serviceInstanceName) {
		return operations.services()
			.getInstance(GetServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.build())
			.doOnSuccess(item -> LOG.info("Got service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error getting service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<Map<String, Object>> getServiceInstanceParams(String serviceInstanceId) {
		return cloudFoundryClient.serviceInstances().
			getParameters(GetServiceInstanceParametersRequest.builder()
				.serviceInstanceId(serviceInstanceId)
				.build())
			.map(GetServiceInstanceParametersResponse::getParameters);
	}

	public Mono<ServiceInstanceEntity> getServiceInstanceEntity(String serviceInstanceId) {
		return cloudFoundryClient.serviceInstances()
			.get(org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest.builder()
				.serviceInstanceId(serviceInstanceId)
				.build())
			.map(GetServiceInstanceResponse::getEntity);
	}

	public Flux<ServiceInstanceResource> listServiceInstanceMetadataByLabel(String labelSelector) {
		return cloudFoundryClient.serviceInstancesV3()
		.list(ListServiceInstancesRequest.builder()
			.labelSelector(labelSelector)
			.build())
			.map(ListServiceInstancesResponse::getResources)
			.flatMapMany(Flux::fromIterable);
	}

	private Mono<ServiceKey> getServiceKey(CloudFoundryOperations operations,
		String serviceInstanceName, String serviceKeyName) {

		return operations.services()
			.getServiceKey(GetServiceKeyRequest.builder()
				.serviceKeyName(serviceKeyName)
				.serviceInstanceName(serviceInstanceName)
				.build())
			.doOnSuccess(item -> LOG.info("Got service key " + serviceKeyName + " for service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error getting service key " + serviceKeyName + " for service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<List<ApplicationSummary>> getApplications() {
		return listApplications(cloudFoundryOperations)
			.collectList();
	}

	public Mono<ApplicationDetail> getApplication(String appName) {
		return cloudFoundryOperations.applications().get(GetApplicationRequest.builder()
			.name(appName)
			.build());
	}

	public Mono<ApplicationSummary> getApplication(String appName, String space) {
		return listApplications(createOperationsForSpace(space))
			.filter(applicationSummary -> applicationSummary.getName().equals(appName))
			.single();
	}

	private Flux<ApplicationSummary> listApplications(CloudFoundryOperations operations) {
		return operations.applications()
			.list()
			.doOnComplete(() -> LOG.info("Listed applications"))
			.doOnError(error -> LOG.error("Error listing applications: " + error));
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName) {
		return getApplicationEnvironment(cloudFoundryOperations, appName);
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName, String space) {
		return getApplicationEnvironment(createOperationsForSpace(space), appName);
	}

	private Mono<ApplicationEnvironments> getApplicationEnvironment(CloudFoundryOperations operations, String appName) {
		return operations.applications()
			.getEnvironments(GetApplicationEnvironmentsRequest.builder()
				.name(appName)
				.build())
			.doOnSuccess(item -> LOG.info("Got environment for application " + appName))
			.doOnError(error -> LOG.error("Error getting environment for application " + appName + ": " + error));
	}

	public Mono<Void> stopApplication(String appName) {
		return cloudFoundryOperations.applications().stop(StopApplicationRequest.builder()
			.name(appName)
			.build());
	}

	public Mono<List<String>> getSpaces() {
		return cloudFoundryOperations.spaces()
			.list()
			.doOnComplete(() -> LOG.info("Listed spaces"))
			.doOnError(error -> LOG.error("Error listing spaces: " + error))
			.map(SpaceSummary::getName)
			.collectList();
	}

	public Mono<SpaceSummary> getOrCreateDefaultSpace() {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();

		Spaces spaceOperations = DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.cloudFoundryOperations)
			.organization(cloudFoundryProperties.getDefaultOrg())
			.build()
			.spaces();

		final String defaultSpace = getDefaultSpaceName();
		return getDefaultSpace(spaceOperations)
			.switchIfEmpty(spaceOperations.create(CreateSpaceRequest
				.builder()
				.name(defaultSpace)
				.organization(defaultOrg)
				.build())
				.then(getDefaultSpace(spaceOperations)));
	}

	public String getDefaultSpaceName() {
		return cloudFoundryProperties.getDefaultSpace();
	}

	public Mono<OrganizationSummary> getOrCreateDefaultOrganization() {
		Organizations organizationOperations = cloudFoundryOperations.organizations();

		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return getDefaultOrg(organizationOperations)
			.switchIfEmpty(organizationOperations
				.create(CreateOrganizationRequest
					.builder()
					.organizationName(defaultOrg)
					.build())
				.then(getDefaultOrg(organizationOperations)));
	}

	private Mono<OrganizationSummary> getDefaultOrg(Organizations orgOperations) {
		return orgOperations.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultOrg()))
			.next();
	}

	private Mono<SpaceSummary> getDefaultSpace(Spaces spaceOperations) {
		return spaceOperations.list()
			.filter(r -> r
				.getName()
				.equals(getDefaultSpaceName()))
			.next();
	}

	public Mono<Void> associateAppBrokerClientWithOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap(userId -> associateOrgUser(orgId, userId)
				.then(associateOrgManager(orgId, userId))
				.then(associateSpaceDeveloper(spaceId, userId)))
			.then();
	}

	public Mono<Void> removeAppBrokerClientFromOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap(userId -> removeSpaceDeveloper(spaceId, userId)
				.then(removeOrgManager(orgId, userId))
				.then(removeOrgUser(orgId, userId)));
	}

	public Mono<Void> createDomain(String domain) {
		return cloudFoundryOperations
			.domains()
			.create(CreateDomainRequest
				.builder()
				.domain(domain)
				.organization(cloudFoundryProperties.getDefaultOrg())
				.build())
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteDomain(String domain) {
		return cloudFoundryOperations
			.domains()
			.list()
			.filter(d -> d.getName().equals(domain))
			.map(Domain::getId)
			.flatMap(domainId -> cloudFoundryClient
				.privateDomains()
				.delete(DeletePrivateDomainRequest
					.builder()
					.privateDomainId(domainId)
					.build()))
			.then();
	}

	private Mono<AssociateOrganizationUserResponse> associateOrgUser(String orgId, String userId) {
		return cloudFoundryClient.organizations().associateUser(AssociateOrganizationUserRequest.builder()
			.organizationId(orgId)
			.userId(userId)
			.build());
	}

	private Mono<AssociateOrganizationManagerResponse> associateOrgManager(String orgId, String userId) {
		return cloudFoundryClient.organizations().associateManager(AssociateOrganizationManagerRequest.builder()
			.organizationId(orgId)
			.managerId(userId)
			.build());
	}

	private Mono<AssociateSpaceDeveloperResponse> associateSpaceDeveloper(String spaceId, String userId) {
		return cloudFoundryClient.spaces().associateDeveloper(AssociateSpaceDeveloperRequest.builder()
			.spaceId(spaceId)
			.developerId(userId)
			.build());
	}

	private Mono<Void> removeOrgUser(String orgId, String userId) {
		return cloudFoundryClient.organizations().removeUser(RemoveOrganizationUserRequest.builder()
			.organizationId(orgId)
			.userId(userId)
			.build());
	}

	private Mono<Void> removeOrgManager(String orgId, String userId) {
		return cloudFoundryClient.organizations().removeManager(RemoveOrganizationManagerRequest.builder()
			.organizationId(orgId)
			.managerId(userId)
			.build());
	}

	private Mono<Void> removeSpaceDeveloper(String spaceId, String userId) {
		return cloudFoundryClient.spaces().removeDeveloper(RemoveSpaceDeveloperRequest.builder()
			.spaceId(spaceId)
			.developerId(userId)
			.build());
	}

	private CloudFoundryOperations createOperationsForSpace(String space) {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) cloudFoundryOperations)
			.organization(defaultOrg)
			.space(space)
			.build();
	}

	private Map<String, String> appBrokerDeployerEnvironmentVariables(String brokerClientId) {
		Map<String, String> deployerVariables = new HashMap<>();
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-host",
			cloudFoundryProperties.getApiHost());
		// systematically configure expected x-api-info-location here since it is easier to derive it from CF API host
		deployerVariables.put("osbcmdb.broker.expectedXApiInfoLocationHeader", cloudFoundryProperties.getApiHost() +
			"/v2/info");
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-port",
			String.valueOf(cloudFoundryProperties.getApiPort()));
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-org",
			cloudFoundryProperties.getDefaultOrg());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-space",
			getDefaultSpaceName());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "skip-ssl-validation",
			String.valueOf(cloudFoundryProperties.isSkipSslValidation()));
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "properties.memory", "1024M");
		if (cloudFoundryProperties.getUsername() != null && cloudFoundryProperties.getPassword() != null) {
			deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "username", cloudFoundryProperties.getUsername());
			deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + BROKER_PASSWORD, cloudFoundryProperties.getPassword());
		} else {
			deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "client-id", brokerClientId);
			deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "client-secret",
				CloudFoundryClientConfiguration.APP_BROKER_CLIENT_SECRET);
		}
		return deployerVariables;
	}

	private Map<String, String> propertiesToEnvironment(List<String> properties) {
		Map<String, String> environment = new HashMap<>();
		for (String property : properties) {
			final String[] propertyKeyValue = property.split("=");
			if (propertyKeyValue.length == EXPECTED_PROPERTY_PARTS) {
				environment.put(propertyKeyValue[0], propertyKeyValue[1]);
			}
			else {
				throw new IllegalArgumentException(format("App Broker property '%s' is incorrectly formatted",
					Arrays.toString(propertyKeyValue)));
			}
		}
		return environment;
	}

}
