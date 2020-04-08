package org.springframework.cloud.appbroker.deployer;

import reactor.core.publisher.Mono;

public interface ServiceDeployer {

	Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request);

	Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request);

	Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request);

	Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request);

}
