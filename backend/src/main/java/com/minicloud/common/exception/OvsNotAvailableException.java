package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class OvsNotAvailableException extends ApiException {

    public OvsNotAvailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "OVS_NOT_AVAILABLE", message);
    }
}
