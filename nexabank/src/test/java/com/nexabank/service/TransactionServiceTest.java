package com.nexabank.service;

import com.nexabank.dto.request.DepositWithdrawRequest;
import com.nexabank.dto.request.TransferRequest;
import com.nexabank.dto.response.TransactionResponse;
import com.nexabank.exception.AccountFrozenException;
import com.nexabank.exception.ApiException;
import com.nexabank.exception.InsufficientFundsException;
import com.nexabank.model.Account;
import com.nexabank.model.Transaction;
import com.nexabank.model.User;
import com.nexabank.model.enums.AccountStatus;
import com.nexabank.model.enums.AccountType;
import com.nexabank.model.enums.Role;
import com.nexabank.model.enums.TransactionType;
import com.nexabank.repository.AccountRepository;
import com.nexabank.repository.TransactionRepository;
import com.nexabank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock AccountRepository     accountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository        userRepository;
    @Mock AccountService        accountService;
    @Mock AuditLogService       auditLogService;

    @InjectMocks TransactionService transactionService;

    private User    owner;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).email("juan@test.com")
                .fullName("Juan dela Cruz").role(Role.CUSTOMER).build();

        account = Account.builder()
                .id(1L).accountNumber("PH-20260624-ABC123")
                .user(owner).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE).build();
    }

    // ── Deposit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deposit → increases balance and records DEPOSIT transaction")
    void deposit_success() {
        DepositWithdrawRequest req = new DepositWithdrawRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("1000.00"));
        req.setDescription("Salary");

        Transaction saved = Transaction.builder()
                .id(1L).account(account).type(TransactionType.DEPOSIT)
                .amount(req.getAmount())
                .balanceAfter(new BigDecimal("6000.00")).build();

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));

        TransactionResponse response = transactionService.deposit("juan@test.com", req);

        assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(account.getBalance()).isEqualByComparingTo("6000.00");
        verify(accountRepository).save(account);
    }

    // ── Withdraw ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("withdraw → decreases balance and records WITHDRAWAL transaction")
    void withdraw_success() {
        DepositWithdrawRequest req = new DepositWithdrawRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("500.00"));

        Transaction saved = Transaction.builder()
                .id(2L).account(account).type(TransactionType.WITHDRAWAL)
                .amount(req.getAmount())
                .balanceAfter(new BigDecimal("4500.00")).build();

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));

        TransactionResponse response = transactionService.withdraw("juan@test.com", req);

        assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(account.getBalance()).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName("withdraw → throws InsufficientFundsException when balance is too low")
    void withdraw_insufficientFunds_throws() {
        DepositWithdrawRequest req = new DepositWithdrawRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("99999.00")); // more than balance

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);

        assertThatThrownBy(() -> transactionService.withdraw("juan@test.com", req))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("withdraw → throws AccountFrozenException when account is frozen")
    void withdraw_frozenAccount_throws() {
        account.setStatus(AccountStatus.FROZEN);

        DepositWithdrawRequest req = new DepositWithdrawRequest();
        req.setAccountId(1L);
        req.setAmount(new BigDecimal("100.00"));

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);

        assertThatThrownBy(() -> transactionService.withdraw("juan@test.com", req))
                .isInstanceOf(AccountFrozenException.class);
    }

    // ── Transfer ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("transfer → atomically debits sender and credits receiver")
    void transfer_success() {
        Account receiver = Account.builder()
                .id(2L).accountNumber("PH-20260624-XYZ999")
                .user(User.builder().id(2L).email("maria@test.com").build())
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE).build();

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountNumber("PH-20260624-XYZ999");
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Rent");

        Transaction outTx = Transaction.builder()
                .id(3L).account(account).type(TransactionType.TRANSFER_OUT)
                .amount(req.getAmount()).balanceAfter(new BigDecimal("4500.00")).build();

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);
        when(accountService.findByAccountNumber("PH-20260624-XYZ999")).thenReturn(receiver);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(outTx);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(owner));

        TransactionResponse response = transactionService.transfer("juan@test.com", req);

        assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(account.getBalance()).isEqualByComparingTo("4500.00");  // debited
        assertThat(receiver.getBalance()).isEqualByComparingTo("1500.00"); // credited
        verify(accountRepository, times(2)).save(any(Account.class));      // both sides saved
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // both records
    }

    @Test
    @DisplayName("transfer → throws when sending to own account")
    void transfer_sameAccount_throws() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountNumber("PH-20260624-ABC123");
        req.setAmount(new BigDecimal("100.00"));

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);
        when(accountService.findByAccountNumber("PH-20260624-ABC123")).thenReturn(account);

        assertThatThrownBy(() -> transactionService.transfer("juan@test.com", req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("same account");
    }

    @Test
    @DisplayName("transfer → throws InsufficientFundsException when balance too low")
    void transfer_insufficientFunds_throws() {
        Account receiver = Account.builder()
                .id(2L).accountNumber("PH-20260624-XYZ999")
                .status(AccountStatus.ACTIVE).build();

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountNumber("PH-20260624-XYZ999");
        req.setAmount(new BigDecimal("99999.00"));

        when(accountService.findAndAssertOwnership(1L, "juan@test.com")).thenReturn(account);
        when(accountService.findByAccountNumber("PH-20260624-XYZ999")).thenReturn(receiver);

        assertThatThrownBy(() -> transactionService.transfer("juan@test.com", req))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
