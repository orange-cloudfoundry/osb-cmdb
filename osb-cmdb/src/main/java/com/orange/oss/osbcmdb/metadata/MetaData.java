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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import org.springframework.util.CollectionUtils;

import static org.springframework.util.Assert.notNull;

public class MetaData {

	private Map annotations;

	private Map<String, String> labels;

	private MetaData() {
	}

	public MetaData(Map<String, String> annotations,
		Map<String, String> labels) {
		this.annotations = annotations;
		this.labels = labels;
	}


	public Map getAnnotations() { return annotations; }

	public void setAnnotations(Map annotations) { this.annotations = annotations; }

	public Map<String, String> getLabels() { return labels; }

	public void setLabels(Map<String, String> labels) { this.labels = labels; }

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private final Map annotations = new HashMap<>();

		private final Map<String, String> labels = new HashMap<>();

		private Builder() {
		}

		public Builder from(MetaData metaData) {
			notNull(metaData, "metaData required");
			annotations(metaData.getAnnotations());
			labels(metaData.getLabels());
			return this;
		}

		public Builder annotations(Map annotations) {
			if (!CollectionUtils.isEmpty(annotations)) {
				this.annotations.putAll(annotations);
			}
			return this;
		}

		public Builder labels(Map<String, String> labels) {
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
