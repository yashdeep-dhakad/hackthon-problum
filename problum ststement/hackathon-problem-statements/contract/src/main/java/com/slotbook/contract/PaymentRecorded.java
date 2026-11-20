package com.slotbook.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * A charge or refund the gateway has confirmed. Sent on {@link Topics#PAYMENTS} as JSON in a JMS text message.
 *
 * @param kind   "CHARGE" or "REFUND"
 * @param amount rupees, as a decimal string with two places ("135.45"); always positive
 */
public record PaymentRecorded(UUID eventId, long paymentId, long bookingId, long slotId, long memberId,
                              String kind, String amount, Instant occurredAt) {
}
