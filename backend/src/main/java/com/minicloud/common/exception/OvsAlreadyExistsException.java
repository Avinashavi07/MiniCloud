package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class OvsAlreadyExistsException extends ApiException {

    public OvsAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "OVS_ALREADY_EXISTS", "Open vSwitch already has bridge '" + name + "'");
    }
}
