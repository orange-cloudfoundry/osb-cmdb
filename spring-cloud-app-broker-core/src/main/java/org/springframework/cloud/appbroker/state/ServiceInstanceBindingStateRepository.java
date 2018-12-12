/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.state;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * @author Roy Clarkson
 */
public interface ServiceInstanceBindingStateRepository {

	default Mono<ServiceInstanceState> saveState(String serviceInstanceId, String bindingId, OperationState state, String description) {
		return Mono.empty();
	}

	default Mono<ServiceInstanceState> getState(String serviceInstanceId, String bindingId) {
		return Mono.empty();
	}

	default Mono<ServiceInstanceState> removeState(String serviceInstanceId, String bindingId) {
		return Mono.empty();
	}

}