package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class StorageInUseException extends ApiException {

    public StorageInUseException(String message) {
        super(HttpStatus.CONFLICT, "STORAGE_IN_USE", message);
    }
}
