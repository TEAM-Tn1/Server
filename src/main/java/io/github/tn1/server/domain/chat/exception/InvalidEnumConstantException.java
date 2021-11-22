package io.github.tn1.server.domain.chat.exception;

import io.github.tn1.server.global.error.exception.ErrorCode;
import io.github.tn1.server.global.error.exception.ServerException;

public class InvalidEnumConstantException extends ServerException {

	public InvalidEnumConstantException() {
		super(ErrorCode.INVALID_ENUM_CONSTANT);
	}

}
