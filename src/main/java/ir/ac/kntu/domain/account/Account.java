package ir.ac.kntu.domain.account;

import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bank account entity managing balance and ledger history with thread-safe operations.
 */
public class Account {
    private final String accountNumber;
    private final String ownerPhone;
    private final CreditCard card;
    private double balance;
    private final List<Transaction> transactions;

    public Account(String accountNumber, String ownerPhone, CreditCard card) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new ValidationException("Account number cannot be empty.");
        }
        if (ownerPhone == null || ownerPhone.trim().isEmpty()) {
            throw new ValidationException("Owner phone cannot be empty.");
        }
        this.accountNumber = accountNumber.trim();
        this.ownerPhone = ownerPhone.trim();
        this.card = Objects.requireNonNull(card, "Card cannot be null.");
        this.balance = 0.0;
        this.transactions = Collections.synchronizedList(new ArrayList<>());
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public String getOwnerPhoneNumber() {
        return ownerPhone;
    }

    public CreditCard getCard() {
        return card;
    }

    public CreditCard getCreditCard() {
        return card;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        synchronized (transactions) {
            return List.copyOf(transactions);
        }
    }

    public synchronized void charge(double amount, Transaction trx) {
        if (amount <= 0) {
            throw new ValidationException("Charge amount must be positive.");
        }
        this.balance += amount;
        if (trx != null) {
            this.transactions.add(trx);
        }
    }

    public synchronized void credit(double amount, Transaction trx) {
        charge(amount, trx);
    }

    public synchronized void debit(double amount, Transaction trx) {
        if (amount <= 0) {
            throw new ValidationException("Debit amount must be positive.");
        }
        if (amount > this.balance) {
            throw new InsufficientFundsException("Insufficient funds in account.");
        }
        this.balance -= amount;
        if (trx != null) {
            this.transactions.add(trx);
        }
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new ValidationException("Deposit amount must be positive.");
        }
        this.balance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new ValidationException("Withdraw amount must be positive.");
        }
        if (amount > this.balance) {
            throw new InsufficientFundsException("Insufficient funds in account.");
        }
        this.balance -= amount;
    }

    public void addTransaction(Transaction trx) {
        if (trx != null) {
            this.transactions.add(trx);
        }
    }
}