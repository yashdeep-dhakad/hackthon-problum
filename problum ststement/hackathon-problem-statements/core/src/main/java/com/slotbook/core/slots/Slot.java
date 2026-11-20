package com.slotbook.core.slots;

import java.time.Instant;

public record Slot(
        long id,
        long batchId,
        String batchName,
        String instructor,
        Instant startsAt,
        int durationMinutes,
        int capacity,
        String notes,
        double price,
        int booked) {
}
