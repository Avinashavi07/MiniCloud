package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class CephNotFoundException extends ApiException {

    public CephNotFoundException(String kind, String name) {
        super(HttpStatus.NOT_FOUND, "CEPH_NOT_FOUND", "Ceph has no " + kind + " named '" + name + "'");
    }
}
