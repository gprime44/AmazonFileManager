package org.optimus.amazon.backup.server.exception;

import org.slf4j.helpers.MessageFormatter;

public class ServiceException extends Exception {

	private static final long serialVersionUID = 2457783048497103025L;

	public ServiceException(String message, Exception e, Object... params) {
		super(MessageFormatter.arrayFormat(message, params).getMessage(), e);
	}

	public ServiceException(String message, Object... params) {
		super(MessageFormatter.arrayFormat(message, params).getMessage());
	}
}
