/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.client.v2.spaces.DeleteSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.builds.BuildState;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.CreateBuildResponse;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildResponse;
import org.cloudfoundry.client.v3.deployments.CreateDeploymentRequest;
import org.cloudfoundry.client.v3.deployments.CreateDeploymentResponse;
import org.cloudfoundry.client.v3.deployments.DeploymentRelationships;
import org.cloudfoundry.client.v3.deployments.DeploymentState;
import org.cloudfoundry.client.v3.deployments.GetDeploymentRequest;
import org.cloudfoundry.client.v3.deployments.GetDeploymentResponse;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.GetPackageResponse;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.util.DelayUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.CreateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.CreateServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.DeleteServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.DeleteServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.DeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationRequest;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationResponse;
import org.springframework.cloud.appbroker.deployer.UpdateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.UpdateServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.util.ByteSizeUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@SuppressWarnings("PMD.GodClass")
public class CloudFoundryAppDeployer implements AppDeployer, ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(CloudFoundryAppDeployer.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final CloudFoundryDeploymentProperties defaultDeploymentProperties;

	private final CloudFoundryOperations operations;

	private final CloudFoundryClient client;

	private final CloudFoundryOperationsUtils operationsUtils;

	private final CloudFoundryTargetProperties targetProperties;

	private ResourceLoader resourceLoader;

	public CloudFoundryAppDeployer(CloudFoundryDeploymentProperties deploymentProperties,
								   CloudFoundryOperations operations,
								   CloudFoundryClient client,
								   CloudFoundryOperationsUtils operationsUtils,
								   CloudFoundryTargetProperties targetProperties,
								   ResourceLoader resourceLoader) {
		this.defaultDeploymentProperties = deploymentProperties;
		this.operations = operations;
		this.client = client;
		this.operationsUtils = operationsUtils;
		this.targetProperties = targetProperties;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
		String appName = request.getName();
		Resource appResource = getAppResource(request.getPath());
		Map<String, String> deploymentProperties = request.getProperties();

		logger.trace("Deploying application: request={}, resource={}",
			appName, appResource);

		return pushApplication(request, deploymentProperties, appResource)
				.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
				.doOnSuccess(item -> logger.info("Successfully deployed {}", appName))
				.doOnError(error -> {
					if (isNotFoundError().test(error)) {
						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
					}
					else {
						logError(String.format("Failed to deploy %s", appName)).accept(error);
					}
				})
				.thenReturn(DeployApplicationResponse.builder()
					.name(appName)
					.build());
	}

	@Override
	public Mono<UpdateApplicationResponse> update(UpdateApplicationRequest request) {
		final String name = request.getName();
		final Map<String, Object> environmentVariables =
			getApplicationEnvironment(request.getProperties(), request.getEnvironment(), request.getServiceInstanceId());

		return operationsUtils.getOperations(request.getProperties())
			.flatMap(cfOperations -> cfOperations.applications()
				.get(GetApplicationRequest.builder().name(name).build())
				.doOnRequest(l -> logger.debug("Getting application {}", name))
				.doOnSuccess(response -> logger.info("Success getting application {}", name))
				.doOnError(e -> logger.warn(String.format("Error getting application %s: %s", name, e.getMessage())))
				.map(ApplicationDetail::getId)
				.flatMap(applicationId ->
					updateApplicationEnvironment(applicationId, environmentVariables).thenReturn(applicationId)
				)
				.flatMap(applicationId -> Mono.zip(Mono.just(applicationId),
					createPackageForApplication(applicationId)))
				.map(tuple2 -> tuple2.mapT2(CreatePackageResponse::getId))
				.flatMap(tuple2 -> {
					String packageId = tuple2.getT2();
					return Mono.zip(Mono.just(tuple2.getT1()), uploadPackage(request, packageId));
				})

				.map(tuple2 -> tuple2.mapT2(Package::getId))
				.flatMap(tuple2 -> {
						String packageId1 = tuple2.getT2();
						return Mono.zip(Mono.just(tuple2.getT1()), waitForPackageReady(packageId1));
					}
				)
				.map(tuple2 -> tuple2.mapT2(Package::getId))
				.flatMap(tuple2 -> {
					String packageId = tuple2.getT2();
					return Mono.zip(Mono.just(tuple2.getT1()), createBuildForPackage(packageId));
				})
				.flatMap(tuple2 -> {
						String buildId = tuple2.getT2();
						return Mono.zip(Mono.just(tuple2.getT1()), waitForBuildStaged(buildId));
					}
				)
				.map(tuple2 -> tuple2.mapT2((t2) -> t2.getDroplet().getId()))
				.flatMap(tuple2 -> {
					String dropletId = tuple2.getT2();
					String applicationId = tuple2.getT1();
					return createDeployment(dropletId, applicationId);
				})
				.map(CreateDeploymentResponse::getId)
				.flatMap(this::waitForDeploymentDeployed)
				.doOnRequest(l -> logger.debug("Updating application {}", name))
				.doOnSuccess(item -> logger.info("Successfully updated application {}", name))
				.doOnError(error -> logger.error("Failed to update application {}", name))
				.thenReturn(UpdateApplicationResponse.builder().name(name).build()));
	}

	private Mono<GetDeploymentResponse> waitForDeploymentDeployed(String deploymentId) {
		return this.client
			.deploymentsV3()
			.get(GetDeploymentRequest
				.builder()
				.deploymentId(deploymentId)
				.build())
			.filter(p -> p.getState().equals(DeploymentState.DEPLOYED))
			.repeatWhenEmpty(getExponentialBackOff())
			.doOnRequest(l -> logger.debug("Waiting for deployment deployed {}", deploymentId))
			.doOnSuccess(response -> logger.info("Deployment deployed {}", deploymentId))
			.doOnError(e -> logger.warn(String.format("Error waiting for deployment deployed %s: %s", deploymentId, e.getMessage())));
	}

	private Mono<CreateDeploymentResponse> createDeployment(String dropletId, String applicationId) {
		return this.client
			.deploymentsV3()
			.create(CreateDeploymentRequest
				.builder()
				.droplet(Relationship
					.builder()
					.id(dropletId).build()).relationships(DeploymentRelationships
					.builder()
					.app(ToOneRelationship
						.builder()
						.data(Relationship.builder().id(applicationId).build())
						.build()
					).build())
				.build())
			.doOnRequest(l -> logger.debug("Creating deployment for application {}", applicationId))
			.doOnSuccess(response -> logger.info("Created deployment for application {}", applicationId))
			.doOnError(e -> logger.warn(String.format("Error creating deployment for application %s: %s", applicationId, e.getMessage())));
	}

	private Mono<GetBuildResponse> waitForBuildStaged(String buildId) {
		return this.client
			.builds().get(GetBuildRequest.builder().buildId(buildId).build())
			.filter(p -> p.getState().equals(BuildState.STAGED))
			.repeatWhenEmpty(getExponentialBackOff())
			.doOnRequest(l -> logger.debug("Waiting for build staged {}", buildId))
			.doOnSuccess(response -> logger.info("Build staged {}", buildId))
			.doOnError(e -> logger.warn(String.format("Error waiting for build staged %s: %s", buildId, e.getMessage())));
	}

	private Mono<String> createBuildForPackage(String packageId) {
		return this.client
			.builds()
			.create(CreateBuildRequest
				.builder()
				.getPackage(Relationship.builder().id(packageId).build())
				.build())
			.map(CreateBuildResponse::getId)
			.doOnRequest(l -> logger.debug("Creating build for package {}", packageId))
			.doOnSuccess(response -> logger.info("Created build for package {}", packageId))
			.doOnError(e -> logger.warn(String.format("Error creating build package %s: %s", packageId, e.getMessage())));
	}

	private Mono<GetPackageResponse> waitForPackageReady(String packageId) {
		return this.client
			.packages()
			.get(GetPackageRequest.builder().packageId(packageId).build())
			.filter(p -> p.getState().equals(PackageState.READY))
			.repeatWhenEmpty(getExponentialBackOff())
			.doOnRequest(l -> logger.debug("Waiting for package ready {}", packageId))
			.doOnSuccess(response -> logger.info("Package ready {}", packageId))
			.doOnError(e -> logger.warn(String.format("Error waiting for package ready %s: %s", packageId, e.getMessage())));
	}

	private Mono<UploadPackageResponse> uploadPackage(UpdateApplicationRequest request, String packageId) {
		try {
			return this.client
				.packages()
				.upload(UploadPackageRequest
					.builder()
					.packageId(packageId)
					.bits(Paths.get(getAppResource(request.getPath()).getURI()))
					.build())
				.doOnRequest(l -> logger.debug("Uploading package {}", packageId))
				.doOnSuccess(response -> logger.info("Package uploaded {}", packageId))
				.doOnError(e -> logger.warn(String.format("Error uploading package %s: %s", packageId, e.getMessage())));
		}
		catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	private Mono<CreatePackageResponse> createPackageForApplication(String applicationId) {
		return this.client
			.packages()
			.create(CreatePackageRequest
				.builder()
				.relationships(PackageRelationships
					.builder()
					.application(ToOneRelationship
						.builder()
						.data(Relationship
							.builder()
							.id(applicationId)
							.build())
						.build())
					.build())
				.type(PackageType.BITS)
				.build())
			.doOnRequest(l -> logger.debug("Creating package for application {}", applicationId))
			.doOnSuccess(response -> logger.info("Created package for application {}", applicationId))
			.doOnError(e -> logger.warn(String.format("Error creating package for application %s: %s", applicationId, e.getMessage())));
	}

	private Mono<org.cloudfoundry.client.v2.applications.UpdateApplicationResponse> updateApplicationEnvironment(
		String applicationId, Map<String, Object> environmentVariables) {
		return this.client
			.applicationsV2()
			.update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest
				.builder()
				.applicationId(applicationId)
				.putAllEnvironmentJsons(environmentVariables)
				.build())
			.doOnRequest(l -> logger.debug("Updating environment for application {}", applicationId))
			.doOnSuccess(response -> logger.info("Updated environment for application {}", applicationId))
			.doOnError(e -> logger.warn(String.format("Error Updating environment for application %s: %s", applicationId, e.getMessage())));
	}

	private Function<Flux<Long>, Publisher<?>> getExponentialBackOff() {
		return DelayUtils.exponentialBackOff(Duration.ofSeconds(2), Duration.ofMinutes(5), Duration.ofMinutes(10));
	}

	private Mono<Void> pushApplication(DeployApplicationRequest request,
									   Map<String, String> deploymentProperties,
									   Resource appResource) {
		ApplicationManifest manifest = buildAppManifest(request, deploymentProperties, appResource);

		logger.debug("Pushing manifest" + manifest.toString());

		PushApplicationManifestRequest applicationManifestRequest =
			PushApplicationManifestRequest.builder()
										  .manifest(manifest)
										  .stagingTimeout(this.defaultDeploymentProperties.getStagingTimeout())
										  .startupTimeout(this.defaultDeploymentProperties.getStartupTimeout())
										  .build();

		Mono<Void> requestPushApplication;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_PROPERTY_KEY);
			requestPushApplication = pushManifestInSpace(applicationManifestRequest, space);
		} else {
			requestPushApplication = pushManifest(applicationManifestRequest);
		}

		return requestPushApplication
			.doOnSuccess(v -> logger.info("Done uploading bits for {}", request.getName()))
			.doOnError(e -> logger.error(String.format("Error creating app %s.  Exception Message %s", request.getName(), e.getMessage())));
	}

	private ApplicationManifest buildAppManifest(DeployApplicationRequest request,
												 Map<String, String> deploymentProperties,
												 Resource appResource) {
		ApplicationManifest.Builder manifest = ApplicationManifest.builder()
			.name(request.getName())
			.path(getApplication(appResource))
			.environmentVariables(getEnvironmentVariables(deploymentProperties,
				request.getEnvironment(), request.getServiceInstanceId()))
			.services(request.getServices())
			.instances(instances(deploymentProperties))
			.memory(memory(deploymentProperties))
			.disk(diskQuota(deploymentProperties))
			.healthCheckType(healthCheck(deploymentProperties))
			.healthCheckHttpEndpoint(healthCheckEndpoint(deploymentProperties))
			.timeout(healthCheckTimeout(deploymentProperties))
			.noRoute(toggleNoRoute(deploymentProperties));

		Optional.ofNullable(host(deploymentProperties)).ifPresent(manifest::host);
		Optional.ofNullable(domain(deploymentProperties)).ifPresent(manifest::domain);
		Optional.ofNullable(routePath(deploymentProperties)).ifPresent(manifest::routePath);

		if (route(deploymentProperties) != null) {
			manifest.route(Route.builder().route(route(deploymentProperties)).build());
		}

		if (!routes(deploymentProperties).isEmpty()) {
			Set<Route> routes = routes(deploymentProperties).stream()
					.map(r -> Route.builder().route(r).build())
					.collect(Collectors.toSet());
			manifest.routes(routes);
		}

		if (getDockerImage(appResource) == null) {
			manifest.buildpack(buildpack(deploymentProperties));
		} else {
			manifest.docker(Docker.builder().image(getDockerImage(appResource)).build());
		}
		return manifest.build();
	}

	private Mono<Void> pushManifest(PushApplicationManifestRequest request) {
		return this.operations.applications()
			.pushManifest(request);
	}

	private Mono<Void> pushManifestInSpace(PushApplicationManifestRequest request, String spaceName) {
		return createSpace(spaceName)
			.then(operationsUtils.getOperationsForSpace(spaceName))
			.flatMap(cfOperations -> cfOperations.applications().pushManifest(request));
	}

	private Mono<Void> createSpace(String spaceName) {
		Mono<String> createSpacePublisher = getDefaultOrganizationId()
			.flatMap(orgId -> this.client.spaces()
										 .create(CreateSpaceRequest.builder()
																   .organizationId(orgId)
																   .name(spaceName)
																   .build())
										 .doOnSuccess(response -> logger.info("Created space {}", spaceName))
										 .doOnError(e -> logger.warn(String.format("Error creating space %s: %s", spaceName, e.getMessage())))
										 .onErrorResume(e -> Mono.empty())
										 .then(Mono.empty()));
		return getSpaceIdFromName(spaceName)
			.switchIfEmpty(createSpacePublisher).then();
	}

	private Mono<String> getDefaultOrganizationId() {
		return this.operations.organizations()
			.get(OrganizationInfoRequest.builder()
				.name(targetProperties.getDefaultOrg())
				.build())
			.map(OrganizationDetail::getId);
	}

	@Override
	public Mono<UndeployApplicationResponse> undeploy(UndeployApplicationRequest request) {
		logger.trace("Undeploying application: request={}", request);

		String appName = request.getName();
		Map<String, String> deploymentProperties = request.getProperties();

		Mono<Void> requestDeleteApplication;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_PROPERTY_KEY);
			requestDeleteApplication = deleteApplicationInSpace(appName, space)
				.then(deleteSpace(space));
		} else {
			requestDeleteApplication = deleteApplication(appName);
		}

		return requestDeleteApplication
			.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
			.doOnSuccess(v -> logger.info("Successfully undeployed app {}", appName))
			.doOnError(logError(String.format("Failed to undeploy app %s", appName)))
			.then(Mono.just(UndeployApplicationResponse.builder()
				.name(appName)
				.build()));
	}

	private Mono<Void> deleteApplication(String name) {
		return this.operations.applications()
			.delete(DeleteApplicationRequest.builder()
				.deleteRoutes(this.defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build());
	}

	private Mono<Void> deleteApplicationInSpace(String name, String spaceName) {
		return getSpaceIdFromName(spaceName)
			.doOnError(error -> logger.warn("Unable to get space name: {} ", spaceName))
			.then(operationsUtils.getOperationsForSpace(spaceName))
			.flatMap(cfOperations -> cfOperations.applications().delete(DeleteApplicationRequest.builder()
				.deleteRoutes(this.defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build())
				.doOnError(error -> logger.warn("Unable to delete application: {} ", name)))
			.onErrorResume(e -> Mono.empty());
	}

	private Mono<Void> deleteSpace(String spaceName) {
		return getSpaceIdFromName(spaceName)
			.doOnError(error -> logger.warn("Unable to get space name: {} ", spaceName))
			.flatMap(spaceId -> this.client.spaces()
				.delete(DeleteSpaceRequest.builder()
					.spaceId(spaceId)
					.build())
				.then(Mono.empty()));
	}

	private Mono<String> getSpaceIdFromName(String spaceName) {
		return getDefaultOrganizationId()
			.flatMap(orgId -> PaginationUtils.requestClientV2Resources(page -> client.organizations()
				.listSpaces(ListOrganizationSpacesRequest.builder()
					.name(spaceName)
					.organizationId(orgId)
					.page(page)
					.build()))
			.filter(resource -> resource.getEntity().getName().equals(spaceName))
			.map(resource -> resource.getMetadata().getId())
			.next());
	}

	private Map<String, Object> getEnvironmentVariables(Map<String, String> properties,
														Map<String, Object> environment,
														String serviceInstanceId) {
		Map<String, Object> envVariables = getApplicationEnvironment(properties, environment, serviceInstanceId);

		String javaOpts = javaOpts(properties);
		if (StringUtils.hasText(javaOpts)) {
			envVariables.put("JAVA_OPTS", javaOpts);
		}

		return envVariables;
	}

	private Map<String, Object> getApplicationEnvironment(Map<String, String> properties,
														  Map<String, Object> environment,
														  String serviceInstanceId) {
		Map<String, Object> applicationEnvironment = sanitizeApplicationEnvironment(environment);

		if (serviceInstanceId != null) {
			applicationEnvironment.put("spring.cloud.appbroker.service-instance-id", serviceInstanceId);
		}

		if (!applicationEnvironment.isEmpty() && useSpringApplicationJson(properties)) {
			try {
				String jsonEnvironment = OBJECT_MAPPER.writeValueAsString(applicationEnvironment);
				applicationEnvironment = new HashMap<>(1);
				applicationEnvironment.put("SPRING_APPLICATION_JSON", jsonEnvironment);
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Error writing environment to SPRING_APPLICATION_JSON", e);
			}
		}

		return applicationEnvironment;
	}

	private Map<String, Object> sanitizeApplicationEnvironment(Map<String, Object> environment) {
		Map<String, Object> applicationEnvironment = new HashMap<>(environment);

		// Remove server.port as CF assigns a port for us, and we don't want to override that
		Optional.ofNullable(applicationEnvironment.remove("server.port"))
			.ifPresent(port -> logger.warn("Ignoring 'server.port={}', " +
				"as Cloud Foundry will assign a local dynamic port. " +
				"Route to the app will use port 80.", port));

		return applicationEnvironment;
	}

	private boolean useSpringApplicationJson(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.USE_SPRING_APPLICATION_JSON_KEY))
			.map(Boolean::valueOf)
			.orElse(this.defaultDeploymentProperties.isUseSpringApplicationJson());
	}

	private String domain(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY))
			.orElse(this.defaultDeploymentProperties.getDomain());
	}

	private ApplicationHealthCheck healthCheck(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY))
			.map(this::toApplicationHealthCheck)
			.orElse(this.defaultDeploymentProperties.getHealthCheck());
	}

	private ApplicationHealthCheck toApplicationHealthCheck(String raw) {
		try {
			return ApplicationHealthCheck.from(raw);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Unsupported health-check value '%s'. Available values are %s", raw,
				StringUtils.arrayToCommaDelimitedString(ApplicationHealthCheck.values())), e);
		}
	}

	private String healthCheckEndpoint(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getHealthCheckHttpEndpoint());
	}

	private Integer healthCheckTimeout(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_TIMEOUT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getHealthCheckTimeout());
	}

	private Integer instances(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.COUNT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getCount());
	}

	private String host(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.HOST_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getHost());
	}

	private String routePath(Map<String, String> properties) {
		String routePath = properties.get(CloudFoundryDeploymentProperties.ROUTE_PATH_PROPERTY);
		if (StringUtils.hasText(routePath) && routePath.charAt(0) != '/') {
			throw new IllegalArgumentException(
					"Cloud Foundry routes must start with \"/\". Route passed = [" + routePath + "].");
		}
		return routePath;
	}

	private String route(Map<String, String> properties) {
		return properties.get(CloudFoundryDeploymentProperties.ROUTE_PROPERTY);
	}

	private Set<String> routes(Map<String, String> properties) {
		Set<String> routes = new HashSet<>();
		routes.addAll(this.defaultDeploymentProperties.getRoutes());
		routes.addAll(StringUtils.commaDelimitedListToSet(properties.get(CloudFoundryDeploymentProperties.ROUTES_PROPERTY)));
		return routes;
	}

	private Boolean toggleNoRoute(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY))
				.map(Boolean::valueOf)
				.orElse(null);
	}

	private Integer memory(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.MEMORY_PROPERTY_KEY))
			.map(ByteSizeUtils::parseToMebibytes)
			.orElse(ByteSizeUtils.parseToMebibytes(defaultDeploymentProperties.getMemory()));
	}

	private Integer diskQuota(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.DISK_PROPERTY_KEY))
			.map(ByteSizeUtils::parseToMebibytes)
			.orElse(ByteSizeUtils.parseToMebibytes(defaultDeploymentProperties.getDisk()));
	}

	private String buildpack(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getBuildpack());
	}

	private String javaOpts(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getJavaOpts());
	}

	private Predicate<Throwable> isNotFoundError() {
		return t -> t instanceof AbstractCloudFoundryException &&
			((AbstractCloudFoundryException) t).getStatusCode() == HttpStatus.NOT_FOUND.value();
	}

	/**
	 * Return a Docker image identifier if the application Resource is for a Docker image, or {@literal null} otherwise.
	 *
	 * @see #getApplication(Resource)
	 */
	private String getDockerImage(Resource resource) {
		try {
			String uri = resource.getURI().toString();
			if (uri.startsWith("docker:")) {
				return uri.substring("docker:".length());
			} else {
				return null;
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	private Resource getAppResource(String path) {
		return resourceLoader.getResource(path);
	}

	/**
	 * Return a Path to the application Resource or {@literal null} if the request is for a Docker image.
	 *
	 * @see #getDockerImage(Resource)
	 * @param resource the resource representing the app bits
	 */
	private Path getApplication(Resource resource) {
		try {
			if (resource.getURI().toString().startsWith("docker:")) {
				return null;
			} else {
				return resource.getFile().toPath();
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return Mono.defer(() -> {
			if (StringUtils.hasText(request.getServiceInstanceId())) {
				return getServiceInstance(request.getServiceInstanceId())
					.flatMap(serviceInstanceEntity -> getSpace(serviceInstanceEntity.getSpaceId())
						.flatMap(spaceEntity -> getServiceInstance(serviceInstanceEntity.getName(), spaceEntity)));
			}
			else {
				return operationsUtils.getOperations(request.getProperties())
					.flatMap(cfOperations -> cfOperations.services()
						.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
							.name(request.getName())
							.build()));
			}
		})
			.map(serviceInstance -> GetServiceInstanceResponse.builder()
				.name(serviceInstance.getName())
				.service(serviceInstance.getService())
				.plan(serviceInstance.getPlan())
				.build());
	}

	private Mono<ServiceInstance> getServiceInstance(String name, SpaceEntity spaceEntity) {
		return getOrganization(spaceEntity.getOrganizationId())
			.flatMap(organizationEntity ->
							operationsUtils.getOperationsForOrgAndSpace(organizationEntity.getName(), spaceEntity.getName())
						   		.flatMap(cfOperations -> cfOperations.services()
										.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
	  								    .name(name)
									 	.build())));
	}

	private Mono<ServiceInstanceEntity> getServiceInstance(String serviceInstanceId) {
		return client.serviceInstances().get(org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest.builder()
			.serviceInstanceId(serviceInstanceId)
			.build())
			.map(ResourceUtils::getEntity);
	}

	private Mono<SpaceEntity> getSpace(String spaceId) {
		return client.spaces().get(org.cloudfoundry.client.v2.spaces.GetSpaceRequest.builder()
			.spaceId(spaceId)
			.build())
			.map(ResourceUtils::getEntity);
	}

	private Mono<OrganizationEntity> getOrganization(String organizationId) {
		return client.organizations()
					 .get(GetOrganizationRequest.builder().organizationId(organizationId).build())
					 .map(GetOrganizationResponse::getEntity);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		org.cloudfoundry.operations.services.CreateServiceInstanceRequest createServiceInstanceRequest =
			org.cloudfoundry.operations.services.CreateServiceInstanceRequest
				.builder()
				.serviceInstanceName(request.getServiceInstanceName())
				.serviceName(request.getName())
				.planName(request.getPlan())
				.parameters(request.getParameters())
				.build();

		Mono<CreateServiceInstanceResponse> createServiceInstanceResponseMono =
			Mono.just(CreateServiceInstanceResponse.builder()
				.name(request.getServiceInstanceName())
				.build());

		if (request.getProperties().containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			return createSpace(request.getProperties().get(DeploymentProperties.TARGET_PROPERTY_KEY))
				.then(
					operationsUtils.getOperations(request.getProperties())
						.flatMap(cfOperations -> cfOperations.services()
							.createInstance(createServiceInstanceRequest)
							.then(createServiceInstanceResponseMono)));
		}
		else {
			return operations
				.services()
				.createInstance(createServiceInstanceRequest)
				.then(createServiceInstanceResponseMono);
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return operationsUtils.getOperations(request.getProperties())
			.flatMap(cfOperations -> rebindServiceInstanceIfNecessary(request, cfOperations)
				.then(updateServiceInstanceIfNecessary(request, cfOperations)));
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return operationsUtils.getOperations(request.getProperties())
			.flatMap(cfOperations -> Mono.just(request.getServiceInstanceName())
				.flatMap(serviceInstanceName -> unbindServiceInstance(serviceInstanceName, cfOperations)
					.then(deleteServiceInstance(serviceInstanceName, cfOperations)
						.thenReturn(DeleteServiceInstanceResponse.builder()
							.name(serviceInstanceName)
							.build()))));
	}

	private Mono<Void> deleteServiceInstance(String serviceInstanceName, CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services().deleteInstance(
			org.cloudfoundry.operations.services.DeleteServiceInstanceRequest
				.builder()
				.name(serviceInstanceName)
				.build());
	}

	private Mono<Void> unbindServiceInstance(String serviceInstanceName,
											 CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services().getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
			.name(serviceInstanceName)
			.build())
			.map(ServiceInstance::getApplications)
			.flatMap(applications -> Flux.fromIterable(applications)
				.flatMap(application -> cloudFoundryOperations.services().unbind(
					UnbindServiceInstanceRequest.builder()
						.applicationName(application)
						.serviceInstanceName(serviceInstanceName)
						.build())
				)
			.then(Mono.empty()));
	}

	private Mono<Void> rebindServiceInstance(String serviceInstanceName,
											 CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services().getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
			.name(serviceInstanceName)
			.build())
			.map(ServiceInstance::getApplications)
			.flatMap(applications -> Flux.fromIterable(applications)
				.flatMap(application -> cloudFoundryOperations.services().unbind(
					UnbindServiceInstanceRequest.builder()
						.applicationName(application)
						.serviceInstanceName(serviceInstanceName)
						.build())
					.then(cloudFoundryOperations.services().bind(
						BindServiceInstanceRequest.builder()
							.applicationName(application)
							.serviceInstanceName(serviceInstanceName)
							.build()))
				)
			.then(Mono.empty()));
	}

	private Mono<Void> rebindServiceInstanceIfNecessary(UpdateServiceInstanceRequest request,
														CloudFoundryOperations cloudFoundryOperations) {
		if (request.isRebindOnUpdate()) {
			return rebindServiceInstance(request.getServiceInstanceName(), cloudFoundryOperations);
		}
		return Mono.empty();
	}

	private Mono<UpdateServiceInstanceResponse> updateServiceInstanceIfNecessary(UpdateServiceInstanceRequest request,
																				 CloudFoundryOperations cloudFoundryOperations) {
		// service instances can be updated with a change to the plan, name, or parameters;
		// of these only parameter changes are supported, so don't update if the
		// backing service instance has no parameters
		if (request.getParameters() == null || request.getParameters().isEmpty()) {
			return Mono.empty();
		}

		final String serviceInstanceName = request.getServiceInstanceName();

		return cloudFoundryOperations.services().updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.parameters(request.getParameters())
				.build())
			.then(Mono.just(UpdateServiceInstanceResponse.builder()
				.name(serviceInstanceName)
				.build()));
	}

	/**
	 * Return a function usable in {@literal doOnError} constructs that will unwrap unrecognized Cloud Foundry Exceptions
	 * and log the text payload.
	 */
	private Consumer<Throwable> logError(String msg) {
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
