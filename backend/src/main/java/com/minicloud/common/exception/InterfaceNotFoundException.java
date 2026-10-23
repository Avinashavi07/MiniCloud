package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InterfaceNotFoundException extends ApiException {

    public InterfaceNotFoundException(String vmName, String mac) {
        super(HttpStatus.NOT_FOUND, "INTERFACE_NOT_FOUND",
                "libvirt domain '" + vmName + "' has no interface with MAC " + mac);
    }
}
