package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class StorageOperationException extends ApiException {

    public StorageOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "STORAGE_OPERATION_FAILED", message);
    }
}
