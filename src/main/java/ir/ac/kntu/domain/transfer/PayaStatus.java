package ir.ac.kntu.domain.transfer;

/**
 * Lifecycle states of queued Paya batch settlement items.
 */
public enum PayaStatus {
    QUEUED,
    PROCESSED,
    REJECTED
}