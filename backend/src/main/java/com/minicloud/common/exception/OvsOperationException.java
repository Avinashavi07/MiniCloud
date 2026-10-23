package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class OvsOperationException extends ApiException {

    public OvsOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "OVS_OPERATION_FAILED", message);
    }
}
