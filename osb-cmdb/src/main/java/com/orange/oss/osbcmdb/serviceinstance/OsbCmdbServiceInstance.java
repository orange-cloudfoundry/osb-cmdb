package com.orange.oss.osbcmdb.serviceinstance;

import java.time.Duration;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.osbcmdb.AbstractOsbCmdbService;
import com.orange.oss.osbcmdb.metadata.CreateServiceMetadataFormatterServiceImpl;
import com.orange.oss.osbcmdb.metadata.MetaData;
import com.orange.oss.osbcmdb.metadata.UpdateServiceMetadataFormatterService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.LastOperation;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.util.ExceptionUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceInstance extends AbstractOsbCmdbService implements ServiceInstanceService {

	private final CreateServiceMetadataFormatterServiceImpl createServiceMetadataFormatterService;

	private final UpdateServiceMetadataFormatterService updateServiceMetadataFormatterService;

	private ServiceInstanceInterceptor osbInterceptor;

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

	private final String defaultSpace;

	protected final Logger LOG = Loggers.getLogger(OsbCmdbServiceInstance.class);

	public OsbCmdbServiceInstance(CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient,
		String defaultOrg, String defaultSpace, String userName,
		ServiceInstanceInterceptor osbInterceptor,
		CreateServiceMetadataFormatterServiceImpl createServiceMetadataFormatterService,
		UpdateServiceMetadataFormatterService updateServiceMetadataFormatterService) {
		super(cloudFoundryClient, defaultOrg, userName, cloudFoundryOperations);

		this.defaultSpace = defaultSpace;
		this.osbInterceptor = osbInterceptor;
		this.createServiceMetadataFormatterService = createServiceMetadataFormatterService;
		this.updateServiceMetadataFormatterService = updateServiceMetadataFormatterService;
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.createServiceInstance(request);
		}

		String backingServiceName = request.getServiceDefinition().getName();
		String backingServicePlanName = request.getPlan().getName();

		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);
		String backingServicePlanId = fetchBackingServicePlanId(backingServiceName, backingServicePlanName, spaceId);


		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();
		String backingServiceInstanceInstanceId = null;
		try {
			org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceResponse createServiceInstanceResponse;
			createServiceInstanceResponse = client.serviceInstances()
				.create(org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest.builder()
					.name(request.getServiceInstanceId())
					.servicePlanId(backingServicePlanId)
					.parameters(request.getParameters())
					.spaceId(spaceId)
					.build())
				.block();

			backingServiceInstanceInstanceId = createServiceInstanceResponse.getMetadata().getId();
			LastOperation lastOperation = createServiceInstanceResponse.getEntity().getLastOperation();
			if (lastOperation == null) {
				LOG.error("Unexpected missing last operation from CSI. Full response was {}",
					createServiceInstanceResponse);
				throw new ServiceBrokerException("Internal CF protocol error");
			}
			boolean asyncProvisioning;
			switch (lastOperation.getState()) {
				case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
					asyncProvisioning= true;
					//make get last operation faster by tracking the underlying CF instance
					// GUID
					responseBuilder.operation(toJson(new CmdbOperationState(createServiceInstanceResponse.getMetadata().getId(),
						OsbOperation.CREATE)));
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
					asyncProvisioning = false;
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
					LOG.info("Backing service failed to provision with {}, flowing up the error to the osb client",
						lastOperation);
					throw new ServiceBrokerException(lastOperation.getDescription());
				default:
					LOG.error("Unexpected last operation state:" + lastOperation.getState());
					throw new ServiceBrokerException("Internal CF protocol error");
			}
			responseBuilder.async(asyncProvisioning);
		}
		finally {
			if (backingServiceInstanceInstanceId != null) {
				updateServiceInstanceMetadata(request, backingServiceInstanceInstanceId);
			}
		}

		return Mono.just(responseBuilder.build());
	}

	private String getSpacedIdFromTargettedOperationsInternals(CloudFoundryOperations spacedTargetedOperations) {
		DefaultCloudFoundryOperations spacedTargetedOperationsInternals = (DefaultCloudFoundryOperations) spacedTargetedOperations;
		String spaceId = spacedTargetedOperationsInternals.getSpaceId().block();
		if(spaceId == null) {
			LOG.error("Unexpected null spaceId in DefaultCloudFoundryOperations {}", spacedTargetedOperationsInternals);
			throw new ServiceBrokerException("Internal CF client error");
		}
		return spaceId;
	}

	private String fetchBackingServicePlanId(String backingServiceName, String backingServicePlanName, String spaceId) {
		//Inspired from first instrcutions of cf-java-client:
		// org.cloudfoundry.operations.services.DefaultServices.createInstance()
		String backingServiceId = PaginationUtils
			.requestClientV2Resources(page -> client.spaces()
				.listServices(ListSpaceServicesRequest.builder()
					.label(backingServiceName)
					.page(page)
					.spaceId(spaceId)
					.build()))
			.single()
			.onErrorResume(
				NoSuchElementException.class, t -> ExceptionUtils.illegalArgument("Service %s does not exist", backingServiceName))
			.map(ResourceUtils::getId)
			.block();

		return PaginationUtils
			.requestClientV2Resources(page -> client.servicePlans()
				.list(ListServicePlansRequest.builder()
					.page(page)
					.serviceId(backingServiceId)
					.build()))
			.filter(resource -> {
				return backingServicePlanName.equals(ResourceUtils.getEntity(resource).getName());
			})
			.single()
			.map(ResourceUtils::getId)
			.onErrorResume(NoSuchElementException.class,
				t -> ExceptionUtils.illegalArgument("Service plan %s does not exist", backingServicePlanName))
			.block();
	}

	protected String toJson(CmdbOperationState cmdbOperationState)  {
		try {
			return OBJECT_MAPPER.writeValueAsString(cmdbOperationState);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.deleteServiceInstance(request);
		}
		String backingServiceInstanceName = request.getServiceInstanceId();

		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, backingServiceInstanceName);

		if (existingSi == null) {
			LOG.info("No such backing service instance id={} to delete, return early.", backingServiceInstanceName);
			return Mono.just(DeleteServiceInstanceResponse.builder().build());
		}

		//List and delete service keys first
		//Delete service keys if any. Ignore race
		spacedTargetedOperations.services().listServiceKeys(ListServiceKeysRequest.builder()
			.serviceInstanceName(backingServiceInstanceName).build())
			.flatMap(sk -> spacedTargetedOperations.services().deleteServiceKey(DeleteServiceKeyRequest.builder()
				.serviceInstanceName(backingServiceInstanceName)
				.serviceKeyName(sk.getName())
				.build()))
			.blockLast();


		//set completion
		// timeout to 5s: would return an error if service instance is "in_progress" state
		//expects noop on missing service instance
		spacedTargetedOperations.services()
			.deleteInstance(org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
				.name(backingServiceInstanceName)
				.completionTimeout(SYNC_COMPLETION_TIMEOUT)
				.build())
			.block();
		boolean asyncProvisionning = false;

		DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder builder = DeleteServiceInstanceResponse
			.builder()
			.async(asyncProvisionning);
		if (asyncProvisionning) {
			builder.operation(toJson(new CmdbOperationState(existingSi.getId(), OsbOperation.DELETE)));
		}
		return Mono.just(builder.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.getLastOperation(request);
		}

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
		//TODO: consider moving the values in the enum and remove the switch statement
		switch (cfServiceInstanceState) {
			case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
				return OperationState.SUCCEEDED;
			case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
				return OperationState.FAILED;
			case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
				return OperationState.IN_PROGRESS;
			default:
				LOG.error("Unknown CF service instance state {}", cfServiceInstanceState);
				throw new RuntimeException("Unknown CF service instance state " + cfServiceInstanceState);
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.updateServiceInstance(request);
		}

		String backingServiceName = request.getServiceDefinition().getName();
		String backingServicePlanName = request.getPlan().getName();

		//ignore race condition during space creation for K8S dupl requests
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
		ServiceInstance existingBackingServiceInstance = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);
		String backingServicePlanId = fetchBackingServicePlanId(backingServiceName, backingServicePlanName, spaceId);

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();

		try {
			org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceResponse updateServiceInstanceResponse;
			updateServiceInstanceResponse = client.serviceInstances()
				.update(org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceRequest.builder()
					.serviceInstanceId(existingBackingServiceInstance.getId())
					.servicePlanId(backingServicePlanId)
					.parameters(request.getParameters())
					.build())
				.block();

			LastOperation lastOperation = updateServiceInstanceResponse.getEntity().getLastOperation();
			if (lastOperation == null) {
				LOG.error("Unexpected missing last operation from USI. Full response was {}",
					updateServiceInstanceResponse);
				throw new ServiceBrokerException("Internal CF protocol error");
			}
			boolean asyncProvisioning;
			switch (lastOperation.getState()) {
				case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
					asyncProvisioning = true;
					//make get last operation faster by tracking the underlying CF instance
					// GUID
					responseBuilder
						.operation(toJson(new CmdbOperationState(updateServiceInstanceResponse.getMetadata().getId(),
							OsbOperation.UPDATE)));
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
					asyncProvisioning = false;
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
					LOG.info("Backing service failed to update with {}, flowing up the error to the osb " +
							"client",
						lastOperation);
					throw new ServiceBrokerException(lastOperation.getDescription());
				default:
					LOG.error("Unexpected last operation state:" + lastOperation.getState());
					throw new ServiceBrokerException("Internal CF protocol error");
			}
			responseBuilder.async(asyncProvisioning);
		}
		finally {
			//systematically try to update metadata (e.g. service instance rename) even if update failed
			updateServiceInstanceMetadata(request, existingBackingServiceInstance.getId());
		}
		return Mono.just(responseBuilder.build());
	}

	protected CmdbOperationState fromJson(String operation) {
		try {
			return OBJECT_MAPPER.readValue(operation, CmdbOperationState.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void updateServiceInstanceMetadata(CreateServiceInstanceRequest request, String serviceInstanceId) {
		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request);
		updateMetadata(metaData, serviceInstanceId);
	}

	private void updateServiceInstanceMetadata(UpdateServiceInstanceRequest request, String serviceInstanceId) {
		MetaData metaData = updateServiceMetadataFormatterService.formatAsMetadata(request);
		updateMetadata(metaData, serviceInstanceId);
	}

	private void updateMetadata(MetaData metaData, String serviceInstanceId) {
		metaData.getLabels().put("backing_service_instance_guid", serviceInstanceId); //Ideally should be
		// assigned within MetadataFormatter instead to centralize the logic. Just avoids passing the Id around as a
		// 1st step.
		LOG.debug("Assigning metadata to service instance with id={} annotations={} and labels={}",
			serviceInstanceId, metaData.getAnnotations(), metaData.getLabels());

		client.serviceInstancesV3().update(org.cloudfoundry.client.v3.serviceInstances.UpdateServiceInstanceRequest.builder()
			.serviceInstanceId(serviceInstanceId)
			.metadata(Metadata.builder()
				.annotations(metaData.getAnnotations())
				.labels(metaData.getLabels())
				.build())
			.build())
			.block();
	}

	protected enum OsbOperation {
		CREATE,
		UPDATE,
		DELETE
	}

	protected static class CmdbOperationState {

		String backingCfServiceInstanceGuid;

		OsbOperation operationType;

		/**
		 * Required for Jackson deserialization. See https://www.baeldung.com/jackson-exception#2-the-solution
		 */
		public CmdbOperationState() {
		}

		public CmdbOperationState(String backingCfServiceInstanceGuid,
			OsbOperation operationType) {
			this.backingCfServiceInstanceGuid = backingCfServiceInstanceGuid;
			this.operationType = operationType;
		}

		public String getBackingCfServiceInstanceGuid() {
			return backingCfServiceInstanceGuid;
		}

		public OsbOperation getOperationType() {
			return operationType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CmdbOperationState that = (CmdbOperationState) o;

			if (!backingCfServiceInstanceGuid.equals(that.backingCfServiceInstanceGuid)) return false;
			return operationType == that.operationType;
		}

		@Override
		public int hashCode() {
			int result = backingCfServiceInstanceGuid.hashCode();
			result = 31 * result + operationType.hashCode();
			return result;
		}

	}

}
