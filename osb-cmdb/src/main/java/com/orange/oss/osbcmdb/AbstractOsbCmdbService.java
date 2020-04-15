package com.orange.oss.osbcmdb;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.useradmin.SetSpaceRoleRequest;
import org.cloudfoundry.operations.useradmin.SpaceRole;
import org.cloudfoundry.util.PaginationUtils;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class AbstractOsbCmdbService {

	protected final Logger LOG = Loggers.getLogger(AbstractOsbCmdbService.class);

	protected final CloudFoundryClient client;

	protected final String defaultOrg;

	protected final CloudFoundryOperations operations;

	protected final String userName;

	public AbstractOsbCmdbService(
		CloudFoundryClient cloudFoundryClient, String defaultOrg, String userName,
		CloudFoundryOperations cloudFoundryOperations) {
		client = cloudFoundryClient;
		this.defaultOrg = defaultOrg;
		this.userName = userName;
		operations = cloudFoundryOperations;
	}

	protected ServiceInstance getCfServiceInstance(CloudFoundryOperations spacedTargetedOperations,
		String serviceInstanceName) {
		return spacedTargetedOperations.services()
			.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
				.name(serviceInstanceName).build()).block();
	}

	protected CloudFoundryOperations getSpaceScopedOperations(String spaceName) {
		createSpace(spaceName).block();

		//We expect the builder to reuse most of Cf Java client internal objects and thus be lightweigth
		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.operations)
			.space(spaceName)
			.build();
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

	private Mono<String> getOrganizationId(String orgName) {
		return operations.organizations().get(OrganizationInfoRequest.builder()
			.name(orgName)
			.build())
			.map(OrganizationDetail::getId);
	}

	protected Mono<String> getSpaceId(String spaceName) {
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


}
