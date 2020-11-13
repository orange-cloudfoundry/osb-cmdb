package com.orange.oss.osbcmdb.metadata;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaDataTest {

	@Test
	@DisplayName("serializes as json to be returned as a param object")
	void serializes_as_json() throws JsonProcessingException {
		MetaData metaData = new MetaData(Collections.singletonMap("annotation-key", "annotation-value"),
			Collections.singletonMap("metadata-key", "metadata-value"));
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(metaData);
		assertThat(json).isEqualTo("{\"annotations\":{\"annotation-key\":\"annotation-value\"},\"labels\":{\"metadata-key\":\"metadata-value\"}}");
	}

	@Test
	void builder_provides_copy_constructor() {
		//given
		MetaData original = new MetaData(Collections.singletonMap("annotation-key", "annotation-value"),
			Collections.singletonMap("metadata-key", "metadata-value"));

		//when
		MetaData clone = MetaData.builder().from(original).build();

		//then
		assertThat(clone).isEqualTo(original);
	}

}