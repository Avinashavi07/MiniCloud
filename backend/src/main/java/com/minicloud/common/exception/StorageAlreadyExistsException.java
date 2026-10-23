package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class StorageAlreadyExistsException extends ApiException {

    public StorageAlreadyExistsException(String kind, String name) {
        super(HttpStatus.CONFLICT, "STORAGE_ALREADY_EXISTS", "libvirt already has a " + kind + " named '" + name + "'");
    }
}
