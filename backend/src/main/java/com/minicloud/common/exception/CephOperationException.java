package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class CephOperationException extends ApiException {

    public CephOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "CEPH_OPERATION_FAILED", message);
    }
}
