package ir.ac.kntu.domain.fund;

import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Abstract domain model representing a capital investment fund.
 */
public abstract class Fund {
    private final String fundId;
    private final String ownerPhone;
    private final FundType fundType;
    private final Instant createdAt;
    private double balance;

    public Fund(String fundId, String ownerPhone, FundType fundType,
                double initialSum, Instant createdAt) {
        if (fundId == null || fundId.trim().isEmpty()) {
            throw new ValidationException("Fund ID cannot be empty.");
        }
        if (ownerPhone == null || !ownerPhone.trim().matches("^09\\d{9}$")) {
            throw new ValidationException("Invalid owner mobile number.");
        }
        if (initialSum < 0) {
            throw new ValidationException("Initial balance cannot be negative.");
        }
        this.fundId = fundId.trim();
        this.ownerPhone = ownerPhone.trim();
        this.fundType = Objects.requireNonNull(fundType, "Fund type cannot be null.");
        this.balance = initialSum;
        this.createdAt = Objects.requireNonNull(createdAt, "Creation date is mandatory.");
    }

    public String getFundId() {
        return fundId;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public FundType getFundType() {
        return fundType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new ValidationException("Deposit amount must be strictly positive.");
        }
        this.balance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new ValidationException("Withdrawal amount must be strictly positive.");
        }
        if (amount > this.balance) {
            throw new InsufficientFundsException("Insufficient fund balance: " + balance);
        }
        this.balance -= amount;
    }
}