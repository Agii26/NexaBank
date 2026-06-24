package com.nexabank.service;

import com.nexabank.dto.request.DepositWithdrawRequest;
import com.nexabank.dto.request.TransferRequest;
import com.nexabank.dto.response.PageResponse;
import com.nexabank.dto.response.TransactionResponse;
import com.nexabank.exception.AccountFrozenException;
import com.nexabank.exception.ApiException;
import com.nexabank.exception.InsufficientFundsException;
import com.nexabank.exception.ResourceNotFoundException;
import com.nexabank.model.Account;
import com.nexabank.model.Transaction;
import com.nexabank.model.enums.AccountStatus;
import com.nexabank.model.enums.TransactionType;
import com.nexabank.repository.AccountRepository;
import com.nexabank.repository.TransactionRepository;
import com.nexabank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final AuditLogService auditLogService;

    // ── Deposit ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse deposit(String email, DepositWithdrawRequest request) {
        Account account = accountService.findAndAssertOwnership(request.getAccountId(), email);
        assertActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = save(Transaction.builder()
                .account(account)
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .description(request.getDescription())
                .balanceAfter(account.getBalance())
                .build());

        Long userId = userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
        auditLogService.log(userId, "DEPOSIT", "Account", account.getId(), null,
                "Amount: +" + request.getAmount() + " | Balance: " + account.getBalance());

        log.info("Deposit {} to account {}", request.getAmount(), account.getAccountNumber());
        return toResponse(tx);
    }

    // ── Withdrawal ─────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse withdraw(String email, DepositWithdrawRequest request) {
        Account account = accountService.findAndAssertOwnership(request.getAccountId(), email);
        assertActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(account.getBalance(), request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = save(Transaction.builder()
                .account(account)
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .description(request.getDescription())
                .balanceAfter(account.getBalance())
                .build());

        Long userId = userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
        auditLogService.log(userId, "WITHDRAWAL", "Account", account.getId(), null,
                "Amount: -" + request.getAmount() + " | Balance: " + account.getBalance());

        log.info("Withdrawal {} from account {}", request.getAmount(), account.getAccountNumber());
        return toResponse(tx);
    }

    // ── Transfer ───────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse transfer(String email, TransferRequest request) {
        Account from = accountService.findAndAssertOwnership(request.getFromAccountId(), email);
        assertActive(from);

        Account to = accountService.findByAccountNumber(request.getToAccountNumber());

        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(to.getAccountNumber());
        }
        if (from.getId().equals(to.getId())) {
            throw new ApiException("Cannot transfer to the same account", HttpStatus.BAD_REQUEST);
        }
        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(from.getBalance(), request.getAmount());
        }

        // Atomic: both balance changes in the same @Transactional boundary
        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));
        accountRepository.save(from);
        accountRepository.save(to);

        // Record both sides
        Transaction outTx = save(Transaction.builder()
                .account(from)
                .counterpartAccount(to)
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.getAmount())
                .description(request.getDescription())
                .balanceAfter(from.getBalance())
                .build());

        save(Transaction.builder()
                .account(to)
                .counterpartAccount(from)
                .type(TransactionType.TRANSFER_IN)
                .amount(request.getAmount())
                .description(request.getDescription())
                .balanceAfter(to.getBalance())
                .build());

        Long userId = userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
        auditLogService.log(userId, "TRANSFER", "Account", from.getId(), null,
                from.getAccountNumber() + " → " + to.getAccountNumber() +
                " | Amount: " + request.getAmount());

        log.info("Transfer {} from {} to {}",
                request.getAmount(), from.getAccountNumber(), to.getAccountNumber());
        return toResponse(outTx);
    }

    // ── Transaction history ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(
            Long accountId, String email, Pageable pageable) {
        accountService.findAndAssertOwnership(accountId, email); // ownership check
        return PageResponse.from(
                transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable),
                this::toResponse);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void assertActive(Account account) {
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(account.getAccountNumber());
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ApiException("Account " + account.getAccountNumber() +
                    " is closed", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Transaction save(Transaction tx) {
        return transactionRepository.save(tx);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .counterpartAccountNumber(tx.getCounterpartAccount() != null
                        ? tx.getCounterpartAccount().getAccountNumber() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
