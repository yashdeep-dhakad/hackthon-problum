package com.slotbook.contract;

import java.time.Instant;
import java.util.UUID;

/** Published after a booking is cancelled and its seat freed. Delivered at least once: see CONTRACT.md. */
public record BookingCancelled(UUID eventId, long bookingId, long slotId, long memberId, Instant occurredAt) {
}
