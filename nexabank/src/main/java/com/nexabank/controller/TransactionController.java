package com.nexabank.controller;

import com.nexabank.dto.request.DepositWithdrawRequest;
import com.nexabank.dto.request.TransferRequest;
import com.nexabank.dto.response.PageResponse;
import com.nexabank.dto.response.TransactionResponse;
import com.nexabank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer, and view history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money into an account")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositWithdrawRequest request,
            Authentication auth) {
        return ResponseEntity.ok(transactionService.deposit(auth.getName(), request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money from an account")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody DepositWithdrawRequest request,
            Authentication auth) {
        return ResponseEntity.ok(transactionService.withdraw(auth.getName(), request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money to another account by account number")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication auth) {
        return ResponseEntity.ok(transactionService.transfer(auth.getName(), request));
    }

    @GetMapping("/{accountId}/history")
    @Operation(summary = "Get paginated transaction history for an account")
    public ResponseEntity<PageResponse<TransactionResponse>> getHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(transactionService.getHistory(accountId, auth.getName(), pageable));
    }
}
