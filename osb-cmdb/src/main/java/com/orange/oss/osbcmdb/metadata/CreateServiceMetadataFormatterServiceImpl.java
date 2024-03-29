/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateServiceMetadataFormatterServiceImpl extends AbstractMetadataFormatterService
	implements CreateServiceMetadataFormatterService {


	public CreateServiceMetadataFormatterServiceImpl(K8SMetadataFormatter k8SMetadataFormatter,
		CfMetadataFormatter cfMetadataFormatter) {
		super(k8SMetadataFormatter, cfMetadataFormatter);
	}

	@Override
	public MetaData formatAsMetadata(CreateServiceInstanceRequest request) {
		return setMetadata(request, request.getServiceInstanceId(),
			request.getContext());
	}

}
