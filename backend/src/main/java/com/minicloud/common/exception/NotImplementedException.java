package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class NotImplementedException extends ApiException {

    public NotImplementedException(String message) {
        super(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", message);
    }
}
