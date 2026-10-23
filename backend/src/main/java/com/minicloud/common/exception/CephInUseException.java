package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class CephInUseException extends ApiException {

    public CephInUseException(String message) {
        super(HttpStatus.CONFLICT, "CEPH_IN_USE", message);
    }
}
