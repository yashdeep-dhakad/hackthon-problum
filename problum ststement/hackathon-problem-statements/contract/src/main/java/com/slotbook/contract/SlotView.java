package com.slotbook.contract;

import java.time.Instant;

/** A class slot as modules see it. */
public record SlotView(
        long id,
        String batchName,
        String instructor,
        Instant startsAt,
        int durationMinutes,
        int capacity,
        int confirmedCount) {

    public boolean isFull() {
        return confirmedCount >= capacity;
    }
}
