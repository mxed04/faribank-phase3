package ir.ac.kntu.domain.transfer;

/**
 * Transfer channels supported by Faribank banking switch.
 */
public enum TransferChannel {
    CARD_TO_CARD(300.0, 0.0),
    POL(0.0, 0.02),
    PAYA(2000.0, 0.0),
    FARI_TO_FARI(0.0, 0.0);

    private final double fixedFee;
    private final double rateFee;

    TransferChannel(double fixedFee, double rateFee) {
        this.fixedFee = fixedFee;
        this.rateFee = rateFee;
    }

    public double calculateFee(double amount) {
        if (amount <= 0) {
            return 0.0;
        }
        return fixedFee + (amount * rateFee);
    }
}