package com.orange.oss.osbcmdb.serviceinstance;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.osbcmdb.AbstractOsbCmdbService;
import com.orange.oss.osbcmdb.metadata.CreateServiceMetadataFormatterServiceImpl;
import com.orange.oss.osbcmdb.metadata.MetaData;
import com.orange.oss.osbcmdb.metadata.UpdateServiceMetadataFormatterService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.MaintenanceInfo;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersResponse;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.LastOperation;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.serviceInstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v3.serviceInstances.ListServiceInstancesResponse;
import org.cloudfoundry.client.v3.serviceInstances.ServiceInstanceResource;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.util.ExceptionUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
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
import org.springframework.lang.Nullable;

import static com.orange.oss.osbcmdb.metadata.BaseMetadataFormatter.BROKERED_SERVICE_INSTANCE_GUID;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceInstance extends AbstractOsbCmdbService implements ServiceInstanceService {

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static final String X_OSB_CMDB_CUSTOM_KEY_NAME = "x-osb-cmdb";

	protected final Logger LOG = Loggers.getLogger(OsbCmdbServiceInstance.class);

	private final CreateServiceMetadataFormatterServiceImpl createServiceMetadataFormatterService;

	private final ServiceInstanceInterceptor osbInterceptor;

	private final UpdateServiceMetadataFormatterService updateServiceMetadataFormatterService;

	/**
	 * When set to true, then a custom param (whose name is @{link
	 * X_OSB_CMDB_CUSTOM_KEY_NAME}) is sent to backing service with meta-data about osb client context.
	 * This is a transient workaround until OSB 2.16 with annotations is supported and metadata will be passed
	 * as annotations in the OSB context object.
	 * Set to false when a backing broker makes a strict check of received params and rejects osb-cmdb custom param
	 */
	private final boolean propagateMetadataAsCustomParam;

	/**
	 * When set to true, then the custom param sent to backing service (whose name is @{link
	 * X_OSB_CMDB_CUSTOM_KEY_NAME}) is hidden from the get service endpoint response.
	 */
	private final boolean hideMetadataCustomParamInGetServiceInstanceEndpoint;

	private final MaintenanceInfoFormatterService maintenanceInfoFormatterService;

	public OsbCmdbServiceInstance(CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient,
		String defaultOrg, String userName,
		ServiceInstanceInterceptor osbInterceptor,
		CreateServiceMetadataFormatterServiceImpl createServiceMetadataFormatterService,
		UpdateServiceMetadataFormatterService updateServiceMetadataFormatterService,
		boolean propagateMetadataAsCustomParam,
		boolean hideMetadataCustomParamInGetServiceInstanceEndpoint,
		MaintenanceInfoFormatterService maintenanceInfoFormatterService) {
		super(cloudFoundryClient, defaultOrg, userName, cloudFoundryOperations);

		this.osbInterceptor = osbInterceptor;
		this.createServiceMetadataFormatterService = createServiceMetadataFormatterService;
		this.updateServiceMetadataFormatterService = updateServiceMetadataFormatterService;
		this.propagateMetadataAsCustomParam = propagateMetadataAsCustomParam;
		this.hideMetadataCustomParamInGetServiceInstanceEndpoint = hideMetadataCustomParamInGetServiceInstanceEndpoint;
		this.maintenanceInfoFormatterService = maintenanceInfoFormatterService;
	}

	@Override
	public Mono<org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse> getServiceInstance(
		org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.getServiceInstance(request);
		}
		String serviceInstanceId = request.getServiceInstanceId();
		try {
			List<ServiceInstanceResource> backingServicesFromBrokeredServiceGuid = lookupBackingServicesFromBrokeredServiceGuid(
				serviceInstanceId);

			for (ServiceInstanceResource backingServiceInstanceResource : backingServicesFromBrokeredServiceGuid) {
				String backingSpaceId = backingServiceInstanceResource.getRelationships().getSpace().getData().getId();
				SpaceEntity backingSpace = client.spaces().get(GetSpaceRequest.builder()
					.spaceId(backingSpaceId)
					.build())
					.map(GetSpaceResponse::getEntity)
					.block();
				assert backingSpace != null;
				String backingOrganizationId = backingSpace.getOrganizationId();
				OrganizationEntity backingOrganization = client.organizations().get(GetOrganizationRequest.builder()
					.organizationId(backingOrganizationId)
					.build())
					.map(GetOrganizationResponse::getEntity)
					.block();
				assert backingOrganization != null;
				String backingOrganizationName = backingOrganization.getName();
				if (! this.defaultOrg.equals(backingOrganizationName)) {
					LOG.warn("Suspicious request to look up service instance guid {} from another tenant with guid {} and" +
						" name {} whereas tenant org name is {}. Skipping candidate backing service.",
						serviceInstanceId,
						backingOrganizationId,
						backingOrganizationName, this.defaultOrg);
					continue;
				}
				String backingServiceInstanceId = backingServiceInstanceResource.getId();
				ServiceInstanceEntity backingServiceInstance = client.serviceInstances().get(GetServiceInstanceRequest.builder()
					.serviceInstanceId(backingServiceInstanceId)
					.build())
					.map(GetServiceInstanceResponse::getEntity)
					.block();
				assert backingServiceInstance != null : "unable to fetch details of service instance whose id was looked up by name";
				Map<String, Object> backingServiceInstanceParams = client.serviceInstances()
					.getParameters(GetServiceInstanceParametersRequest.builder()
						.serviceInstanceId(backingServiceInstanceId)
						.build())
					.map(GetServiceInstanceParametersResponse::getParameters)
					.block();
				if (hideMetadataCustomParamInGetServiceInstanceEndpoint &&
					backingServiceInstanceParams != null) {
					//Original map is immutable.
					Map<String, Object> sanitizedParams = new HashMap<>(backingServiceInstanceParams);
					Object customParams = sanitizedParams.remove(X_OSB_CMDB_CUSTOM_KEY_NAME);
					LOG.debug("Hiding param with key {} and value {} from GSI response", X_OSB_CMDB_CUSTOM_KEY_NAME, customParams);
					backingServiceInstanceParams = sanitizedParams;
				}
				org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse.GetServiceInstanceResponseBuilder builder =
					org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse.builder();
				if (backingServiceInstanceParams != null) {
					builder.parameters(backingServiceInstanceParams);
				}
				builder.dashboardUrl(backingServiceInstance.getDashboardUrl());
				//Waiting for OSB API 2.16 support in SC-OSB,
				//	see https://github .com/spring-cloud/spring-cloud-open-service-broker/issues/287
	//				.planId(brokeredPlanId)
	//				.serviceDefinitionId(brokeredServiceId)
				return Mono.just(builder.build());
			}
		}
		catch (Exception e) {
			LOG.info("Unable to process {}, caught: {}", request, e.toString(), e);
			throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
		}
		LOG.debug("No brokered service found from metadata with id {}, returning 404", serviceInstanceId);
		throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.createServiceInstance(request);
		}
		validateServiceDefinitionAndPlanIds(request.getServiceDefinition(), request.getPlan(),
			request.getServiceDefinitionId(), request.getPlanId());
		maintenanceInfoFormatterService.validateAnyCreateRequest(request);
		String backingServiceName = request.getServiceDefinition().getName();
		String backingServicePlanName = request.getPlan().getName();
		CloudFoundryOperations spacedTargetedOperations = null;

		MetaData metaData = createServiceMetadataFormatterService.formatAsMetadata(request);
		try {
			spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
			//Lookup guids necessary for low level api usage, and that CloudFoundryOperations hides in its response
			String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);
			String backingServicePlanId = fetchBackingServicePlanId(backingServiceName, backingServicePlanName,
				spaceId);


			CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();
			rejectDuplicateServiceInstanceGuid(request, spacedTargetedOperations);

			org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceResponse createServiceInstanceResponse;
			createServiceInstanceResponse = client.serviceInstances()
				.create(org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest.builder()
					.name(ServiceInstanceNameHelper.truncateNameToCfMaxSize(request.getServiceInstanceId()))
					.servicePlanId(backingServicePlanId)
					.parameters(formatParameters(metaData, request.getParameters()))
					.spaceId(spaceId)
					.build())
				.block();

			//noinspection ConstantConditions
			responseBuilder.dashboardUrl(createServiceInstanceResponse.getEntity().getDashboardUrl());

			LastOperation lastOperation = createServiceInstanceResponse.getEntity().getLastOperation();
			if (lastOperation == null) {
				LOG.error("Unexpected missing last operation from CSI. Full response was {}",
					createServiceInstanceResponse);
				throw new OsbCmdbInternalErrorException("Internal CF protocol error");
			}
			boolean asyncProvisioning;
			switch (lastOperation.getState()) {
				case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
					asyncProvisioning = true;
					//make get last operation faster by tracking the underlying CF instance
					// GUID
					responseBuilder.operation(
						toJson(new CmdbOperationState(createServiceInstanceResponse.getMetadata().getId(),
							OsbOperation.CREATE, metaData)));
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
					asyncProvisioning = false;
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
					LOG.info("Backing service failed to provision with {}, flowing up the error to the osb client",
						lastOperation);
					throw new OsbCmdbInternalErrorException(redactExceptionMessage(lastOperation.getDescription()));
				default:
					LOG.error("Unexpected last operation state:" + lastOperation.getState());
					throw new OsbCmdbInternalErrorException("Internal CF protocol error");
			}
			responseBuilder.async(asyncProvisioning);

			return Mono.just(responseBuilder.build());
		}
		catch (Exception e) {
			LOG.info("Unable to provision service, caught:" + e, e);
			//CF API errors can be multiple and can change without notification
			//See CF service broker client specs at
			// https://github.com/cloudfoundry/cloud_controller_ng/blob/80176ff0068741088e19629516c0285b4cf57ef3/spec/unit/lib/services/service_brokers/v2/client_spec.rb
			// To avoid relying on exceptions thrown to make decisions, we try to diagnose and recover the exception
			// globally by inspecting the backing service instance state instead.
			return handleCreateException(e, backingServiceName, spacedTargetedOperations, request, metaData);
		}
	}

	private Map<String, Object> formatParameters(MetaData metaData, Map<String, Object> requestParams) {
		Map<String, Object> parameters = new HashMap<>(requestParams);
		if (propagateMetadataAsCustomParam) {
			parameters.put(X_OSB_CMDB_CUSTOM_KEY_NAME, metaData);
		}
		return parameters;
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.deleteServiceInstance(request);
		}

		//Validate mandatory service id and plan Id
		validateServiceDefinitionAndPlanIds(request.getServiceDefinition(), request.getPlan(),
			request.getServiceDefinitionId(), request.getPlanId());

		String backingServiceInstanceName =
			ServiceInstanceNameHelper.truncateNameToCfMaxSize(request.getServiceInstanceId());

		//Lookup corresponding service instance in the backend org. This also validates incoming request against
		// security attacks passing forged service instance guid
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(
			request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, backingServiceInstanceName);

		if (existingSi == null) {
			LOG.info("No such backing service instance id={} to delete, return early.", backingServiceInstanceName);
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}

		try {
			// Don't not ask to purge the service instance, as this would create leaks in backing service instance
			// broker. Rather, list and delete service keys first Delete service keys if any.
			spacedTargetedOperations.services().listServiceKeys(ListServiceKeysRequest.builder()
				.serviceInstanceName(backingServiceInstanceName).build())
				.flatMap(sk -> spacedTargetedOperations.services().deleteServiceKey(DeleteServiceKeyRequest.builder()
					.serviceInstanceName(backingServiceInstanceName)
					.serviceKeyName(sk.getName())
					.build()))
				.blockLast();

			client.serviceInstances()
				.delete(org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest.builder()
					.serviceInstanceId(existingSi.getId())
					.acceptsIncomplete(request.isAsyncAccepted())
					.build())
				.block();
			//Even if DELETE /v2/service_instances/guid is observed to return service instance entity during async delete
			//Cf-java-client does not parse it
			//We have no other choice than refetching it
			ServiceInstance deletedSi = getCfServiceInstance(spacedTargetedOperations, backingServiceInstanceName);

			DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse
				.builder();
			if (deletedSi != null) {
				if (!"delete".equals(deletedSi.getLastOperation())) {
					LOG.error("Unexpected si state after delete {} full si is {}", deletedSi.getLastOperation(),
						deletedSi);
					throw new ServiceBrokerException("Internal CF protocol error");
				}
				boolean asyncProvisioning;
				switch (deletedSi.getStatus()) {
					case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
						asyncProvisioning = true;
						//make get last operation faster by tracking the underlying CF instance
						// GUID
						responseBuilder
							.operation(toJson(new CmdbOperationState(deletedSi.getId(),
								OsbOperation.DELETE, null)));
						break;
					case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED: //unlikely, deletedSi would be null instead
						asyncProvisioning = false;
						break;
					case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
						LOG.info("Backing service failed to delete with {}, flowing up the error to the osb " +
								"client",
							deletedSi.getMessage());
						throw new ServiceBrokerException(redactExceptionMessage(deletedSi.getMessage()));
					default:
						LOG.error("Unexpected last operation state:" + deletedSi.getStatus());
						throw new ServiceBrokerException("Internal CF protocol error");
				}
				responseBuilder.async(asyncProvisioning);

			}
			//200 OK
			return Mono.just(responseBuilder.build());
		}
		catch (Exception e) {
			LOG.info("Unable to deprovision service, caught:" + e, e);
			return handleDeleteException(e, backingServiceInstanceName, spacedTargetedOperations, request);
		}
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.getLastOperation(request);
		}

		//Note: we don't validate optional service id and plan Id that we don't use

		String requestOperation = request.getOperation();
		if (requestOperation == null) {
			throw new ServiceBrokerInvalidParametersException("missing operation field");
		}
		CmdbOperationState cmdbOperationState = fromJson(requestOperation);
		String backingServiceInstanceGuid = cmdbOperationState.backingCfServiceInstanceGuid;

		GetServiceInstanceResponse serviceInstanceResponse;
		Exception getServiceInstanceException = null;
		try {
			serviceInstanceResponse = client.serviceInstances()
				.get(GetServiceInstanceRequest.builder().serviceInstanceId(backingServiceInstanceGuid).build())
				.block();
		}
		catch (Exception e) {
			LOG.info("No such backing instance with guid {}, caught: {}" + backingServiceInstanceGuid, e.toString());
			getServiceInstanceException = e;
			serviceInstanceResponse = null;
		}

		OperationState operationState;
		String description = null;
		if (serviceInstanceResponse == null) {
			switch (cmdbOperationState.operationType) {
				case UPDATE: // fall through
				case CREATE:
					String exceptionToString = getServiceInstanceException != null ?
						getServiceInstanceException.toString() : "";
					LOG.error("Unable to provide last operation for {} operation of guid={} Missing service instance " +
							"with exception:{} ",
						cmdbOperationState.operationType, backingServiceInstanceGuid, exceptionToString);
					operationState = OperationState.FAILED;
					description = "missing associated backing service with guid: " + backingServiceInstanceGuid;
					break;
				case DELETE:
					operationState = OperationState.SUCCEEDED;
					break;
				default:
					LOG.error("Unexpected last operation state:" + cmdbOperationState.operationType);
					throw new ServiceBrokerInvalidParametersException(
						"Invalid state operation value:" + cmdbOperationState.operationType);
			}
		}
		else { //backing service exists corresponding to `operation` parameter
			// Check that backing guid passed in operation is indeed consistent with brokered service guid, and was
			// not forged by an attacker
			if ((serviceInstanceResponse.getEntity() == null) ||
				(serviceInstanceResponse.getEntity().getName() == null) ||
				(!request.getServiceInstanceId().equals(serviceInstanceResponse.getEntity().getName()))) {
				//noinspection ConstantConditions
				LOG.error("Unexpected operation parameter whose backing service guid ({}) whose name ({}) does not " +
						"name the brokered service id ({})",
					backingServiceInstanceGuid,
					serviceInstanceResponse.getEntity().getName(),
					request.getServiceInstanceId()
				);
				throw new ServiceBrokerInvalidParametersException("invalid operation field: content mismatch");
			}

			// convert CF state values to OSB values:
			// in progress -> in progress
			// errored -> errored
			LastOperation lastOperation = serviceInstanceResponse.getEntity().getLastOperation();
			operationState = convertCfStateToOsbState(lastOperation.getState());
		}

		switch (operationState) {
			case IN_PROGRESS:
				//wait for completion to update meta-data
				break;

			case SUCCEEDED: // fall through
			case FAILED:
				//Now that service provisionning/update is complete, we can update its metadata, see https://github.com/cloudfoundry/capi-release/issues/183
				if (backingServiceInstanceGuid != null) {
					//Flow up any error during meta-a assignment, as to let CF CC_NG perform the retry.
					updateServiceInstanceMetadata(backingServiceInstanceGuid, cmdbOperationState.metaData);
				}
				break;
		}

		return Mono.just(GetLastServiceOperationResponse.builder()
			.description(description)
			.operationState(operationState)
			.deleteOperation(OsbOperation.DELETE.equals(cmdbOperationState.operationType))
			.build());
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.updateServiceInstance(request);
		}

		validateServiceDefinitionAndPlanIds(request.getServiceDefinition(), request.getPlan(),
			request.getServiceDefinitionId(), request.getPlanId());
		maintenanceInfoFormatterService.validateAnyUpgradeRequest(request);

		String backingServiceName = request.getServiceDefinition().getName();
		String backingServicePlanName = request.getPlan().getName();
		String backingServiceInstanceName = ServiceInstanceNameHelper
			.truncateNameToCfMaxSize(request.getServiceInstanceId());

		//ignore race condition during space creation for K8S dupl requests
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
		ServiceInstance existingBackingServiceInstance = getCfServiceInstance(spacedTargetedOperations,
			backingServiceInstanceName);

		if (existingBackingServiceInstance == null) {
			LOG.info("Asked to update a brokered service instance with guid {} for which no backing service instance " +
				"was found");
			throw new ServiceInstanceDoesNotExistException(backingServiceInstanceName);
		}

		//Lookup guids necessary for low level api usage, and that CloudFoundryOperations hides in its response
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);
		String backingServicePlanId = fetchBackingServicePlanId(backingServiceName, backingServicePlanName, spaceId);

		if (maintenanceInfoFormatterService.isNoOpUpgradeBackingService(request)) {
			LOG.info("NoOp upgrade detected, returning early 200 OK");
			return Mono.just(UpdateServiceInstanceResponse.builder()
				.dashboardUrl(existingBackingServiceInstance.getDashboardUrl())
				.async(false)
				.build());
		}

		// Lets assume for now that OSB client properly fill in the PreviousValue.maintenance_info, and that we don't
		//		 need to fetch it from the existing backing instance
		/*
		MaintenanceInfo existingBackingServiceInstanceEntityMaintenanceInfo = null;
		if (existingBackingServiceInstance != null && maintenanceInfoFormatterService.hasMaintenanceInfoChangeRequest(request)) {
			existingBackingServiceInstanceEntityMaintenanceInfo = client.serviceInstances().get(GetServiceInstanceRequest.builder()
				.serviceInstanceId(existingBackingServiceInstance.getId())
				.build())
				.map(GetServiceInstanceResponse::getEntity)
				.map(ServiceInstanceEntity::getMaintenanceInfo)
				.block();
		}
		 */

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();
		MetaData metaData = updateServiceMetadataFormatterService.formatAsMetadata(request);

		try {
			org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceResponse updateServiceInstanceResponse;
			MaintenanceInfo formattedForBackendInstanceMI = maintenanceInfoFormatterService.formatForBackendInstance(request);
			LOG.debug("Passing formatted maintenance info {} to backing service", formattedForBackendInstanceMI);
			updateServiceInstanceResponse = client.serviceInstances()
				.update(org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceRequest.builder()
					.serviceInstanceId(existingBackingServiceInstance.getId())
					.servicePlanId(backingServicePlanId)
					.parameters(formatParameters(metaData, request.getParameters()))
					.maintenanceInfo(formattedForBackendInstanceMI)
					.build())
				.block();

			//noinspection ConstantConditions
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
							OsbOperation.UPDATE, metaData)));
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
					asyncProvisioning = false;
					break;
				case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
					LOG.info("Backing service failed to update with {}, flowing up the error to the osb " +
							"client",
						lastOperation);
					throw new ServiceBrokerException(redactExceptionMessage(lastOperation.getDescription()));
				default:
					LOG.error("Unexpected last operation state:" + lastOperation.getState());
					throw new ServiceBrokerException("Internal CF protocol error");
			}
			responseBuilder.async(asyncProvisioning);
			responseBuilder.dashboardUrl(updateServiceInstanceResponse.getEntity().getDashboardUrl());
		}
		catch (Exception e) {
			LOG.info("Unable to update service, caught:" + e, e);
			return handleUpdateException(e, backingServiceInstanceName, spacedTargetedOperations, request, metaData);
		}
		return Mono.just(responseBuilder.build());
	}

	protected CmdbOperationState fromJson(String operation) {
		try {
			return OBJECT_MAPPER.readValue(operation, CmdbOperationState.class);
		}
		catch (JsonProcessingException e) {
			throw new ServiceBrokerInvalidParametersException("Invalid operation content: " + operation + " parsing " +
				"failed with:" + e);
		}
	}

	protected String toJson(CmdbOperationState cmdbOperationState) {
		try {
			return OBJECT_MAPPER.writeValueAsString(cmdbOperationState);
		}
		catch (JsonProcessingException e) {
			LOG.error("Unable to json serialize {} caught {}", cmdbOperationState, e.toString());
			throw new OsbCmdbInternalErrorException(e.getMessage(), e);
		}
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
				NoSuchElementException.class,
				t -> ExceptionUtils.illegalArgument("Service %s does not exist", backingServiceName))
			.map(ResourceUtils::getId)
			.block();

		//noinspection ConstantConditions
		return PaginationUtils
			.requestClientV2Resources(page -> client.servicePlans()
				.list(ListServicePlansRequest.builder()
					.page(page)
					.serviceId(backingServiceId)
					.build()))
			.filter(resource -> backingServicePlanName.equals(ResourceUtils.getEntity(resource).getName()))
			.single()
			.map(ResourceUtils::getId)
			.onErrorResume(NoSuchElementException.class,
				t -> ExceptionUtils.illegalArgument("Service plan %s does not exist", backingServicePlanName))
			.block();
	}

	private String getOrgIdFromTargettedOperationsInternals(CloudFoundryOperations spacedTargetedOperations) {
		DefaultCloudFoundryOperations spacedTargetedOperationsInternals = (DefaultCloudFoundryOperations) spacedTargetedOperations;
		String orgId = spacedTargetedOperationsInternals.getOrganizationId().block();
		if (orgId == null) {
			LOG.error("Unexpected null orgId in DefaultCloudFoundryOperations {}", spacedTargetedOperationsInternals);
			throw new ServiceBrokerException("Internal CF client error");
		}
		return orgId;
	}

	private String getRequestIncompatibilityWithExistingInstance(ServiceInstance existingServiceInstance,
		ServiceDefinition serviceDefinition, Plan plan) {
		if (!existingServiceInstance.getService().equals(serviceDefinition.getName())) {
			return "service definition mismatch with:" + existingServiceInstance.getService();
		}
		if (!existingServiceInstance.getPlan().equals(plan.getName())) {
			return "service plan mismatch with:" + existingServiceInstance.getPlan();
		}
		return null; //no incompatibility
	}

	/**
	 * Handle exceptions by checking current backing service instance in order to return the appropriate response
	 * as expected in the OSB specifications.
	 *
	 * @param originalException The exception occuring during processing. Note that Subclasses of
	 * 	ServiceBrokerException are considered already qualified, and returned as-is
	 * @param metaData metadata to assign to service instance upon its async completion
	 */
	private Mono<CreateServiceInstanceResponse> handleCreateException(Exception originalException,
		String backingServiceName,
		CloudFoundryOperations spacedTargetedOperations,
		CreateServiceInstanceRequest request, MetaData metaData) {
		LOG.info("Inspecting exception caught {} for possible concurrent dupl while handling request {} ",
			originalException, request);

		if (originalException instanceof ServiceBrokerException && !originalException.getClass()
			.equals(ServiceBrokerException.class)) {
			LOG.info("Exception was thrown by ourselves, and already qualified, rethrowing it unmodified");
			throw (ServiceBrokerException) originalException;
		}

		if (spacedTargetedOperations == null) {
			spacedTargetedOperations = getSpaceScopedOperations(backingServiceName);
		}

		//Lookup guids necessary for low level api usage, and that CloudFoundryOperations hides in its response
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);

		ServiceInstance existingServiceInstance = null;
		try {
			existingServiceInstance = spacedTargetedOperations.services()
				.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
					.name(request.getServiceInstanceId())
					.build())
				.block();
		}
		catch (Exception exception) {
			LOG.info("Unable to lookup existing service with id={} caught {}", request.getServiceInstanceId(),
				exception.toString());
		}
		if (existingServiceInstance != null) {
			String incompatibilityWithExistingInstance = getRequestIncompatibilityWithExistingInstance(
				existingServiceInstance, request.getServiceDefinition(), request.getPlan());
			if (incompatibilityWithExistingInstance == null) {
				if ("succeeded".equals(existingServiceInstance.getStatus())) {
					LOG.info("Concurrent request is not incompatible and was completed previously: 200");
					//200 OK. See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/a8fa63fdfcbf0f2f6e103ae1f9082ff3c59e7a90/spring-cloud-open-service-broker-core/src/test/java/org/springframework/cloud/servicebroker/controller/ServiceInstanceControllerResponseCodeTest.java#L89-L94
					return Mono.just(CreateServiceInstanceResponse.builder()
						.dashboardUrl(existingServiceInstance.getDashboardUrl())
						.async(false)
						.instanceExisted(true)
						.build());
				}
				else {
					LOG.info("Concurrent request is not incompatible and is still in progress success: 202");
					String operation = toJson(
						new CmdbOperationState(existingServiceInstance.getId(), OsbOperation.CREATE,
							metaData));
					//202 Accepted (can't yet throw ServiceBrokerCreateOperationInProgressException,
					//because despite fix for
					//https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/284
					//we're still lacking support for returning dashboard url)
					return Mono.just(CreateServiceInstanceResponse.builder()
						.dashboardUrl(existingServiceInstance.getDashboardUrl())
						.operation(operation)
						.async(true)
						.instanceExisted(false)
						.build());
				}
			}
			else {
				LOG.info("Concurrent request is incompatible, returning conflicts with msg {}",
					incompatibilityWithExistingInstance);
				throw new ServiceInstanceExistsException(incompatibilityWithExistingInstance,
					request.getServiceInstanceId(), request.getServiceDefinitionId()); //409
			}
		}
		LOG.info("No existing instance in the inventory with id={} in space with id={}, the exception is likely not " +
			"related to concurrent or conflicting duplicate, rethrowing it", request.getServiceInstanceId(), spaceId);
		throw redactExceptionAndWrapAsServiceBrokerException(originalException);
	}

	/**
	 * Handle exceptions by checking current backing service instance in order to return the appropriate response
	 * as expected in the OSB specifications.
	 *
	 * @param originalException The exception occuring during processing. Note that Subclasses of
	 * 	ServiceBrokerException are considered already qualified, and returned as-is
	 */
	private Mono<DeleteServiceInstanceResponse> handleDeleteException(Exception originalException,
		String backingServiceInstanceName,
		CloudFoundryOperations spacedTargetedOperations,
		DeleteServiceInstanceRequest request) {
		LOG.info("Inspecting exception caught {} for possible concurrent dupl while handling request {} ",
			originalException, request);

		if (originalException instanceof ServiceBrokerException && !originalException.getClass()
			.equals(ServiceBrokerException.class)) {
			LOG.info("Exception was thrown by ourselves, and already qualified, rethrowing it unmodified");
			throw (ServiceBrokerException) originalException;
		}

		//Can't reuse code from delete, because we should return 410 when missing
		ServiceInstance deletedSi = getCfServiceInstance(spacedTargetedOperations, backingServiceInstanceName);

		if (deletedSi != null) {
			switch (deletedSi.getStatus()) {
				case "in progress":
					LOG.info("Concurrent deprovisionning request is still in progress. " +
						"Returning accepted: 202");
					String operation = toJson(new CmdbOperationState(deletedSi.getId(),
						OsbOperation.DELETE, null));
					//202 Accepted
					return Mono.just(DeleteServiceInstanceResponse.builder()
						.operation(operation)
						.async(true)
						.build());

				case "failed":
					LOG.info("Backing service failed to delete with {}, flowing up the error to the osb " +
							"client",
						deletedSi.getMessage());
					//500 error
					throw new ServiceBrokerException(deletedSi.getMessage());

				default:
					LOG.error("Unexpected last operation state:" + deletedSi.getStatus());
					throw new ServiceBrokerException("Internal CF protocol error");
			}
		}
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);
		LOG.info("No existing instance in the inventory with id={} in space with id={}, assuming race condition " +
			"among concurrent deprovisionning request. Returning 410", request.getServiceInstanceId(), spaceId);
		//410 Gone
		throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
	}

	/**
	 * Handle exceptions by checking current backing service instance in order to return the appropriate response
	 * as expected in the OSB specifications.
	 *
	 * @param originalException The exception occuring during processing. Note that Subclasses of
	 * 	ServiceBrokerException are considered already qualified, and returned as-is
	 * @param metaData metadata to update to service instance upon its async completion
	 */
	private Mono<UpdateServiceInstanceResponse> handleUpdateException(Exception originalException,
		String backingServiceInstanceName,
		CloudFoundryOperations spacedTargetedOperations,
		UpdateServiceInstanceRequest request,
		MetaData metaData) {
		LOG.info("Inspecting exception caught {} for possible concurrent dupl while handling request {} ",
			originalException, request);

		if (originalException instanceof ServiceBrokerException && !originalException.getClass()
			.equals(ServiceBrokerException.class)) {
			LOG.info("Exception was thrown by ourselves, and already qualified, rethrowing it unmodified");
			throw (ServiceBrokerException) originalException;
		}

		ServiceInstance updatedSi = getCfServiceInstance(spacedTargetedOperations, backingServiceInstanceName);
		String spaceId = getSpacedIdFromTargettedOperationsInternals(spacedTargetedOperations);

		if (updatedSi != null) {
			if (!"update".equals(updatedSi.getLastOperation())) {
				LOG.info("No instance in the inventory with id={} in space with id={}, and recently updated: " +
						"last_operation={}, flowing up original exception",
					request.getServiceInstanceId(), spaceId, updatedSi.getLastOperation());
				//500 error
				throw new ServiceBrokerInvalidParametersException(originalException.getMessage());
			}
			switch (updatedSi.getStatus()) {
				case OsbApiConstants.LAST_OPERATION_STATE_INPROGRESS:
					LOG.info("Concurrent update request is still in progress. " +
						"Returning accepted: 202");
					String operation = toJson(new CmdbOperationState(updatedSi.getId(),
						OsbOperation.UPDATE, metaData));
					//202 Accepted
					return Mono.just(UpdateServiceInstanceResponse.builder()
						.operation(operation)
						.async(true)
						.build());

				case OsbApiConstants.LAST_OPERATION_STATE_SUCCEEDED:
					if (updatedSi.getService().equals(request.getServiceDefinition().getName()) &&
						updatedSi.getPlan().equals(request.getPlan().getName())) {
						LOG.info("Concurrent update request has completed. " +
							"Returning 200 OK");
						//200 OK
						return Mono.just(UpdateServiceInstanceResponse.builder()
							.async(false)
							.build());
					}
					else {
						LOG.info("Plan update did not succeed while SI update completed, assuming invalid input. " +
								"Existing si service name={} and service plan={}", updatedSi.getService(),
							updatedSi.getPlan());
						throw new ServiceBrokerInvalidParametersException(originalException.getMessage());
					}

				case OsbApiConstants.LAST_OPERATION_STATE_FAILED:
					LOG.info("Backing service failed to update with {}, flowing up the original error to the osb " +
							"client: ",
						updatedSi.getMessage(), originalException.getMessage());
					//500 error
					//In the future, return the usable field once CF supports it
					throw new ServiceBrokerException(originalException.getMessage(), originalException);
				default:
					LOG.error("Unexpected last operation state:" + updatedSi.getStatus());
					throw new ServiceBrokerException("Internal CF protocol error");
			}
		}
		LOG.info("No existing instance in the inventory with id={} in space with id={}, assuming invalid requestrace " +
			"condition " +
			"among concurrent update request. Returning 410", request.getServiceInstanceId(), spaceId);
		//410 Gone
		throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
	}

	private void rejectDuplicateServiceInstanceGuid(CreateServiceInstanceRequest request,
		CloudFoundryOperations spacedTargetedOperations) {
		List<ServiceInstanceResource> existingBackingServicesWithSameInstanceGuid;
		String serviceInstanceId = request.getServiceInstanceId();
		existingBackingServicesWithSameInstanceGuid = lookupBackingServicesFromBrokeredServiceGuid(serviceInstanceId);
		if (!existingBackingServicesWithSameInstanceGuid.isEmpty()) {
			LOG.info("Service instance guid {} already exists and is backed by services: {}",
				serviceInstanceId, existingBackingServicesWithSameInstanceGuid);
		}

		for (ServiceInstanceResource serviceInstanceResource : existingBackingServicesWithSameInstanceGuid) {
			String backingSpaceId = serviceInstanceResource.getRelationships().getSpace().getData().getId();
			SpaceEntity backingSpace = client.spaces().get(GetSpaceRequest.builder()
				.spaceId(backingSpaceId)
				.build())
				.map(GetSpaceResponse::getEntity)
				.block();
			assert backingSpace != null;
			String backingOrganizationId = backingSpace.getOrganizationId();
			String currentTenantOrgId = getOrgIdFromTargettedOperationsInternals(spacedTargetedOperations);
			if (!backingOrganizationId.equals(currentTenantOrgId)) {
				LOG.warn("Suspicious service instance id={} reused across tenants. Requested in current tenant " +
						"with orgId {} while SI id exists in other tenant with orgId {}. Still accepting the request," +
						" but statistically unlikely",
					serviceInstanceId, currentTenantOrgId, backingOrganizationId);
				continue;
			}
			String backingSpaceName = backingSpace.getName();
			LOG.info("Corresponding backing org id={} space name={}", backingOrganizationId, backingSpaceName);
			if (!request.getServiceDefinition().getName().equals(backingSpaceName)) {
				String msg = "Existing conflicting service in same backing org with different service "
					+ "definition name: backingSpaceName=" + backingSpaceName
					+ " additional metadata: " + serviceInstanceResource.getMetadata();
				LOG.info(msg);
				throw new ServiceInstanceExistsException(msg,
					serviceInstanceId, request.getServiceDefinitionId()); //409
			}
		}
	}

	private List<ServiceInstanceResource> lookupBackingServicesFromBrokeredServiceGuid(
		String brokeredServiceInstanceId) {
		List<ServiceInstanceResource> existingBackingServicesWithSameInstanceGuid;
		String labelSelector = BROKERED_SERVICE_INSTANCE_GUID + "=" + brokeredServiceInstanceId;
		try {
			existingBackingServicesWithSameInstanceGuid = client.serviceInstancesV3()
				.list(ListServiceInstancesRequest.builder()
					.labelSelector(labelSelector)
					.build())
				.map(ListServiceInstancesResponse::getResources)
				.flatMapMany(Flux::fromIterable)
				.collectList()
				.block();
		}
		catch (Exception e) {
			LOG.error("Unable to lookup potential duplicates with label {}, caught {}", labelSelector, e);
			throw new OsbCmdbInternalErrorException("Internal CF protocol error");
		}
		assert existingBackingServicesWithSameInstanceGuid != null;
		return existingBackingServicesWithSameInstanceGuid;
	}

	private void updateMetadata(MetaData metaData, String serviceInstanceId) {
		metaData.getLabels().put("backing_service_instance_guid", serviceInstanceId); //Ideally should be
		// assigned within MetadataFormatter instead to centralize the logic. Just avoids passing the Id around as a
		// 1st step.
		LOG.debug("Assigning metadata to service instance with id={} annotations={} and labels={}",
			serviceInstanceId, metaData.getAnnotations(), metaData.getLabels());

		client.serviceInstancesV3()
			.update(org.cloudfoundry.client.v3.serviceInstances.UpdateServiceInstanceRequest.builder()
				.serviceInstanceId(serviceInstanceId)
				.metadata(Metadata.builder()
					.annotations(metaData.getAnnotations())
					.labels(metaData.getLabels())
					.build())
				.build())
			.block();
	}

	private void updateServiceInstanceMetadata(String serviceInstanceId, MetaData metaData) {
		updateMetadata(metaData, serviceInstanceId);
	}

	protected enum OsbOperation {
		CREATE,
		UPDATE,
		DELETE
	}

	protected static class CmdbOperationState {

		String backingCfServiceInstanceGuid;

		OsbOperation operationType;

		@Nullable
		MetaData metaData;

		/**
		 * Required for Jackson deserialization. See https://www.baeldung.com/jackson-exception#2-the-solution
		 */
		@SuppressWarnings("unused")
		public CmdbOperationState() {
		}

		/**
		 * construct a new {@link CmdbOperationState}
		 * @param metaData metadata to assign/update to service instance, or null to not update/assign
		 * one
		 */
		public CmdbOperationState(String backingCfServiceInstanceGuid,
			OsbOperation operationType,
			@Nullable MetaData metaData) {
			this.backingCfServiceInstanceGuid = backingCfServiceInstanceGuid;
			this.operationType = operationType;
			this.metaData = metaData;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CmdbOperationState that = (CmdbOperationState) o;

			if (!backingCfServiceInstanceGuid.equals(that.backingCfServiceInstanceGuid)) return false;
			if (operationType != that.operationType) return false;
			return metaData != null ? metaData.equals(that.metaData) : that.metaData == null;
		}

		@Override
		public int hashCode() {
			int result = backingCfServiceInstanceGuid.hashCode();
			result = 31 * result + operationType.hashCode();
			result = 31 * result + (metaData != null ? metaData.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "CmdbOperationState{" +
				"backingCfServiceInstanceGuid='" + backingCfServiceInstanceGuid + '\'' +
				", operationType=" + operationType +
				", metaData=" + metaData +
				'}';
		}

		@SuppressWarnings("unused")
		public String getBackingCfServiceInstanceGuid() {
			return backingCfServiceInstanceGuid;
		}

		@SuppressWarnings("unused")
		@Nullable
		public MetaData getMetaData() { return metaData; }

		@SuppressWarnings("unused")
		public OsbOperation getOperationType() { return operationType; }

	}

}
