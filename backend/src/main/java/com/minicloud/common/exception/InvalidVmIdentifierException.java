package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidVmIdentifierException extends ApiException {

    public InvalidVmIdentifierException(String name) {
        super(HttpStatus.BAD_REQUEST, "INVALID_VM_ID", "Invalid VM identifier: " + name);
    }
}
