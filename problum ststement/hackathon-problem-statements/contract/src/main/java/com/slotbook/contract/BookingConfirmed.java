package com.slotbook.contract;

import java.time.Instant;
import java.util.UUID;

/** Published after a seat is booked and paid for. Delivered at least once: see CONTRACT.md. */
public record BookingConfirmed(UUID eventId, long bookingId, long slotId, long memberId, Instant occurredAt) {
}
