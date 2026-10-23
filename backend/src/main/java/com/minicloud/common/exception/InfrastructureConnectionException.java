package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InfrastructureConnectionException extends ApiException {

    public InfrastructureConnectionException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "INFRASTRUCTURE_CONNECTION", message);
    }
}
