package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class NetworkInUseException extends ApiException {

    public NetworkInUseException(String message) {
        super(HttpStatus.CONFLICT, "NETWORK_IN_USE", message);
    }
}
