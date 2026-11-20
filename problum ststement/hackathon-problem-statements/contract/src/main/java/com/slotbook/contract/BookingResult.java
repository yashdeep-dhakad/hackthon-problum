package com.slotbook.contract;

/** What happened when a module asked core to book a seat. bookingId is set only when outcome is BOOKED. */
public record BookingResult(Outcome outcome, Long bookingId) {

    public enum Outcome {
        BOOKED,
        FULL,
        ALREADY_BOOKED,
        SLOT_NOT_FOUND,
        SLOT_STARTED
    }

    public boolean booked() {
        return outcome == Outcome.BOOKED;
    }
}
