package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransferServiceTest {
    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    private Account account1;
    private Account account2;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        account1 = new Account("3455667", new BigDecimal("23445"));
        account2 = new Account("993992", new BigDecimal("4445"));
    }

    @Test
    public void testAccountNotFound(){
        when(accountsRepository.getAccount("3455667")).thenReturn(null);
        Exception exception = assertThrows(javax.security.auth.login.AccountNotFoundException.class,() ->
                transferService.transferMoney("3455667","993992",new BigDecimal("1000")));

        assertEquals("Sender account not found: 3455667", exception.getMessage());
        verify(accountsRepository, never()).updateAccount(any());
        verify(notificationService,never()).notifyAboutTransfer(any(),any());
    }

    @Test
    public void testTransferFailsDueToDuplicateAccount(){
        Exception exception = assertThrows(DuplicateAccountIdException.class, () ->
                transferService.transferMoney("3455667", "3455667", new BigDecimal("100"))
        );

        assertEquals("Sender and beneficiary account cannot be the same.", exception.getMessage());
        verify(accountsRepository, never()).updateAccount(any());
        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    public void testTransferSuccess() throws Exception {
        when(accountsRepository.getAccount("3455667")).thenReturn(account1);
        when(accountsRepository.getAccount("993992")).thenReturn(account2);

        transferService.transferMoney("3455667", "993992", new BigDecimal("100"));

        assertEquals(new BigDecimal("23345"), account1.getBalance());
        assertEquals(new BigDecimal("4545"), account2.getBalance());

        verify(accountsRepository).updateAccount(account1);
        verify(accountsRepository).updateAccount(account2);
        verify(notificationService).notifyAboutTransfer(account1, "Transferred 100 to " + account1.getAccountId());
        verify(notificationService).notifyAboutTransfer(account2, "Received 100 from " + account2.getAccountId());
    }

    @Test
    public void testTransferFailsDueToInsufficientBalance() {
        when(accountsRepository.getAccount("3455667")).thenReturn(account1);
        when(accountsRepository.getAccount("993992")).thenReturn(account2);

        Exception exception = assertThrows(InsufficientBalanceException.class, () ->
                transferService.transferMoney("3455667", "993992", new BigDecimal("200000"))
        );

        assertEquals("Insufficient balance in sender account: 3455667", exception.getMessage());
        verify(accountsRepository, never()).updateAccount(any());
        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    public void testTransferFailsDueToInvalidAmount() {
        when(accountsRepository.getAccount("3455667")).thenReturn(account1);
        when(accountsRepository.getAccount("993992")).thenReturn(account2);

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                transferService.transferMoney("3455667", "993992", new BigDecimal("-100"))
        );

        assertEquals("Amount Transfer must be positive", exception.getMessage());
        verify(accountsRepository, never()).updateAccount(any());
        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }
}
