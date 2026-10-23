package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCephConfigurationException extends ApiException {

    public InvalidCephConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CEPH_CONFIGURATION", message);
    }
}
