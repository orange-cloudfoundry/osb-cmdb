package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BackingAppDeploymentServiceTest {

	private static final String STATUS_RUNNING = "running";
	private static final String STATUS_DELETED = "deleted";

	@Mock
	private DeployerClient deployerClient;

	private BackingAppDeploymentService backingAppDeploymentService;
	private BackingApplications backingApps;

	@BeforeEach
	void setUp() {
		backingAppDeploymentService = new BackingAppDeploymentService(deployerClient);
		backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("testApp1")
				.path("http://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication.builder()
				.name("testApp2")
				.path("http://myfiles/app2.jar")
				.build())
			.build();
	}

	@Test
	void shouldDeployApplications() {
		doReturn(Mono.just(STATUS_RUNNING))
			.when(deployerClient).deploy(backingApps.get(0));
		doReturn(Mono.just(STATUS_RUNNING))
			.when(deployerClient).deploy(backingApps.get(1));

		StepVerifier.create(backingAppDeploymentService.deploy(backingApps))
			.expectNext(STATUS_RUNNING + "," + STATUS_RUNNING)
			.verifyComplete();
	}

	@Test
	void shouldUndeployApplications() {
		doReturn(Mono.just(STATUS_DELETED))
			.when(deployerClient).undeploy(backingApps.get(0));
		doReturn(Mono.just(STATUS_DELETED))
			.when(deployerClient).undeploy(backingApps.get(1));

		StepVerifier.create(backingAppDeploymentService.undeploy(backingApps))
			.expectNext(STATUS_DELETED + "," + STATUS_DELETED)
			.verifyComplete();
	}
}