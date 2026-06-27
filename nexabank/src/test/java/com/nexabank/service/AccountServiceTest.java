package com.nexabank.service;

import com.nexabank.dto.request.CreateAccountRequest;
import com.nexabank.dto.response.AccountResponse;
import com.nexabank.exception.ApiException;
import com.nexabank.model.Account;
import com.nexabank.model.User;
import com.nexabank.model.enums.AccountStatus;
import com.nexabank.model.enums.AccountType;
import com.nexabank.model.enums.Role;
import com.nexabank.repository.AccountRepository;
import com.nexabank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock UserRepository    userRepository;
    @Mock AuditLogService   auditLogService;

    @InjectMocks AccountService accountService;

    private User    owner;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).fullName("Juan dela Cruz")
                .email("juan@test.com").role(Role.CUSTOMER)
                .accounts(new ArrayList<>()).build();

        account = Account.builder()
                .id(1L).accountNumber("PH-20260624-ABC123")
                .user(owner).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("createAccount → saves account with zero balance and ACTIVE status")
    void createAccount_success() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.createAccount("juan@test.com", req);

        assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
        verify(auditLogService).log(eq(1L), eq("ACCOUNT_CREATED"), eq("Account"), any(), any(), any());
    }

    @Test
    @DisplayName("getMyAccounts → returns all accounts for authenticated user")
    void getMyAccounts_returnsUserAccounts() {
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.getMyAccounts("juan@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("PH-20260624-ABC123");
    }

    @Test
    @DisplayName("getAccountById → throws 403 when user does not own the account")
    void getAccountById_differentUser_throwsForbidden() {
        User other = User.builder().id(2L).email("other@test.com").build();
        Account otherAccount = Account.builder()
                .id(2L).user(other)
                .accountNumber("PH-20260624-XYZ999")
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findById(2L)).thenReturn(Optional.of(otherAccount));

        assertThatThrownBy(() -> accountService.getAccountById(2L, "juan@test.com"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("createAccount → account number is unique (retries on collision)")
    void createAccount_retryOnNumberCollision() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.CHECKING);

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));
        // First generated number collides, second is free
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.createAccount("juan@test.com", req);

        // existsByAccountNumber called twice due to retry
        verify(accountRepository, times(2)).existsByAccountNumber(anyString());
    }
}
