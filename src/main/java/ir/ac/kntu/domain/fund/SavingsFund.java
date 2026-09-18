package ir.ac.kntu.domain.fund;

import java.time.Instant;

/**
 * Flexible savings fund permitting arbitrary deposits and withdrawals.
 */
public class SavingsFund extends Fund {
    public SavingsFund(String fundId, String ownerPhone, double initialSum, Instant createdAt) {
        super(fundId, ownerPhone, FundType.SAVINGS, initialSum, createdAt);
    }
}