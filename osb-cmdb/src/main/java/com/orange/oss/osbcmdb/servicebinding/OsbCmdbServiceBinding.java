package com.orange.oss.osbcmdb.servicebinding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.osbcmdb.serviceinstance.OsbCmdbServiceBrokerException;
import java.time.Duration;

import com.orange.oss.osbcmdb.AbstractOsbCmdbService;
import java.util.function.Function;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyResponse;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.jobs.GetJobRequest;
import org.cloudfoundry.client.v3.jobs.GetJobResponse;
import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.client.v3.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v3.servicebindings.CreateServiceBindingResponse;
import org.cloudfoundry.client.v3.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingDetailsRequest;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingDetailsResponse;
import org.cloudfoundry.client.v3.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v3.servicebindings.ListServiceBindingsResponse;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.util.JobUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceBinding extends AbstractOsbCmdbService implements ServiceInstanceBindingService {

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private final Logger LOG = Loggers.getLogger(OsbCmdbServiceBinding.class);

	private final ServiceBindingInterceptor osbInterceptor;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public OsbCmdbServiceBinding(CloudFoundryClient cloudFoundryClient, String defaultOrg, String userName,
		CloudFoundryOperations cloudFoundryOperations, ServiceBindingInterceptor osbInterceptor) {
		super(cloudFoundryClient, defaultOrg, userName, cloudFoundryOperations);
		this.osbInterceptor = osbInterceptor;
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		final CreateServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.createServiceInstanceBinding(request);
		}

		//No need to validate mandatory service id and plan Id as sc-osb does it already

		//Lookup corresponding service instance in the backend org to validate incoming request against security
		// attacks passing forged service instance guid
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(
			request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		if (existingSi == null) {
			LOG.warn("Asked to bind service instance id={} which does not exists in backing space associated with " +
				"service definition name={}", request.getServiceInstanceId(), request.getServiceDefinition().getName());
			throw new ServiceBrokerInvalidParametersException("instance_id path param: " + request.getServiceInstanceId() + " " +
				"does not match service_id=" + request.getServiceDefinitionId() + " (possibly missing backing service " +
				"instance guid associated with requested instance_id of type service_id))");
		}


		return createServiceBindingCapiv3OBlocked(request, existingSi);


//		return createServiceBindingCapiV2(request, existingSi);

	}

	@NotNull
	private Mono<CreateServiceInstanceBindingResponse> createServiceBindingCapiV2(
		CreateServiceInstanceBindingRequest request, ServiceInstance existingSi) {
		//Try to use the v2 api to request a synchronous service binding creation
		try {
			CreateServiceKeyResponse createServiceKeyResponse = client.serviceKeys()
				.create(CreateServiceKeyRequest.builder()
					.serviceInstanceId(existingSi.getId())
					.parameters(request.getParameters())
					.name(request.getBindingId())
					.build())
				.block();

			//If no error, assume async binding completed synchronously, and return success
			assert createServiceKeyResponse != null;
			assert createServiceKeyResponse.getEntity().getCredentials() != null;

			//Return 201 Created
			return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
				.credentials(createServiceKeyResponse.getEntity().getCredentials())
				.async(false)
				.build());
		}
		catch (Exception originalException) {
			//Only proceed when receiving async required exception
			if (isExceptionReportingAsyncRequired(originalException)) {
				LOG.info("Unable to create sync service binding, caught:" + originalException + " Trying async");
			}
			else {
				LOG.info("Unable to create service binding, caught:" + originalException);
				throw redactExceptionAndWrapAsServiceBrokerException(originalException);
			}
		}


		try {
			//Ask for async binding creation. No async opt-out is supported in CAPI v3,
			// see http://v3-apidocs.cloudfoundry.org/version/3.203.0/index.html#asynchronous-operations
			// > Unlike V2, clients cannot opt-in for asynchronous responses from endpoints.
			CreateServiceBindingRequest createServiceBindingRequest = CreateServiceBindingRequest.builder()
				.relationships(
					ServiceBindingRelationships.builder()
						.serviceInstance(
							ToOneRelationship.builder()
								.data(Relationship.builder().id(existingSi.getId()).build())
								.build())
						.build())
				.type(ServiceBindingType.KEY)
				.parameters(request.getParameters())
				.name(request.getBindingId())
				.build();

			CreateServiceBindingResponse createServiceBindingResponse = client.serviceBindingsV3()
				.create(createServiceBindingRequest).block();

			assert createServiceBindingResponse != null;
			assert createServiceBindingResponse.getJobId().isPresent();
			String jobId = createServiceBindingResponse.getJobId().get();

			//Return 202 Accepted
			return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
				.async(true)
				.operation(toJson(new CmdbOperationState(jobId, OsbOperation.CREATE)))
				.build());
		}
		catch (Exception originalException) {
			LOG.info("Unable to create async service binding, caught:" + originalException);
			throw redactExceptionAndWrapAsServiceBrokerException(originalException);
		}
	}


	private static Mono<String> requestSingleListServiceBindingsId(
		CloudFoundryClient cloudFoundryClient, String serviceInstanceName, String serviceBindingName) {
		return PaginationUtils.requestClientV3Resources(
			page ->
				cloudFoundryClient
					.serviceBindingsV3()
					.list(
						ListServiceBindingsRequest.builder()
							.page(page)
							.serviceInstanceName(serviceInstanceName)
							.name(serviceBindingName)
							.build()))
					.single()
					.map(ServiceBindingResource::getId)
			.switchIfEmpty(Mono.error(new ServiceBrokerException("Unable to list created service binding")));
	}

	/**
	 * Currently blocked attempt to use capi v3 only calls: JobId is always returned
	 */
	@NotNull
	private Mono<CreateServiceInstanceBindingResponse> createServiceBindingCapiv3OBlocked(
		CreateServiceInstanceBindingRequest request, ServiceInstance existingSi) {
		try {
			final boolean asyncAccepted = request.isAsyncAccepted();
			//Ask for async binding creation. No async opt-out is supported in CAPI v3,
			// see http://v3-apidocs.cloudfoundry.org/version/3.203.0/index.html#asynchronous-operations
			// > Unlike V2, clients cannot opt-in for asynchronous responses from endpoints.
			String serviceBindingName = request.getBindingId();
			CreateServiceBindingRequest createServiceBindingRequest = CreateServiceBindingRequest.builder()
				.relationships(
					ServiceBindingRelationships.builder()
						.serviceInstance(
							ToOneRelationship.builder()
								.data(Relationship.builder().id(existingSi.getId()).build())
								.build())
						.build())
				.type(ServiceBindingType.KEY)
				.parameters(request.getParameters())
				.name(serviceBindingName)
				.build();

			if (!asyncAccepted) {
				//Async not accepted, try to synchronously peek async job for completion and return credentials
				//If backing broker does not support async, i.e. if job is'nt complete immediately, then error.
				return client.serviceBindingsV3()
					.create(createServiceBindingRequest)
					.map(response -> response.getJobId().get())
					.flatMap(
						jobId ->
							JobUtils.waitForCompletion(client, Duration.ofSeconds(5), jobId))
					.then(requestSingleListServiceBindingsId(client, existingSi.getName(), serviceBindingName))
					.flatMap(requestServiceBindingDetails())
					.map(GetServiceBindingDetailsResponse::getCredentials)
					.switchIfEmpty(Mono.error(new ServiceBrokerException("Missing credentials in returned " +
						"backing service key")))
					.map(credentials ->
						CreateServiceInstanceAppBindingResponse.builder()
							.credentials(credentials)
							.build())
					.cast(CreateServiceInstanceBindingResponse.class)
					.doOnRequest(next -> {
						LOG.info("Start CSK with async not accepted");
					})
					.doOnSuccess(next -> {
						LOG.info("End CSK with async not accepted, returning {}", next);
					});
			} else {
				return client.serviceBindingsV3()
					.create(createServiceBindingRequest)
					// return 202 Accepted
					.map(response -> {
						return CreateServiceInstanceAppBindingResponse.builder()
							.async(true)
							.operation(toJson(new CmdbOperationState(response.getJobId().get(), OsbOperation.CREATE)))
							.build();
					});

			}

		}
		catch (Exception originalException) {
			LOG.info("Unable to create async service binding, caught:" + originalException);
			throw redactExceptionAndWrapAsServiceBrokerException(originalException);
		}
	}

	@NotNull
	private Function<String, Mono<? extends GetServiceBindingDetailsResponse>> requestServiceBindingDetails() {
		return serviceBindingId ->
			client
				.serviceBindingsV3()
				.getDetails(
					GetServiceBindingDetailsRequest.builder()
						.serviceBindingId(serviceBindingId)
						.build())
			.switchIfEmpty(Mono.error(new ServiceBrokerException("Unable to get service binding details")));
	}

	private boolean isExceptionReportingAsyncRequired(Exception originalException) {
		boolean asyncRequired=false;
		if (originalException instanceof ClientV2Exception) {
			ClientV2Exception clientV2Exception = (ClientV2Exception) originalException;
			Integer clientV2ExceptionCode = clientV2Exception.getCode();
			//OUT Caused by: org.cloudfoundry.client.v2.ClientV2Exception: CF-AsyncRequired(10001): This service plan requires client support for asynchronous service operations.
			if (clientV2ExceptionCode != null && clientV2ExceptionCode.equals(10001)) {
				asyncRequired=true;
			}
		}
		return asyncRequired;
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
		GetLastServiceBindingOperationRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.getLastOperation(request);
		}

		try {
			CmdbOperationState cmdbOperationState = fromJson(request.getOperation());
			GetJobResponse jobResponse = client.jobsV3().get(GetJobRequest.builder()
					.jobId(cmdbOperationState.getAsyncJobId())
					.build())
				.block();

			assert jobResponse != null;
			return Mono.just(GetLastServiceBindingOperationResponse.builder()
				.deleteOperation(OsbOperation.DELETE.equals(cmdbOperationState.operationType))
				.operationState(convertCfStateToOsbState(jobResponse.getState()))
				.build());
		}
		catch (Exception originalException) {
			//CF API errors can be multiple and can change without notification
			// To avoid relying on exceptions thrown to make decisions, we try to diagnose and recover the exception
			// globally by inspecting the backing service instance state instead.
			LOG.info("Unable to get async service binding last operation with operations " + request.getOperation() + originalException );
			throw redactExceptionAndWrapAsServiceBrokerException(originalException);
		}
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.getServiceInstanceBinding(request);
		}

		try {
			ListServiceBindingsResponse listServiceBindingsResponse = client
				.serviceBindingsV3()
				.list(ListServiceBindingsRequest.builder()
					.type(ServiceBindingType.KEY)
					.name(request.getBindingId())
					.build())
				.block();

			if (listServiceBindingsResponse == null || listServiceBindingsResponse.getResources().isEmpty()) {
				throw new OsbCmdbServiceBrokerException("No service bindings found for bindingId=" + request.getBindingId());
			}
			assert listServiceBindingsResponse.getResources().size() == 1;
			String serviceBindingId = listServiceBindingsResponse.getResources().get(0).getId();

			GetServiceBindingDetailsResponse serviceBindingDetailsResponse = client
				.serviceBindingsV3()
				.getDetails(
					GetServiceBindingDetailsRequest.builder()
						.serviceBindingId(serviceBindingId)
						.build())
				.block();

			assert serviceBindingDetailsResponse != null;
			return Mono.just(GetServiceInstanceAppBindingResponse.builder()
				.credentials(serviceBindingDetailsResponse.getCredentials())
				.build());
		}
		catch (Exception originalException) {
			LOG.info("Unable to get async service binding with id=" + request.getBindingId() + " caught:" + originalException );
			throw redactExceptionAndWrapAsServiceBrokerException(originalException);
		}
	}

	private OperationState convertCfStateToOsbState(JobState cfServiceInstanceState) {
		//Source of truth: https://apidocs.cloudfoundry.org/12.42.0/service_instances/creating_a_service_instance.html
		//TODO: consider moving the values in the enum and remove the switch statement
		switch (cfServiceInstanceState) {
			case COMPLETE:
				return OperationState.SUCCEEDED;
			case FAILED:
				return OperationState.FAILED;

			//See http://v3-apidocs.cloudfoundry.org/version/3.203.0/index.html#jobs
			// POLLING happens during asynchronous services operations that require polling the last operation from the service broker
			case POLLING: //fall through
			case PROCESSING:
				return OperationState.IN_PROGRESS;
			default:
				LOG.error("Unknown CF service instance state {}", cfServiceInstanceState);
				throw new RuntimeException("Unknown CF service instance state " + cfServiceInstanceState);
		}
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.deleteServiceInstanceBinding(request);
		}

		//No need to validate mandatory service id and plan Id as sc-osb does it already

		//Lookup corresponding service instance to validate incoming request against security attacks passing
		// forged service instance guid
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(
			request.getServiceDefinition().getName());
		ServiceInstance backingServiceInstance = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		if (backingServiceInstance == null) {
			LOG.warn("No such service instance id={} to delete binding from, client error or attempt to delete " +
					"binding from unauthorized service instance.",
				request.getServiceInstanceId());
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}

		//Try to use the v2 api to request a synchronous service binding deletion
		String backingServiceInstanceName = backingServiceInstance.getName();
		String backingServiceKeyName = request.getBindingId();
		try {
			spacedTargetedOperations.services()
				.deleteServiceKey(org.cloudfoundry.operations.services.DeleteServiceKeyRequest.builder()
					.serviceInstanceName(backingServiceInstanceName)
					.serviceKeyName(backingServiceKeyName)
					.build())
				.block();

			//For now CF api V2 & V3 do not support async service bindings
			return Mono.just(DeleteServiceInstanceBindingResponse.builder()
				.build());
		}
		catch (Exception e) {
			if (isExceptionReportingAsyncRequired(e)) {
				LOG.info("Unable to delete sync service binding, caught:" + e + " Trying async");
			} else {
				LOG.info(
					"Unable to delete backing service key with name={} from backing service instance name={} Got {}",
					backingServiceKeyName,
					backingServiceInstanceName, e.toString(), e);
				throw redactExceptionAndWrapAsServiceBrokerException(e);
			}
		}

		try {
			return spacedTargetedOperations.services()
				.getServiceKey(GetServiceKeyRequest.builder()
					.serviceKeyName(backingServiceKeyName)
					.serviceInstanceName(backingServiceInstanceName)
					.build())
				.map(ServiceKey::getId)
				.flatMap(serviceBindingId ->
					client.serviceBindingsV3().delete(
						DeleteServiceBindingRequest.builder()
							.serviceBindingId(serviceBindingId)
							.build())
				)
				.map(jobId ->
					DeleteServiceInstanceBindingResponse.builder()
						.async(true)
						.operation(toJson(new CmdbOperationState(jobId, OsbOperation.DELETE)))
						.build());
		} catch (Exception originalException) {
				LOG.info("Unable to create async service binding, caught:" + originalException);
				throw redactExceptionAndWrapAsServiceBrokerException(originalException);
			}

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
			throw new OsbCmdbServiceBrokerException(e.getMessage(), e);
		}
	}

	
	protected enum OsbOperation {
		CREATE,
		DELETE
	}

	protected static class CmdbOperationState {

		String asyncJobId;

		OsbOperation operationType;

		/**
		 * Required for Jackson deserialization. See
		 * <a href="https://www.baeldung.com/jackson-exception#2-the-solution">details about default constructor</a>
		 */
		@SuppressWarnings("unused")
		public CmdbOperationState() {
		}

		public CmdbOperationState(String asyncJobId,
			OsbOperation operationType) {
			this.asyncJobId = asyncJobId;
			this.operationType = operationType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CmdbOperationState that = (CmdbOperationState) o;

			if (!asyncJobId.equals(that.asyncJobId)) return false;
			return operationType == that.operationType;
		}

		@SuppressWarnings("unused")
		public String getAsyncJobId() {
			return asyncJobId;
		}

		@SuppressWarnings("unused")
		public OsbOperation getOperationType() {
			return operationType;
		}

		@Override
		public int hashCode() {
			int result = asyncJobId.hashCode();
			result = 31 * result + operationType.hashCode();
			return result;
		}

	}


}
