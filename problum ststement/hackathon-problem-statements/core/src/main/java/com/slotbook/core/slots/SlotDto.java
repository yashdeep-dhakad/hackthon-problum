package com.slotbook.core.slots;

import java.time.Instant;

public record SlotDto(
        long id,
        long batchId,
        String batchName,
        String instructor,
        Instant startsAt,
        String startsAtLocal,
        int durationMinutes,
        int capacity,
        int booked,
        double price,
        String notes) {
}
