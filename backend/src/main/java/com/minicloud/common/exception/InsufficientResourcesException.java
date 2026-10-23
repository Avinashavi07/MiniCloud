package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientResourcesException extends ApiException {

    public InsufficientResourcesException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_RESOURCES", message);
    }
}
