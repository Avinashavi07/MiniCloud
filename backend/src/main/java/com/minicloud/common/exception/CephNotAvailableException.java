package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class CephNotAvailableException extends ApiException {

    public CephNotAvailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "CEPH_NOT_AVAILABLE", message);
    }
}
