package com.slotbook.core.bookings;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cancellation policy: 90% back if the booking is cancelled at least 24 hours before the class starts,
 * otherwise 50%.
 */
public final class RefundCalculator {

    static final BigDecimal EARLY_REFUND = BigDecimal.valueOf(0.9);
    static final BigDecimal LATE_REFUND = BigDecimal.valueOf(0.5);

    private RefundCalculator() {
    }

    /** The refund for an amount paid, in rupees, rounded to the nearest paisa. */
    public static double refundFor(double paid, boolean early) {
        return BigDecimal.valueOf(paid)
                .multiply(early ? EARLY_REFUND : LATE_REFUND)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
