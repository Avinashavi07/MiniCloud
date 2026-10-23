package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOvsConfigurationException extends ApiException {

    public InvalidOvsConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_OVS_CONFIGURATION", message);
    }
}
