package com.slotbook.core.bookings;

import java.time.Instant;

public record Booking(
        long id,
        long slotId,
        long memberId,
        String status,
        double amount,
        Instant createdAt,
        Instant cancelledAt) {

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }
}
