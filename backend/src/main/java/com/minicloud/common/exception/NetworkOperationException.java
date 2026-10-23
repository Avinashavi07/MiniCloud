package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class NetworkOperationException extends ApiException {

    public NetworkOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "NETWORK_OPERATION_FAILED", message);
    }
}
