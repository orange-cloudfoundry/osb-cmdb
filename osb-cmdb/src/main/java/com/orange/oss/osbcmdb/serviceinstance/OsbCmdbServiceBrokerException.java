package com.orange.oss.osbcmdb.serviceinstance;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

/**
 * Used to qualify diagnosed exceptions thrown by OsbCmdb so that no attempt is made to recover from them (unlike
 * oxceptions due to concurrent requests received from svcat replicas)
 */
public class OsbCmdbServiceBrokerException extends ServiceBrokerException {


	public OsbCmdbServiceBrokerException(String message) {
		super(message);
	}


	public OsbCmdbServiceBrokerException(String message, Throwable cause) {
		super(message, cause);
	}

}
