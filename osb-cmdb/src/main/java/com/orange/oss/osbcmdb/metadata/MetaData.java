/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orange.oss.osbcmdb.metadata;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.util.CollectionUtils;

public class MetaData {

	private Map<String, String> annotations;

	private Map<String, String> labels;

	private MetaData() {
	}

	public MetaData(Map<String, String> annotations,
		Map<String, String> labels) {
		this.annotations = annotations;
		this.labels = labels;
	}


	public Map<String, String> getAnnotations() { return annotations; }

	public void setAnnotations(Map<String, String> annotations) { this.annotations = annotations; }

	public Map<String, String> getLabels() { return labels; }

	public void setLabels(Map<String, String> labels) { this.labels = labels; }

	public static BackingServiceBuilder builder() {
		return new BackingServiceBuilder();
	}

	public static final class BackingServiceBuilder {

		private final Map<String, String> annotations = new HashMap<>();

		private final Map<String, String> labels = new HashMap<>();

		private BackingServiceBuilder() {
		}

		public BackingServiceBuilder annotations(Map<String, String> annotations) {
			if (!CollectionUtils.isEmpty(annotations)) {
				this.annotations.putAll(annotations);
			}
			return this;
		}

		public BackingServiceBuilder labels(Map<String, String> labels) {
			if (!CollectionUtils.isEmpty(labels)) {
				this.labels.putAll(labels);
			}
			return this;
		}

		public MetaData build() {
			return new MetaData(annotations, labels);
		}

	}

}
