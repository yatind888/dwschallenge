package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransferService {
    @Autowired
    private AccountsRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    private final ReentrantLock lock = new ReentrantLock();

    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) throws Exception{
        lock.lock();
        try{
            if (fromAccountId.equals(toAccountId)) {
                throw new DuplicateAccountIdException("Sender and beneficiary account cannot be the same.");
            }

            Account fromAccount = accountRepository.getAccount(fromAccountId);
            Account toAccount = accountRepository.getAccount(toAccountId);

            if (fromAccount == null) {
                throw new AccountNotFoundException("Sender account not found: " + fromAccountId);
            }
            if (toAccount == null) {
                throw new AccountNotFoundException("Beneficiary account not found: " + toAccountId);
            }

            if(amount.compareTo(BigDecimal.ZERO)<= 0){
                throw new IllegalArgumentException("Amount Transfer must be positive");
            }

            fromAccount.debit(amount);
            toAccount.credit(amount);

            accountRepository.updateAccount(fromAccount);
            accountRepository.updateAccount(toAccount);

            notificationService.notifyAboutTransfer(fromAccount, "Transferred " + amount + " to " + fromAccount.getAccountId());
            notificationService.notifyAboutTransfer(toAccount, "Received " + amount + " from " + toAccount.getAccountId());
        }finally {
            lock.unlock();
        }
    }
}
