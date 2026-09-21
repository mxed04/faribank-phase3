package ir.ac.kntu.domain.fund;

import ir.ac.kntu.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Fixed-term investment fund offering fixed interest rates upon maturity.
 */
public class BonusFund extends Fund {
    private final double initialCapital;
    private final double interestRate;
    private final Instant maturityDate;
    private boolean interestPaid;

    public BonusFund(String fundId, String ownerPhone, double initialSum,
                     double interestRate, Instant maturityDate, Instant createdAt) {
        super(fundId, ownerPhone, FundType.BONUS, initialSum, createdAt);
        if (interestRate <= 0) {
            throw new ValidationException("Interest rate must be positive.");
        }
        this.initialCapital = initialSum;
        this.interestRate = interestRate;
        this.maturityDate = Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");
        this.interestPaid = false;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }

    public boolean isInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(boolean interestPaid) {
        this.interestPaid = interestPaid;
    }

    public boolean hasMatured(Instant currentInstant) {
        return currentInstant.isAfter(maturityDate) || currentInstant.equals(maturityDate);
    }

    public synchronized double claimInterest(Instant currentInstant) {
        if (!hasMatured(currentInstant)) {
            throw new ValidationException("Fund has not reached maturity date yet.");
        }
        if (interestPaid) {
            return 0.0;
        }
        double profit = initialCapital * interestRate;
        this.deposit(profit);
        this.interestPaid = true;
        return profit;
    }

    @Override
    public synchronized void withdraw(double amount) {
        if (!hasMatured(Instant.now())) {
            throw new ValidationException("Premature withdrawals are prohibited for Bonus Funds.");
        }
        super.withdraw(amount);
    }
}