package com.slotbook.contract;

/** What happened when a module asked core to cancel a booking. refund (rupees, "121.91") is set only when CANCELLED. */
public record CancelResult(Outcome outcome, String refund) {

    public enum Outcome {
        CANCELLED,
        NOT_FOUND,
        NOT_YOURS,
        ALREADY_CANCELLED,
        CLASS_STARTED
    }

    public boolean cancelled() {
        return outcome == Outcome.CANCELLED;
    }
}
