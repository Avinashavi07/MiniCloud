package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class StorageNotFoundException extends ApiException {

    public StorageNotFoundException(String kind, String name) {
        super(HttpStatus.NOT_FOUND, "STORAGE_NOT_FOUND", "libvirt has no " + kind + " named '" + name + "'");
    }
}
