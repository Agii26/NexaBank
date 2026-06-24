package com.nexabank.dto.request;

import com.nexabank.model.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "Account type is required (SAVINGS or CHECKING)")
    private AccountType accountType;
}
