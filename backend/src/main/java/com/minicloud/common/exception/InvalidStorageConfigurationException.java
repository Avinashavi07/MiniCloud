package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStorageConfigurationException extends ApiException {

    public InvalidStorageConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_STORAGE", message);
    }
}
