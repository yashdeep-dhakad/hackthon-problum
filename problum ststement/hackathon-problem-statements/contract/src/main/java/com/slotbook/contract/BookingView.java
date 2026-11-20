package com.slotbook.contract;

/** A booking as modules see it. status is "CONFIRMED" or "CANCELLED". */
public record BookingView(long id, long slotId, long memberId, String status) {

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }
}
