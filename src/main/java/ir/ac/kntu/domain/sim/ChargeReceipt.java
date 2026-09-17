package ir.ac.kntu.domain.sim;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable audit receipt generated upon successful cellular recharge operations.
 */
public class ChargeReceipt {
    private final String receiptId;
    private final String sourcePhone;
    private final String targetPhone;
    private final double pureAmount;
    private final double taxAmount;
    private final double totalAmount;
    private final Instant timestamp;

    public ChargeReceipt(String receiptId, String sourcePhone, String targetPhone,
                         double pureAmount, double taxAmount, double totalAmount,
                         Instant timestamp) {
        this.receiptId = Objects.requireNonNull(receiptId, "Receipt ID cannot be null.");
        this.sourcePhone = Objects.requireNonNull(sourcePhone, "Source phone cannot be null.");
        this.targetPhone = Objects.requireNonNull(targetPhone, "Target phone cannot be null.");
        this.pureAmount = pureAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null.");
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getSourcePhone() {
        return sourcePhone;
    }

    public String getTargetPhone() {
        return targetPhone;
    }

    public double getPureAmount() {
        return pureAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}