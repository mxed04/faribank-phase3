package ir.ac.kntu.domain.sim;

import ir.ac.kntu.exception.ValidationException;

/**
 * Cellular SIM card entity tracking mobile balance independent of neo-bank accounts.
 */
public class SimCard {
    private final String phoneNumber;
    private double balance;

    public SimCard(String phoneNumber) {
        this(phoneNumber, 0.0);
    }

    public SimCard(String phoneNumber, double balance) {
        if (phoneNumber == null || !phoneNumber.trim().matches("^09\\d{9}$")) {
            throw new ValidationException("Invalid mobile phone number format.");
        }
        if (balance < 0) {
            throw new ValidationException("SIM airtime balance cannot be negative.");
        }
        this.phoneNumber = phoneNumber.trim();
        this.balance = balance;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public double getBalance() {
        return balance;
    }

    public synchronized void recharge(double amount) {
        if (amount <= 0) {
            throw new ValidationException("Recharge amount must be strictly positive.");
        }
        this.balance += amount;
    }
}