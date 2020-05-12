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

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class OsbCmdbServiceBinding extends AbstractOsbCmdbService implements ServiceInstanceBindingService {

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private final Logger log = Loggers.getLogger(OsbCmdbServiceBinding.class);

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
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
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
			LOG.info("Unable to update bind service, caught:" + originalException, originalException);
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
				LOG.info("Service binding guid {} already exists and is backed by service key: {}, returning 201",
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
			throw new ServiceBrokerException(originalException.getMessage(), originalException);
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
			LOG.info("No such service instance id={} to delete binding from, return early.",
				request.getServiceInstanceId());
			return Mono.just(DeleteServiceInstanceBindingResponse.builder()
				.build());
		}

		//For now CF api V2 & V3 do not support async service bindings
		try {
			spacedTargetedOperations.services()
				.deleteServiceKey(org.cloudfoundry.operations.services.DeleteServiceKeyRequest.builder()
					.serviceInstanceName(existingSi.getName())
					.serviceKeyName(request.getBindingId())
					.build())
				.block();
		}
		catch (Exception e) {
			LOG.info("Unable to delete service key {} from service instance {} Got {}", request.getBindingId(),
				existingSi.getName(), e.toString());
			throw new ServiceBrokerException(e.toString());
		}

		//For now CF api V2 & V3 do not support async service bindings
		return Mono.just(DeleteServiceInstanceBindingResponse.builder()
			.build());
	}

	private void validateServiceDefinitionAndPlanIds(ServiceDefinition serviceDefinition, Plan plan,
		String serviceDefinitionId,
		String planId) {
		if (plan == null) {
			LOG.info("Invalid plan received with unknown id {}", planId);
			throw new ServiceBrokerInvalidParametersException("Invalid plan received with unknown id:" + planId);
		}
		if (serviceDefinition == null) {
			LOG.info("Invalid service definition received with unknown id {}", serviceDefinitionId);
			throw new ServiceBrokerInvalidParametersException(
				"Invalid service definition received with unknown id:" + serviceDefinitionId);
		}
	}

}
