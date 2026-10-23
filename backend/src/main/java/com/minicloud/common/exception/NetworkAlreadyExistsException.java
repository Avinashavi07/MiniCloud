package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class NetworkAlreadyExistsException extends ApiException {

    public NetworkAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "NETWORK_ALREADY_EXISTS", "libvirt already has a network named '" + name + "'");
    }
}
