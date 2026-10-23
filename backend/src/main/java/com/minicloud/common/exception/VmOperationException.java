package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class VmOperationException extends ApiException {

    public VmOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "VM_OPERATION_FAILED", message);
    }
}
