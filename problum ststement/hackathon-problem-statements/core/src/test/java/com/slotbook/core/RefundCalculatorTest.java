package com.slotbook.core;

import com.slotbook.core.bookings.RefundCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundCalculatorTest {

    @Test
    void earlyCancellationRefunds90Percent() {
        assertEquals(405.00, RefundCalculator.refundFor(450.00, true), 0.001);
        assertEquals(300.00, RefundCalculator.refundFor(333.33, true), 0.001);
        assertEquals(121.91, RefundCalculator.refundFor(135.45, true), 0.001);
        assertEquals(116.96, RefundCalculator.refundFor(129.95, true), 0.001);
    }

    @Test
    void lateCancellationRefundsHalf() {
        assertEquals(299.50, RefundCalculator.refundFor(599.00, false), 0.001);
        assertEquals(225.00, RefundCalculator.refundFor(450.00, false), 0.001);
        assertEquals(67.73, RefundCalculator.refundFor(135.45, false), 0.001);
    }
}
