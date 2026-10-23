package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidNetworkConfigurationException extends ApiException {

    public InvalidNetworkConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_NETWORK", message);
    }
}
