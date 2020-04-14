package com.orange.oss.osbcmdb;

import java.time.Duration;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyResponse;
import org.cloudfoundry.client.v2.servicekeys.GetServiceKeyRequest;
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
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;

public class OsbCmdbServiceBinding implements ServiceInstanceBindingService {

	private CloudFoundryClient client;

	public static final Duration SYNC_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

	private final Logger log = Loggers.getLogger(OsbCmdbServiceBinding.class);

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {

		//Lookup corresponding service instance to validate incoming request against security attacks passing
		// forged service instance guid
		GetServiceInstanceResponse serviceInstanceResponse = client.serviceInstances().get(GetServiceInstanceRequest.builder()
			.serviceInstanceId(request.getServiceInstanceId())
			.build())
			.block();

		ServiceInstanceEntity existingSi = serviceInstanceResponse.getEntity();
		if (existingSi == null) {
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}
		CloudFoundryOperations spaceTargettedOperations;
		existingSi.

		spaceTargettedOperations.services().createServiceKey(org.cloudfoundry.operations.services.CreateServiceKeyRequest.builder()
			.serviceInstanceName(existingSi.getName())
			.serviceKeyName(request.getBindingId())
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

		//Lookup corresponding service instance to validate incoming request against security attacks passing
		// forged service instance guid
		GetServiceInstanceResponse serviceInstanceResponse = client.serviceInstances().get(GetServiceInstanceRequest.builder()
			.serviceInstanceId(request.getServiceInstanceId())
			.build())
			.block();

		ServiceInstanceEntity existingSi = serviceInstanceResponse.getEntity();
		if (existingSi == null) {
			throw new ServiceInstanceDoesNotExistException(request.getServiceInstanceId());
		}
		CloudFoundryOperations spaceTargettedOperations;

		spaceTargettedOperations.services().deleteServiceKey(org.cloudfoundry.operations.services.CreateServiceKeyRequest.builder()
			.serviceInstanceName(existingSi.getName())
			.serviceKeyName(request.getBindingId())
			.build())
			.block();

		//For now CF api V2 & V3 do not support async service bindings
		return Mono.just(DeleteServiceInstanceBindingResponse.builder()
			.async(false)
			.build());

	}

}
