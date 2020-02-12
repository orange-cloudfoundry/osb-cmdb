package org.springframework.cloud.appbroker.extensions.targets;

public class ServiceInstanceNameHelper {

	// See https://github.com/cloudfoundry/cloud_controller_ng/blob/757a9d4d6f41b57ccba7b2007de2d51fbf1a4385/vendor/errors/v2.yml#L231-L235
	// > You have requested an invalid service instance name. Names are limited to 50 characters
	public static final int MAX_CF_SERVICE_INSTANCE_NAME_LENGTH = 50;

	public static String truncateNameToCfMaxSize(String name) {
		return name.substring(0, Math.min(name.length(), MAX_CF_SERVICE_INSTANCE_NAME_LENGTH));
	}

}
