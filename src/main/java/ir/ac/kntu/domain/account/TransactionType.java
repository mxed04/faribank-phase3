package ir.ac.kntu.domain.account;

/**
 * Ledger transaction categories supported in Faribank.
 */
public enum TransactionType {
    CHARGE,
    TRANSFER,
    TRANSFER_IN,
    TRANSFER_OUT,
    SIM_CHARGE
}