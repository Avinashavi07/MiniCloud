package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class CephAlreadyExistsException extends ApiException {

    public CephAlreadyExistsException(String kind, String name) {
        super(HttpStatus.CONFLICT, "CEPH_ALREADY_EXISTS", "Ceph already has " + kind + " '" + name + "'");
    }
}
