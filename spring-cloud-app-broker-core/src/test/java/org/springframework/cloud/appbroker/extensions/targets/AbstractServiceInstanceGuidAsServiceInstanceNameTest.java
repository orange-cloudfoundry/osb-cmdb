package org.springframework.cloud.appbroker.extensions.targets;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractServiceInstanceGuidAsServiceInstanceNameTest {

	protected TargetFactory strategy;

	@Test
	void serviceInstanceNameUseServiceInstanceGuid() {
		//more than 13 chars
		ArtifactDetails artifactDetails = generateArtifact("Any-service-name-Does-not-get-used", "99643ba8-8805-4a2b-a059-c62bc1ea5cf1");
		assertThat(artifactDetails.getName()).isEqualTo("99643ba8-8805-4a2b-a059-c62bc1ea5cf1");
	}

	@Test
	void serviceInstanceNamesAreShorterThanCfLimit() {
		assertServiceInstanceNameIsSmallerThanCfLimit("Any-service-name-Does-not-get-used",   //more than 13 chars
			"0123456789"+ "0123456789"+ "0123456789"+ "0123456789"+"0123456789"+"0123456789"+"0123456789"+"0123456789"
		);
		assertServiceInstanceNameIsSmallerThanCfLimit("p-mysql",
			"99643ba8-8805-4a2b-a059-c62bc1ea5cf1" //36 chars
		);
		assertServiceInstanceNameIsSmallerThanCfLimit("p-mysql",
			"33ffdf64-784d-4ecb-8e70-17d54b2a73" //34 chars
		);
	}

	private void assertServiceInstanceNameIsSmallerThanCfLimit(String serviceOfferingName, String serviceInstanceId) {
		ArtifactDetails artifactDetails = generateArtifact(serviceOfferingName, serviceInstanceId);
		assertThat(artifactDetails.getName()).hasSizeLessThanOrEqualTo(50);
	}

	protected ArtifactDetails generateArtifact(String serviceOfferingName, String serviceInstanceId) {
		Target target = strategy.create(null);
		return target
			.apply(new HashMap<>(), serviceOfferingName, serviceInstanceId, serviceOfferingName, "10mb");
	}

}
