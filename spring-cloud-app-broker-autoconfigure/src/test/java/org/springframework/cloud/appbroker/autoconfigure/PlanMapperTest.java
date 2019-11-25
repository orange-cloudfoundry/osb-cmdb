package org.springframework.cloud.appbroker.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.servicebroker.model.catalog.Plan;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class PlanMapperTest {

	@Test
	void mapsPlanResourcesIntoPlan() {
		List<ServicePlanResource> servicePlans = asList(
		ServicePlanResource.builder()
			.entity(ServicePlanEntity.builder()
				.name("plan1")
				.active(true)
				.bindable(true)
				.description("description")
				.extra("{\"displayName\":\"Big Bunny\"}")
				.free(false)
				.build())
			.build(),
		ServicePlanResource.builder()
			.entity(ServicePlanEntity.builder()
				.name("plan2")
				.build())
			.build());


		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		List<Plan> plans = planMapper.toPlans(servicePlans);
		assertThat(plans).hasSize(2);
		assertThat(plans.get(0).getName()).isEqualTo("plan1");
		assertThat(plans.get(1).getName()).isEqualTo("plan2");
	}

	@Test
	void mapsPlanExtraToMetadata() {
		List<ServicePlanResource> servicePlans = Collections.singletonList(
			ServicePlanResource.builder()
				.entity(ServicePlanEntity.builder()
					.name("plan1")
					.active(true)
					.bindable(true)
					.description("description")
					.extra("{\"displayName\":\"Big Bunny\"}")
					.free(false)
					.build())
				.build());


		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		List<Plan> plans = planMapper.toPlans(servicePlans);
		assertThat(plans).hasSize(1);
		Plan plan = plans.get(0);
		assertThat(plan.getMetadata()).hasSize(1);
		assertThat(plan.getMetadata().get("displayName")).isEqualTo("Big Bunny");
	}

	@Test
	void parsesJsonIntoPlan() {
		String json = "{\"active\":true,\"bindable\":true,\"description\":\"This is a default cassandra plan.  All " +
			"services are created equally.\",\"extra\":\"{\\n  \\\"bullets\\\": [\\n    \\\"100 MB Storage (not enforced)\\\",\\n    \\\"40 concurrent connections (not enforced)\\\"\\n  ],\\n  \\\"costs\\\": {\\n    \\\"amount\\\": {\\n      \\\"eur\\\": 10.0\\n    },\\n    \\\"unit\\\": \\\"MONTHLY\\\"\\n  },\\n  \\\"displayName\\\": \\\"Default - Shared cassandra server\\\"\\n}\",\"free\":false,\"name\":\"default\",\"public\":true,\"schemas\":{\"service_binding\":{\"create\":{\"parameters\":{\"$schema\":null,\"type\":null,\"properties\":null}}},\"service_instance\":{\"create\":{\"parameters\":{\"$schema\":null,\"type\":null,\"properties\":null}},\"update\":{\"parameters\":{\"$schema\":null,\"type\":null,\"properties\":null}}}},\"service_guid\":\"7b1bccbe-5435-4853-b753-1b4380e8d989\",\"service_instances_url\":\"/v2/service_plans/04b28e61-c6d1-42d0-a7fc-942396802aa3/service_instances\",\"service_url\":\"/v2/services/7b1bccbe-5435-4853-b753-1b4380e8d989\",\"unique_id\":\"cassandra-plan\"}";
		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());
		assertThat(planMapper.fromJson(json, Plan.class)).isNotNull();
	}

	@Test
	void parsedJsonIntoPlanConvertingExtra() {
		String json = "{\"active\":true,\"bindable\":true,\"description\":\"basic\",\"extra\":\"{\\n\\t\\t\\t\\t\\t\\t\\\"bullets\\\":[\\n\\t\\t\\t\\t\\t\\t\\t\\\"20 GB of messages\\\",\\n\\t\\t\\t\\t\\t\\t\\t\\\"20 connections\\\"\\n\\t\\t\\t\\t\\t\\t],\\n\\t\\t\\t\\t\\t\\t\\\"costs\\\":[\\n\\t\\t\\t\\t\\t\\t\\t{\\n\\t\\t\\t\\t\\t\\t\\t\\t\\\"amount\\\":{\\n\\t\\t\\t\\t\\t\\t\\t\\t\\t\\\"usd\\\":99.0\\n\\t\\t\\t\\t\\t\\t\\t\\t},\\n\\t\\t\\t\\t\\t\\t\\t\\t\\\"unit\\\":\\\"MONTHLY\\\"\\n\\t\\t\\t\\t\\t\\t\\t},\\n\\t\\t\\t\\t\\t\\t\\t{\\n\\t\\t\\t\\t\\t\\t\\t\\t\\\"amount\\\":{\\n\\t\\t\\t\\t\\t\\t\\t\\t\\t\\\"usd\\\":0.99\\n\\t\\t\\t\\t\\t\\t\\t\\t},\\n\\t\\t\\t\\t\\t\\t\\t\\t\\\"unit\\\":\\\"1GB of messages over 20GB\\\"\\n\\t\\t\\t\\t\\t\\t\\t}\\n\\t\\t\\t\\t\\t\\t],\\n\\t\\t\\t\\t\\t\\t\\\"displayName\\\":\\\"Big Bunny\\\"\\n\\t\\t\\t\\t\\t}\\n\",\"free\":true,\"name\":\"standard\",\"public\":true,\"schemas\":{\"service_binding\":{\"create\":{\"parameters\":{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"baz\":{\"type\":\"object\",\"properties\":{\"foo\":{\"type\":\"string\"},\"bar\":{\"type\":\"string\"}},\"allOf\":[{\"required\":[\"foo\"]},{\"required\":[\"bar\"]}]}}}}},\"service_instance\":{\"create\":{\"parameters\":{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"baz\":{\"type\":\"object\",\"properties\":{\"foo\":{\"type\":\"string\"},\"bar\":{\"type\":\"string\"}},\"allOf\":[{\"required\":[\"foo\"]},{\"required\":[\"bar\"]}]}}}},\"update\":{\"parameters\":{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"baz\":{\"type\":\"object\",\"properties\":{\"foo\":{\"type\":\"string\"},\"bar\":{\"type\":\"string\"}},\"allOf\":[{\"required\":[\"foo\"]},{\"required\":[\"bar\"]}]}}}}}},\"service_guid\":\"SERVICE-ID\",\"service_instances_url\":\"/v2/service_plans/SERVICE-PLAN-ID/service_instances\",\"service_url\":\"/v2/services/SERVICE-ID\",\"unique_id\":null}";
		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());
		assertThat(planMapper.fromJson(json, Plan.class)).isNotNull();
	}

}