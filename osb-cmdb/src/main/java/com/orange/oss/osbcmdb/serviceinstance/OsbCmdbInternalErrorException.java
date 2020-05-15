package com.orange.oss.osbcmdb.serviceinstance;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

/**
 * Used to qualify diagnosed exceptions thrown by OsbCmdb so that no attempt is made to recover from them
 */
public class OsbCmdbInternalErrorException extends ServiceBrokerException {


	public OsbCmdbInternalErrorException(String message) {
		super(message);
	}


	public OsbCmdbInternalErrorException(String message, Throwable cause) {
		super(message, cause);
	}

}
