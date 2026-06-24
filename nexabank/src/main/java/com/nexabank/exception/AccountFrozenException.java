package com.nexabank.exception;

import org.springframework.http.HttpStatus;

public class AccountFrozenException extends ApiException {
    public AccountFrozenException(String accountNumber) {
        super("Account " + accountNumber + " is frozen and cannot process transactions",
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
