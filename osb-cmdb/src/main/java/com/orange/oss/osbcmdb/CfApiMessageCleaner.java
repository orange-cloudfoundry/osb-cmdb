package com.orange.oss.osbcmdb;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

public class CfApiMessageCleaner {

	protected final Logger LOG = Loggers.getLogger(CfApiMessageCleaner.class);


	public ServiceBrokerException redactExceptionAndWrapAsServiceBrokerException(Exception originalException) {
		String message = originalException.getMessage();
		message = redactExceptionMessage(message);
		return new ServiceBrokerException(message, originalException);
	}

	public String redactExceptionMessage(String message) {
		//Inspired from https://stackoverflow.com/a/163398/1484823
		String regex = "(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		String redactedMessage = message.replaceAll(regex, "redacted-url");
		LOG.debug("Redacted exception {} with message redacted into {}", message, redactedMessage);
		return redactedMessage;
	}

}
