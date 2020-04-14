package com.orange.oss.osbcmdb;

import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.LastOperation;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.useradmin.SetSpaceRoleRequest;
import org.cloudfoundry.operations.useradmin.SpaceRole;
import org.cloudfoundry.util.PaginationUtils;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceInstance implements ServiceInstanceService {

	public static final CreateServiceInstanceResponse RESPONSE_CREATE_202_ACCEPTED = CreateServiceInstanceResponse
		.builder()
		.async(true)
		.build();

	public static final DeleteServiceInstanceResponse RESPONSE_DELETE_202_ACCEPTED = DeleteServiceInstanceResponse
		.builder()
		.async(true)
		.build();

	public static final UpdateServiceInstanceResponse RESPONSE_UPDATE_202_ACCEPTED =
		UpdateServiceInstanceResponse.builder()
			.async(true)
			.build();

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final Logger LOG = Loggers.getLogger(OsbCmdbServiceInstance.class);

	private final CloudFoundryClient client;

	private final String defaultOrg;

	private final String defaultSpace;

	private final String userName;

	private final CloudFoundryDeploymentProperties deploymentProperties;

	private final CloudFoundryOperations operations;


	public OsbCmdbServiceInstance(CloudFoundryDeploymentProperties deploymentProperties,
		CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient,
		String defaultOrg, String defaultSpace, String userName) {

		this.deploymentProperties = deploymentProperties;
		operations = cloudFoundryOperations;
		client = cloudFoundryClient;
		this.defaultOrg = defaultOrg;
		this.defaultSpace = defaultSpace;
		this.userName = userName;
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		CloudFoundryOperations spacedTargetedOperations = getOrCreateSpace(request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		boolean asyncProvisionning;
		try {
			spacedTargetedOperations.services()
				.createInstance(org.cloudfoundry.operations.services.CreateServiceInstanceRequest.builder()
					.serviceName(request.getServiceDefinition().getName())
					.serviceInstanceName(request.getServiceInstanceId())
					.planName(request.getPlan().getName())
					.parameters(request.getParameters())
					.completionTimeout(SYNC_COMPLETION_TIMEOUT)
					.build())
				.block();
			asyncProvisionning = false;
		}
		catch (Exception timeoutException) {
			// timeout to 5s: would return an error if service instance is "in_progress" state
			asyncProvisionning = true;
		} //Already existing exception should flow up and be returned to osb client

		ServiceInstance provisionnedSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());
		updateServiceInstanceMetadata(spacedTargetedOperations, provisionnedSi, request);

		CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse
			.builder()
			.dashboardUrl(provisionnedSi.getDashboardUrl())
			.async(asyncProvisionning)
			.operation(toJson(new CmdbOperationState(existingSi.getId(), OsbOperation.CREATE)));
				//make get last operation faster by tracking the underlying CF instance
				// GUID
		return Mono.just(responseBuilder.build());
	}

	private String toJson(CmdbOperationState cmdbOperationState)  {
		try {
			return OBJECT_MAPPER.writeValueAsString(cmdbOperationState);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		CloudFoundryOperations spacedTargetedOperations = getOrCreateSpace(request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		//List and delete service keys first
		//Delete service keys if any. Ignore race
		String serviceInstanceName = request.getServiceInstanceId();
		spacedTargetedOperations.services().listServiceKeys(ListServiceKeysRequest.builder()
			.serviceInstanceName(serviceInstanceName).build())
			.flatMap(sk -> spacedTargetedOperations.services().deleteServiceKey(DeleteServiceKeyRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.serviceKeyName(sk.getName())
				.build()))
			.blockLast();


		//set completion
		// timeout to 5s: would return an error if service instance is "in_progress" state
		//expects noop on missing service instance
		spacedTargetedOperations.services()
			.deleteInstance(org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.completionTimeout(SYNC_COMPLETION_TIMEOUT)
				.build());
		boolean asyncProvisionning = false;

		DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder builder = DeleteServiceInstanceResponse
			.builder()
			.async(asyncProvisionning);
		if (asyncProvisionning) {
			builder.operation(toJson(new CmdbOperationState(existingSi.getId(), OsbOperation.DELETE)));
		}
		return Mono.just(builder
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		CmdbOperationState cmdbOperationState = fromJson(request.getOperation());
		String cfServiceGuid = cmdbOperationState.backingCfServiceInstanceGuid;

		GetServiceInstanceResponse serviceInstanceResponse;
		Exception getServiceInstanceException = null;
		try {
			serviceInstanceResponse = client.serviceInstances()
				.get(GetServiceInstanceRequest.builder().serviceInstanceId(cfServiceGuid).build())
				.block();
		}
		catch (Exception e) {
			LOG.info("No such instance with guid " + cfServiceGuid);
			getServiceInstanceException = e;
			serviceInstanceResponse = null;
		}

		OperationState operationState;
		if (serviceInstanceResponse == null) {
			operationState = OperationState.FAILED;
			switch (cmdbOperationState.operationType) {
				case UPDATE: // fall through
				case CREATE:
					LOG.error("Unable to provide last operation for {} operation of guid={} Missing service instance " +
							"with exception:{} ",
						cmdbOperationState.operationType, cfServiceGuid, getServiceInstanceException.toString());
					operationState = OperationState.FAILED;
					break;
				case DELETE:
					operationState = OperationState.SUCCEEDED;
					break;
			}
		}
		else {
			// in progress -> in progress
			// errored -> errored
			LastOperation lastOperation = serviceInstanceResponse.getEntity().getLastOperation();
			operationState = convertCfStateToOsbState(lastOperation.getState());
		}

		return Mono.just(GetLastServiceOperationResponse.builder()
			.operationState(operationState)
			.build());
	}


	private OperationState convertCfStateToOsbState(String cfServiceInstanceState) {
		//Source of truth: https://apidocs.cloudfoundry.org/12.42.0/service_instances/creating_a_service_instance.html
		//TODO: move the values in the enum and remove the switch statement
		switch (cfServiceInstanceState) {
			case "succeeded":
				return OperationState.SUCCEEDED;
			case "failed":
				return OperationState.FAILED;
			case "in progress":
				return OperationState.IN_PROGRESS;
			default:
				LOG.error("Unknown CF service instance state {}", cfServiceInstanceState);
				throw new RuntimeException("Unknown CF service instance state " + cfServiceInstanceState);
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		CloudFoundryOperations spacedTargetedOperations = getOrCreateSpace(
			request.getServiceDefinition().getName()); //ignore race condition during space
		// creation for K8S dupl requests
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		//set completion
		// timeout to 5s: would return an error if service instance is "in_progress" state
		boolean asyncProvisionning;
		try {
			spacedTargetedOperations.services()
				.updateInstance(org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
					.serviceInstanceName(existingSi.getName())
					.planName(request.getPlan().getName())
					.parameters(request.getParameters())
					.completionTimeout(SYNC_COMPLETION_TIMEOUT)
					.build())
				.block();
			asyncProvisionning = false;
		}
		catch (Exception timeoutException) {
			// timeout to 5s: would return an error if service instance is "in_progress" state
			asyncProvisionning = true;
		} //Other exceptions should flow up and be returned to osb client
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(asyncProvisionning)
			.operation(toJson(new CmdbOperationState(existingSi.getId(), OsbOperation.UPDATE)))
			.build());
	}

	private Mono<Void> addSpaceDeveloperRoleForCurrentUser(String orgName, String spaceName) {
		return Mono.defer(() -> {
			return operations.userAdmin().setSpaceRole(SetSpaceRoleRequest.builder()
				.spaceRole(SpaceRole.DEVELOPER)
				.organizationName(orgName)
				.spaceName(spaceName)
				.username(this.userName)
				.build())
				.doOnSuccess(v -> LOG.info("Set space developer role for space {}", spaceName))
				.doOnError(e -> LOG.warn(String
					.format("Error setting space developer role for space %s: %s", spaceName, e.getMessage())));
		});
	}

	private Mono<String> createSpace(String spaceName) {
		return getSpaceId(spaceName)
			.switchIfEmpty(Mono.just(this.defaultOrg)
				.flatMap(orgName -> getOrganizationId(orgName)
					.flatMap(orgId -> client.spaces().create(CreateSpaceRequest.builder()
						.organizationId(orgId)
						.name(spaceName)
						.build())
						.doOnSuccess(response -> LOG.info("Created space {}", spaceName))
						.doOnError(
							e -> LOG.warn(String.format("Error creating space %s: %s", spaceName, e.getMessage())))
						.map(response -> response.getMetadata().getId())
						.flatMap(spaceId -> addSpaceDeveloperRoleForCurrentUser(orgName, spaceName)
							.thenReturn(spaceId)))));
	}

	private CmdbOperationState fromJson(String operation) {
		try {
			return OBJECT_MAPPER.readValue(operation, CmdbOperationState.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private ServiceInstance getCfServiceInstance(CloudFoundryOperations spacedTargetedOperations,
		String serviceInstanceName) {
		return spacedTargetedOperations.services()
			.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
				.name(serviceInstanceName).build()).block();
	}

	private CloudFoundryOperations getOrCreateSpace(String spaceName) {
		createSpace(spaceName).block();

		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.operations)
			.space(spaceName)
			.build();
	}

	private Mono<String> getOrganizationId(String orgName) {
		return operations.organizations().get(OrganizationInfoRequest.builder()
			.name(orgName)
			.build())
			.map(OrganizationDetail::getId);
	}

	private Mono<String> getSpaceId(String spaceName) {
		return Mono.justOrEmpty(this.defaultOrg)
			.flatMap(orgName -> getOrganizationId(orgName)
				.flatMap(orgId -> PaginationUtils.requestClientV2Resources(page -> client.organizations()
					.listSpaces(ListOrganizationSpacesRequest.builder()
						.name(spaceName)
						.organizationId(orgId)
						.page(page)
						.build()))
					.filter(resource -> resource.getEntity().getName().equals(spaceName))
					.map(resource -> resource.getMetadata().getId())
					.next()));
	}

	private void updateServiceInstanceMetadata(CloudFoundryOperations spacedTargetedOperations,
		ServiceInstance provisionnedSi, CreateServiceInstanceRequest request) {
		//TODO: reuse CAFD + metadata formatter code in workflow
	}

	private enum OsbOperation {
		CREATE,
		UPDATE,
		DELETE
	}

	private static class CmdbOperationState {

		String backingCfServiceInstanceGuid;

		OsbOperation operationType;

		public CmdbOperationState(String backingCfServiceInstanceGuid,
			OsbOperation operationType) {
			this.backingCfServiceInstanceGuid = backingCfServiceInstanceGuid;
			this.operationType = operationType;
		}

	}

}
