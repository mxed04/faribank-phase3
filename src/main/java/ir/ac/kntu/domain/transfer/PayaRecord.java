package ir.ac.kntu.domain.transfer;

import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.util.Calendar;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable entity capturing queued Paya fund transfers pending batch execution.
 */
public class PayaRecord {
    private static final double DEFAULT_PAYA_FEE = 2000.0;

    private final String payaId;
    private final String sourceAcc;
    private final String destAcc;
    private final double amount;
    private final double fee;
    private final Instant createdAt;
    private String senderPhone;
    private PayaStatus status;
    private Instant processedAt;

    public PayaRecord(String payaId, String sourceAcc, String destAcc, double amount) {
        if (payaId == null || payaId.trim().isEmpty()) {
            throw new ValidationException("Paya ID cannot be empty.");
        }
        this.payaId = payaId.trim();
        this.sourceAcc = Objects.requireNonNull(sourceAcc, "Source account is required.");
        this.destAcc = Objects.requireNonNull(destAcc, "Destination account is required.");
        this.amount = amount;
        this.fee = DEFAULT_PAYA_FEE;
        this.createdAt = Calendar.now();
        this.status = PayaStatus.QUEUED;
        this.senderPhone = "";
    }

    public String getPayaId() {
        return payaId;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone != null ? senderPhone.trim() : "";
    }

    public String getSourceAcc() {
        return sourceAcc;
    }

    public String getDestAcc() {
        return destAcc;
    }

    public double getAmount() {
        return amount;
    }

    public double getFee() {
        return fee;
    }

    public PayaStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public synchronized void markProcessed(Instant processedInstant) {
        this.status = PayaStatus.PROCESSED;
        this.processedAt = processedInstant;
    }

    public synchronized void markRejected(Instant rejectedInstant) {
        this.status = PayaStatus.REJECTED;
        this.processedAt = rejectedInstant;
    }
}