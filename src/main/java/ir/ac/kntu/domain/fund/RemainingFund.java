package ir.ac.kntu.domain.fund;

import java.time.Instant;

/**
 * Micro-savings fund accumulating 75% of least significant digits to nearest power of 10.
 */
public class RemainingFund extends Fund {
    private static final double SAVINGS_RATE = 0.75;

    public RemainingFund(String fundId, String ownerPhone, double initialSum, Instant createdAt) {
        super(fundId, ownerPhone, FundType.REMAINING, initialSum, createdAt);
    }

    public static double computeRoundUp(double amount) {
        if (amount <= 0) {
            return 0.0;
        }
        double logVal = Math.log10(amount);
        long lowerExp = (long) Math.floor(logVal);
        long upperExp = (long) Math.ceil(logVal);

        double lowerPower = Math.pow(10, lowerExp);
        double upperPower = Math.pow(10, upperExp);

        double lowerDiff = Math.abs(amount - lowerPower);
        double upperDiff = Math.abs(upperPower - amount);

        double leastDigits = Math.min(lowerDiff, upperDiff);
        if (leastDigits == 0.0) {
            return 0.0;
        }
        return leastDigits * SAVINGS_RATE;
    }
}