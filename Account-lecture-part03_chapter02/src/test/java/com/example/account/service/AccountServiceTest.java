package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder().accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder().accountUser(user).accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        AccountUser user = AccountUser.builder().id(15L).name("tester1").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder().accountUser(user).accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    void createAccount_UserNotFount() {

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        assertEquals(exception.getErrorCode(), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void createAccount_maxAccountIs10() {
        AccountUser user = AccountUser.builder().id(15L).name("tester1").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any())).willReturn(10);

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        assertEquals(exception.getErrorCode(), ErrorCode.MAX_ACCOUNT_PER_USER_10);
    }

    @Test
    void deleteAccount_UserNotFount() {

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(exception.getErrorCode(), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void deleteAccount_AccountNotFound() {
        AccountUser user = AccountUser.builder().id(15L).name("tester1").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteAccountFailed_UserUnMatch() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();
        AccountUser otherUser = AccountUser.builder().id(13L).name("tester2").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(otherUser).balance(0L).accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    void deleteAccountFailed_BalanceNotEmpty() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user).balance(100L).accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    void deleteAccountFailed_AlreadyUnregistered() {
        AccountUser user = AccountUser.builder().id(12L).name("tester1").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user).accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L).accountNumber("1000000012").build()));

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        AccountUser user = AccountUser.builder().id(1L).name("tester1").build();

        List<Account> accounts = Arrays.asList(
                Account.builder().accountUser(user).accountNumber("1234567890").balance(1000L).build(),
                Account.builder().accountUser(user).accountNumber("1234567891").balance(2000L).build(),
                Account.builder().accountUser(user).accountNumber("1234567892").balance(3000L).build());

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any())).willReturn(accounts);

        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        assertEquals(3, accountDtos.size());
        assertEquals("1234567890", accountDtos.get(0).getAccountNumber());
        assertEquals(1000, accountDtos.get(0).getBalance());
        assertEquals("1234567891", accountDtos.get(1).getAccountNumber());
        assertEquals(2000, accountDtos.get(1).getBalance());
        assertEquals("1234567892", accountDtos.get(2).getAccountNumber());
        assertEquals(3000, accountDtos.get(2).getBalance());
    }

    @Test
    void failedToGetAccounts() {
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        assertEquals(exception.getErrorCode(), ErrorCode.USER_NOT_FOUND);
    }
}