package com.nexabank.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException(BigDecimal balance, BigDecimal requested) {
        super(String.format(
            "Insufficient funds. Available: %.2f, Requested: %.2f",
            balance, requested), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
