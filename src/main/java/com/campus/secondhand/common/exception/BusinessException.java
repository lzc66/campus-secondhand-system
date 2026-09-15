package com.campus.secondhand.common.exception;

import com.campus.secondhand.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(int code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getHttpStatus(), errorCode.getDefaultMessage());
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
