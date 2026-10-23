package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class VmNotFoundException extends ApiException {

    public VmNotFoundException(String name) {
        super(HttpStatus.NOT_FOUND, "VM_NOT_FOUND", "libvirt has no domain named '" + name + "'");
    }
}
