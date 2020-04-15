package com.orange.oss.osbcmdb;

import java.time.Duration;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstance;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;

public class OsbCmdbServiceBinding extends AbstractOsbCmdbService implements ServiceInstanceBindingService {

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private ServiceBindingInterceptor osbInterceptor;


	private final Logger log = Loggers.getLogger(OsbCmdbServiceBinding.class);

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


		//Lookup corresponding service instance in the backend org to validate incoming request against security
		// attacks passing forged service instance guid

		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		if (existingSi == null) {
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}

//		spacedTargetedOperations.services().createServiceKey(org.cloudfoundry.operations.services.CreateServiceKeyRequest.builder()
//			.serviceInstanceName(existingSi.getName())
//			.serviceKeyName(request.getBindingId())
//			.build())
//			.block();

		//Directly use the v2 api to avoid a second API call to fetch the service key credentials
		CreateServiceKeyResponse createServiceKeyResponse = client.serviceKeys().create(CreateServiceKeyRequest.builder()
			.serviceInstanceId(existingSi.getId())
			.parameters(request.getParameters())
			.name(request.getBindingId())
			.build())
			.block();

		//For now CF api V2 & V3 do not support async service bindings
		return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
			.credentials(createServiceKeyResponse.getEntity().getCredentials())
			.async(false)
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		if (osbInterceptor != null && osbInterceptor.accept(request)) {
			return osbInterceptor.deleteServiceInstanceBinding(request);
		}


		//Lookup corresponding service instance to validate incoming request against security attacks passing
		// forged service instance guid
		CloudFoundryOperations spacedTargetedOperations = getSpaceScopedOperations(request.getServiceDefinition().getName());
		ServiceInstance existingSi = getCfServiceInstance(spacedTargetedOperations, request.getServiceInstanceId());

		if (existingSi == null) {
			LOG.info("No such service instance id={} to delete binding from, return early.",
				request.getServiceInstanceId());
			return Mono.just(DeleteServiceInstanceBindingResponse.builder()
				.build());
		}

		//For now CF api V2 & V3 do not support async service bindings
		try {
			spacedTargetedOperations.services().deleteServiceKey(org.cloudfoundry.operations.services.DeleteServiceKeyRequest.builder()
				.serviceInstanceName(existingSi.getName())
				.serviceKeyName(request.getBindingId())
				.build())
				.block();
		}
		catch (Exception e) {
			LOG.info("Unable to delete service key {} from service instance {} Got {}", request.getBindingId(),
				existingSi.getName(), e.toString());
		}

		//For now CF api V2 & V3 do not support async service bindings
		return Mono.just(DeleteServiceInstanceBindingResponse.builder()
			.build());
	}

}
