package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class HostNotFoundException extends ApiException {

    public HostNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "HOST_NOT_FOUND", "Host not found: " + id);
    }
}
