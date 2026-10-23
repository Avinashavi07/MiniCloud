package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class VmAlreadyExistsException extends ApiException {

    public VmAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "VM_ALREADY_EXISTS", "libvirt already has a domain named '" + name + "'");
    }
}
