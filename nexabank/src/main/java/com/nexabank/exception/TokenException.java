package com.nexabank.exception;

import org.springframework.http.HttpStatus;

public class TokenException extends ApiException {
    public TokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
