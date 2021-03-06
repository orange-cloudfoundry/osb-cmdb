package com.orange.oss.osbcmdb.testfixtures;

import java.util.Map;

import com.orange.oss.osbcmdb.serviceinstance.ServiceInstanceInterceptor;
import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;


/**
 * Base class to accept Backing services requested in backing space. Only accept OSB calls when space is a
 * backing space, i.e. not the default space.
 *
 * Default implementation is to succeed to all requests
 */
public class BaseServiceInstanceBackingSpaceInstanceInterceptor extends BaseBackingSpaceInstanceInterceptor implements ServiceInstanceInterceptor {

	public static final String DASHBOARD_URL = "https://my-dasboard-domain.org";

	public BaseServiceInstanceBackingSpaceInstanceInterceptor(
		String defaultSpaceName) {
		super(defaultSpaceName);
	}

	@Override
	public boolean accept(CreateServiceInstanceRequest request) {
		return isScabAcceptanceTest(request.getContext(), request.toString());
	}

	@Override
	public boolean accept(GetLastServiceOperationRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString());
	}

	@Override
	public boolean accept(GetServiceInstanceRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString());
	}

	@Override
	public boolean accept(DeleteServiceInstanceRequest request) {
		return isServiceGuidPreviousProvisionnedByUs(request.getServiceInstanceId(), request.toString());
	}

	@Override
	public boolean accept(UpdateServiceInstanceRequest request) {
		return isScabAcceptanceTest(request.getContext(), request.toString());
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		provisionnedInstanceParams.put(request.getServiceInstanceId(), request.getParameters());
		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(false)
			.dashboardUrl(DASHBOARD_URL)
			.build());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(false)
			.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return Mono
			.error(new UnsupportedOperationException("This service broker does not support getting the status of " +
				"an asynchronous operation. " +
				"If the service broker returns '202 Accepted' in response to a provision, update, or deprovision" +
				"request, it must also provide an implementation of the get last operation API."));
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		GetServiceInstanceResponse.GetServiceInstanceResponseBuilder builder = GetServiceInstanceResponse.builder();

		Map<String, Object> parameters = provisionnedInstanceParams.get(request.getServiceInstanceId());
		if (parameters != null) {
			builder.parameters(parameters);
		}
		return Mono.just(builder
			.dashboardUrl(DASHBOARD_URL)
			//	wait until sc-osb support for request hints, see https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/287
			//			.planId()
			//			.serviceDefinitionId()
			.build());
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		provisionnedInstanceGuids.add(request.getServiceInstanceId());
		provisionnedInstanceParams.put(request.getServiceInstanceId(), request.getParameters());
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(false)
			.dashboardUrl(DASHBOARD_URL)
			.build());
	}

}
