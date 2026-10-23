package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class OvsInUseException extends ApiException {

    public OvsInUseException(String message) {
        super(HttpStatus.CONFLICT, "OVS_IN_USE", message);
    }
}
