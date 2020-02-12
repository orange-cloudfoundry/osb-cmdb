package org.springframework.cloud.appbroker.extensions.targets;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.cloud.appbroker.deployer.DeploymentProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpacePerServicePlanTest extends AbstractServiceInstanceGuidAsServiceInstanceNameTest {

	@BeforeAll
	public void beforeEach() {
		strategy = new SpacePerServicePlan();
	}

	@Test
	void spaceNameIsServicePlan() {
		//more than 13 chars
		Target target = strategy.create(null);
		ArtifactDetails artifactDetails = target
			.apply(new HashMap<>(), "A-brokered-service-name", "a-service-guid",
				"p-mysql", "10mb");
		assertThat(artifactDetails.getProperties()).containsOnly(
			entry(DeploymentProperties.TARGET_PROPERTY_KEY, "p-mysql-10mb"),
			entry(DeploymentProperties.KEEP_TARGET_ON_DELETE_PROPERTY_KEY, "true")
			);
	}


}