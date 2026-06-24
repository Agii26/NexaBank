package com.nexabank.service;

import com.nexabank.dto.request.CreateAccountRequest;
import com.nexabank.dto.response.AccountResponse;
import com.nexabank.exception.ApiException;
import com.nexabank.exception.ResourceNotFoundException;
import com.nexabank.model.Account;
import com.nexabank.model.User;
import com.nexabank.model.enums.AccountStatus;
import com.nexabank.repository.AccountRepository;
import com.nexabank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Public API ─────────────────────────────────────────────────────────────

    @Transactional
    public AccountResponse createAccount(String email, CreateAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        log.info("Account {} created for user {}", account.getAccountNumber(), email);

        auditLogService.log(user.getId(), "ACCOUNT_CREATED", "Account",
                account.getId(), null,
                "Type: " + request.getAccountType() +
                ", Number: " + account.getAccountNumber());

        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return accountRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, String email) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        assertOwnership(account, email);
        return toResponse(account);
    }

    // ── Internal helpers (used by TransactionService) ──────────────────────────

    public Account findAndAssertOwnership(Long accountId, String email) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        assertOwnership(account, email);
        return account;
    }

    public Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void assertOwnership(Account account, String email) {
        if (!account.getUser().getEmail().equals(email)) {
            throw new ApiException("Access denied to this account", HttpStatus.FORBIDDEN);
        }
    }

    private String generateAccountNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String candidate;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            candidate = "PH-" + date + "-" + sb;
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .ownerName(account.getUser().getFullName())
                .build();
    }
}
