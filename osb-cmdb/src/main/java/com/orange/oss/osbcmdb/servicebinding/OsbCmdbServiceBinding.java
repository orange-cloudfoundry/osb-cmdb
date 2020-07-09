package com.orange.oss.osbcmdb.servicebinding;

import java.time.Duration;

import com.orange.oss.osbcmdb.AbstractOsbCmdbService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceKey;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceBinding extends AbstractOsbCmdbService implements ServiceInstanceBindingService {

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private final Logger LOG = Loggers.getLogger(OsbCmdbServiceBinding.class);

	private ServiceBindingInterceptor osbInterceptor;

	public OsbCmdbServiceBinding(CloudFoundryClient cloudFoundryClient, String defaultOrg, String userName,
		CloudFoundryOperations cloudFoundryOperations, ServiceBindingInterceptor osbInterceptor) {
		super(cloudFoundryClient, defaultOrg, userName, cloudFoundryOperations);
		this.osbInterceptor = osbInterceptor;
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.createServiceInstanceBinding(request);
		}

		//Validate mandatory service id and plan Id (even if plan is currently unused)
		validateServiceDefinitionAndPlanIds(request.getServiceDefinition(), request.getPlan(),
			request.getServiceDefinitionId(), request.getPlanId());

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

		//Directly use the v2 api to avoid a second API call to fetch the service key credentials
		try {
			CreateServiceKeyResponse createServiceKeyResponse = client.serviceKeys()
				.create(CreateServiceKeyRequest.builder()
					.serviceInstanceId(existingSi.getId())
					.parameters(request.getParameters())
					.name(request.getBindingId())
					.build())
				.block();

			//For now CF api V2 & V3 do not support async service bindings
			assert createServiceKeyResponse != null;
			return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
				.credentials(createServiceKeyResponse.getEntity().getCredentials())
				.async(false)
				.build());
		}
		catch (Exception originalException) {
			//CF API errors can be multiple and can change without notification
			// To avoid relying on exceptions thrown to make decisions, we try to diagnose and recover the exception
			// globally by inspecting the backing service instance state instead.
			LOG.info("Unable to create service binding, caught:" + originalException,
				originalException);
			return handleBindException(request, spacedTargetedOperations, originalException);
		}
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.deleteServiceInstanceBinding(request);
		}

		//Validate mandatory service id and plan Id (even if plan is currently unused)
		validateServiceDefinitionAndPlanIds(request.getServiceDefinition(), request.getPlan(),
			request.getServiceDefinitionId(), request.getPlanId());

		//Lookup corresponding service instance to validate incoming request against security attacks passing
		// forged service instance guid
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(
			request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		if (existingSi == null) {
			LOG.warn("No such service instance id={} to delete binding from, client error or attempt to delete " +
					"binding from unauthorized service instance.",
				request.getServiceInstanceId());
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}

		//For now CF api V2 & V3 do not support async service bindings
		try {
			spacedTargetedOperations.services()
				.deleteServiceKey(org.cloudfoundry.operations.services.DeleteServiceKeyRequest.builder()
					.serviceInstanceName(existingSi.getName())
					.serviceKeyName(request.getBindingId())
					.build())
				.block();

			//For now CF api V2 & V3 do not support async service bindings
			return Mono.just(DeleteServiceInstanceBindingResponse.builder()
				.build());
		}
		catch (Exception e) {
			LOG.info("Unable to delete backing service key with name={} from backing service instance name={} Got {}",
				request.getBindingId(),
				existingSi.getName(), e.toString(), e);
			return handleUnbindException(request, spacedTargetedOperations, e);
		}

	}

	public Mono<CreateServiceInstanceBindingResponse> handleBindException(CreateServiceInstanceBindingRequest request,
		CloudFoundryOperations spacedTargetedOperations, Exception originalException) {
		LOG.info("Inspecting exception caught {} for possible concurrent dupl while handling request {} ",
			originalException, request);

		ServiceKey existingServiceKey = null;
		try {
			existingServiceKey = spacedTargetedOperations.services().getServiceKey(GetServiceKeyRequest.builder()
				.serviceInstanceName(request.getServiceInstanceId())
				.serviceKeyName(request.getBindingId())
				.build())
				.block();
		}
		catch (Exception exception) {
			LOG.info("Unable to lookup potential service key dup, caught {}", exception.toString());
		}
		if (existingServiceKey != null) {
			LOG.info("Service binding guid {} already exists and is backed by service key: {}, returning 200",
				request.getBindingId(), existingServiceKey);
			//In the future (with CAPI v3) compare params to return a 409 conflict in case of params mismatch
			//Would need cf-java-client to support fetching service key params, which it
			//does not yet do: it only return service key parameter url
			// See https://github.com/cloudfoundry/cf-java-client/blob/4ce8018050f69619cc9e1eb61a8a7f5a36e2d5c7/cloudfoundry-client/src/main/java/org/cloudfoundry/client/v2/servicekeys/_ServiceKeyEntity.java#L69
			return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
				.credentials(existingServiceKey.getCredentials())
				.bindingExisted(true)
				.build());
		}
		LOG.info("Unable to lookup potential service key dup, flowing up original exception {}",
			originalException.toString());
		throw redactExceptionAndWrapAsServiceBrokerException(originalException);
	}

	public Mono<DeleteServiceInstanceBindingResponse> handleUnbindException(DeleteServiceInstanceBindingRequest request,
		CloudFoundryOperations spacedTargetedOperations, Exception originalException) {
		LOG.info("Inspecting exception caught {} for possible concurrent dupl while handling request {} ",
			originalException, request);

		ServiceKey existingServiceKey = null;
		try {
			existingServiceKey = spacedTargetedOperations.services().getServiceKey(GetServiceKeyRequest.builder()
				.serviceInstanceName(request.getServiceInstanceId())
				.serviceKeyName(request.getBindingId())
				.build())
				.block();
		}
		catch (Exception exception) {
			LOG.info("Unable to lookup potential service key dup, caught {}", exception.toString());
		}
		if (existingServiceKey != null) {
			LOG.info("Service binding guid {} still exists and is backed by service key: {}, flowing up " +
					"original exception {}",
				request.getBindingId(), existingServiceKey, originalException);
			throw redactExceptionAndWrapAsServiceBrokerException(originalException);
		}
		LOG.info("Assuming duplicate concurrent unbind request, returning 410 GONE");
		throw new ServiceInstanceBindingDoesNotExistException(request.getBindingId());
	}

}
