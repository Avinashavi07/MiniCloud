package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class NetworkNotFoundException extends ApiException {

    public NetworkNotFoundException(String name) {
        super(HttpStatus.NOT_FOUND, "NETWORK_NOT_FOUND", "libvirt has no network named '" + name + "'");
    }
}
