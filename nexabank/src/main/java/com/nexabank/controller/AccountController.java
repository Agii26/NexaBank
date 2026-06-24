package com.nexabank.controller;

import com.nexabank.dto.request.CreateAccountRequest;
import com.nexabank.dto.response.AccountResponse;
import com.nexabank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Create and view bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new bank account (SAVINGS or CHECKING)")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(auth.getName(), request));
    }

    @GetMapping
    @Operation(summary = "List all my accounts")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication auth) {
        return ResponseEntity.ok(accountService.getMyAccounts(auth.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific account by ID")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(accountService.getAccountById(id, auth.getName()));
    }
}
