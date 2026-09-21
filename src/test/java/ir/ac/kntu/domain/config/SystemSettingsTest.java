package ir.ac.kntu.domain.config;

import ir.ac.kntu.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemSettingsTest {

    @Test
    void testDefaultValues() {
        SystemSettings settings = new SystemSettings();
        assertEquals(300.0, settings.getCardFee());
        assertEquals(0.02, settings.getPolFeeRate());
        assertEquals(2000.0, settings.getPayaFee());
        assertEquals(0.0, settings.getFariFee());
        assertEquals(0.15, settings.getRewardRate());
        assertEquals(0.09, settings.getChargeTaxRate());
    }

    @Test
    void testValidMutations() {
        SystemSettings settings = new SystemSettings();
        settings.setCardFee(500.0);
        settings.setPolFeeRate(0.03);
        settings.setPayaFee(2500.0);
        settings.setRewardRate(0.20);
        settings.setChargeTaxRate(0.10);

        assertEquals(500.0, settings.getCardFee());
        assertEquals(0.03, settings.getPolFeeRate());
        assertEquals(2500.0, settings.getPayaFee());
        assertEquals(0.20, settings.getRewardRate());
        assertEquals(0.10, settings.getChargeTaxRate());
    }

    @Test
    void testNegativeOrInvalidRatesThrowException() {
        SystemSettings settings = new SystemSettings();
        assertThrows(ValidationException.class, () -> settings.setCardFee(-1.0));
        assertThrows(ValidationException.class, () -> settings.setPayaFee(-50.0));
        assertThrows(ValidationException.class, () -> settings.setPolFeeRate(-0.1));
        assertThrows(ValidationException.class, () -> settings.setPolFeeRate(1.5));
        assertThrows(ValidationException.class, () -> settings.setRewardRate(2.0));
        assertThrows(ValidationException.class, () -> settings.setChargeTaxRate(-0.05));
    }
}