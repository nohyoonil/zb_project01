package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.TransactionResultType.*;
import static com.example.account.type.TransactionType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        AccountUser user = AccountUser.builder().id(1L).name("tester").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        Account account = Account.builder()
                .accountUser(user).balance(10000L)
                .accountStatus(IN_USE).accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build());

        TransactionDto transactionDto = transactionService.useBalance(
                1L, "1000000000", 1000L);

        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    void useBalance_UserNotFount() {

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        assertEquals(exception.getErrorCode(), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void useBalance_AccountNotFound() {
        AccountUser user = AccountUser.builder().id(15L).name("tester1").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void useBalance_UserUnMatch() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();
        AccountUser otherUser = AccountUser.builder().id(13L).name("tester2").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(otherUser).balance(0L).accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    void useBalance_AlreadyUnregistered() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user).accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L).accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    void useBalance_exceedAmount() {
        AccountUser user = AccountUser.builder().id(1L).name("tester").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        Account account = Account.builder()
                .accountUser(user).balance(100L)
                .accountStatus(IN_USE).accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    void saveFailedUseTransaction() {
        AccountUser user = AccountUser.builder().id(1L).name("tester").build();

        Account account = Account.builder()
                .accountUser(user).balance(10000L)
                .accountStatus(IN_USE).accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        AccountUser user = AccountUser.builder().id(1L).name("tester").build();

        Account account = Account.builder()
                .accountUser(user).balance(10000L)
                .accountStatus(IN_USE).accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionIdForCancel")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(10000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", CANCEL_AMOUNT);

        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    void cancelTransaction_AccountNotFound() {
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void cancelTransaction_TransactionNotFound() {
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());


        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 1000L));

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        AccountUser user = AccountUser.builder().id(1L).name("tester").build();

        Account account = Account.builder()
                .accountUser(user).balance(10000L)
                .accountStatus(IN_USE).accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    void queryTransaction_TransactionNotFound() {
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}