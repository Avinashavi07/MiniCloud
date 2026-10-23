package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidLibvirtNameException extends ApiException {

    public InvalidLibvirtNameException(String kind, String name) {
        super(HttpStatus.BAD_REQUEST, "INVALID_LIBVIRT_NAME", "Invalid " + kind + " name: " + name);
    }
}
