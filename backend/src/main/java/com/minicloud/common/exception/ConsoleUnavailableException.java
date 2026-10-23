package com.minicloud.common.exception;

import org.springframework.http.HttpStatus;

public class ConsoleUnavailableException extends ApiException {

    public ConsoleUnavailableException(String message) {
        super(HttpStatus.CONFLICT, "CONSOLE_UNAVAILABLE", message);
    }
}
