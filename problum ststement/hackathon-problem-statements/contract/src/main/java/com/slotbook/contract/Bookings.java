package com.slotbook.contract;

import java.util.List;
import java.util.Optional;

/** Bookings. Implemented by core. */
public interface Bookings {

    /**
     * Books a seat for a member, charging them the class price exactly as if they had booked it themselves.
     * Publishes {@link BookingConfirmed} when the outcome is BOOKED.
     */
    BookingResult book(long memberId, long slotId);

    Optional<BookingView> find(long bookingId);

    /**
     * Cancels a member's own booking and refunds it under the cancellation policy, exactly as if they had pressed
     * Cancel. Refuses a booking that is not theirs. Publishes {@link BookingCancelled} when the outcome is CANCELLED.
     */
    CancelResult cancel(long memberId, long bookingId);

    /** The member's confirmed bookings, upcoming and past. */
    List<BookingView> confirmedForMember(long memberId);
}
