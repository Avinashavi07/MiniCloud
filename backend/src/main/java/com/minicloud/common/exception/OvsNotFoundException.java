package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class OvsNotFoundException extends ApiException {

    public OvsNotFoundException(String name) {
        super(HttpStatus.NOT_FOUND, "OVS_NOT_FOUND", "Open vSwitch has no bridge named '" + name + "'");
    }
}
