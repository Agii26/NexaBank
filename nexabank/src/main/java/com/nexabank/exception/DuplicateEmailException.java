package com.nexabank.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email, HttpStatus.CONFLICT);
    }
}
