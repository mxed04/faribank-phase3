package ir.ac.kntu.domain.config;

import ir.ac.kntu.exception.ValidationException;

/**
 * System-wide operational settings configurable by system administrators.
 */
public class SystemSettings {
    private double cardFee;
    private double polFeeRate;
    private double payaFee;
    private double fariFee;
    private double rewardRate;
    private double chargeTaxRate;

    public SystemSettings() {
        this.cardFee = 300.0;
        this.polFeeRate = 0.02;
        this.payaFee = 2000.0;
        this.fariFee = 0.0;
        this.rewardRate = 0.15;
        this.chargeTaxRate = 0.09;
    }

    public double getCardFee() {
        return cardFee;
    }

    public final void setCardFee(double cardFee) {
        if (cardFee < 0) {
            throw new ValidationException("Card transfer fee cannot be negative.");
        }
        this.cardFee = cardFee;
    }

    public double getPolFeeRate() {
        return polFeeRate;
    }

    public final void setPolFeeRate(double polFeeRate) {
        if (polFeeRate < 0 || polFeeRate > 1.0) {
            throw new ValidationException("POL fee rate must be between 0.0 and 1.0.");
        }
        this.polFeeRate = polFeeRate;
    }

    public double getPayaFee() {
        return payaFee;
    }

    public final void setPayaFee(double payaFee) {
        if (payaFee < 0) {
            throw new ValidationException("Paya transfer fee cannot be negative.");
        }
        this.payaFee = payaFee;
    }

    public double getFariFee() {
        return fariFee;
    }

    public final void setFariFee(double fariFee) {
        if (fariFee < 0) {
            throw new ValidationException("Fari-to-Fari fee cannot be negative.");
        }
        this.fariFee = fariFee;
    }

    public double getRewardRate() {
        return rewardRate;
    }

    public final void setRewardRate(double rewardRate) {
        if (rewardRate < 0 || rewardRate > 1.0) {
            throw new ValidationException("Reward interest rate must be between 0.0 and 1.0.");
        }
        this.rewardRate = rewardRate;
    }

    public double getChargeTaxRate() {
        return chargeTaxRate;
    }

    public final void setChargeTaxRate(double taxRate) {
        if (taxRate < 0 || taxRate > 1.0) {
            throw new ValidationException("SIM charge tax rate must be between 0.0 and 1.0.");
        }
        this.chargeTaxRate = taxRate;
    }
}