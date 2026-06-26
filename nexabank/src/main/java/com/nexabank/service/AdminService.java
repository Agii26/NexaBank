package com.nexabank.service;

import com.nexabank.dto.response.AccountAdminResponse;
import com.nexabank.dto.response.PageResponse;
import com.nexabank.dto.response.UserAdminResponse;
import com.nexabank.exception.ApiException;
import com.nexabank.exception.ResourceNotFoundException;
import com.nexabank.model.Account;
import com.nexabank.model.User;
import com.nexabank.model.enums.AccountStatus;
import com.nexabank.repository.AccountRepository;
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
public class AdminService {

    private final UserRepository    userRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService   auditLogService;

    // ── Users ──────────────────────────────────────────────────────────────────

    public PageResponse<UserAdminResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(pageable),
                this::toUserResponse);
    }

    // ── Accounts ───────────────────────────────────────────────────────────────

    public PageResponse<AccountAdminResponse> getAllAccounts(Pageable pageable) {
        return PageResponse.from(
                accountRepository.findAll(pageable),
                this::toAccountResponse);
    }

    @Transactional
    public AccountAdminResponse freezeAccount(Long accountId, String adminEmail) {
        Account account = getAccount(accountId);
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new ApiException("Account is already frozen", HttpStatus.CONFLICT);
        }
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);

        Long adminId = userRepository.findByEmail(adminEmail).map(User::getId).orElse(null);
        auditLogService.log(adminId, "ACCOUNT_FROZEN", "Account", accountId, null,
                "Frozen by admin: " + adminEmail);

        log.info("Account {} frozen by {}", account.getAccountNumber(), adminEmail);
        return toAccountResponse(account);
    }

    @Transactional
    public AccountAdminResponse unfreezeAccount(Long accountId, String adminEmail) {
        Account account = getAccount(accountId);
        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new ApiException("Account is not frozen", HttpStatus.CONFLICT);
        }
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        Long adminId = userRepository.findByEmail(adminEmail).map(User::getId).orElse(null);
        auditLogService.log(adminId, "ACCOUNT_UNFROZEN", "Account", accountId, null,
                "Unfrozen by admin: " + adminEmail);

        log.info("Account {} unfrozen by {}", account.getAccountNumber(), adminEmail);
        return toAccountResponse(account);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    private UserAdminResponse toUserResponse(User u) {
        return UserAdminResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .accountCount(u.getAccounts().size())
                .build();
    }

    private AccountAdminResponse toAccountResponse(Account a) {
        return AccountAdminResponse.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .ownerName(a.getUser().getFullName())
                .ownerEmail(a.getUser().getEmail())
                .accountType(a.getAccountType())
                .balance(a.getBalance())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
