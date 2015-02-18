package com.webservice.client.exceptions;

public class WebserviceClientException extends Exception {

	private static final long serialVersionUID = 1L;

	String message;

	public WebserviceClientException(Throwable exceptionObject) {
		super(exceptionObject.getMessage());
		this.message = exceptionObject.getMessage();
	}

	public WebserviceClientException(String msg) {
		super(msg);
		this.message = msg;
	}

	@SuppressWarnings("unchecked")
	public Throwable getRootCause(Class causeType, boolean exact) {
		Throwable cause = getCause();
		while (cause != null) {
			if (exact) {
				if (cause.getClass() == causeType) {
					return cause;
				}
			} else if (causeType.isAssignableFrom(cause.getClass())) {
				return cause;
			}
			cause = cause.getCause();
		}
		return null;
	}
}
