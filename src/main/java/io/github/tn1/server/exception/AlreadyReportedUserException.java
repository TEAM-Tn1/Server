package io.github.tn1.server.exception;

import io.github.tn1.server.error.exception.ErrorCode;
import io.github.tn1.server.error.exception.ServerException;

public class AlreadyReportedUserException extends ServerException {

	public AlreadyReportedUserException() {
		super(ErrorCode.ALREADY_REPORTED_USER);
	}

}
