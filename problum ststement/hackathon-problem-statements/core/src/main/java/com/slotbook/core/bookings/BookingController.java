package com.slotbook.core.bookings;

import com.slotbook.contract.BookingResult;
import com.slotbook.contract.CentreClock;
import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.MemberView;
import com.slotbook.core.slots.SlotDto;
import com.slotbook.core.slots.SlotFormatter;
import com.slotbook.core.slots.SlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class BookingController {

    public record BookRequest(long slotId) {
    }

    public record MyBooking(long id, String status, double amount, LocalDate date, SlotDto slot) {
    }

    private final CurrentMember current;
    private final BookingService service;
    private final BookingRepository bookings;
    private final SlotRepository slots;
    private final SlotFormatter formatter;
    private final CentreClock centreClock;

    public BookingController(CurrentMember current, BookingService service, BookingRepository bookings,
                             SlotRepository slots, SlotFormatter formatter, CentreClock centreClock) {
        this.current = current;
        this.service = service;
        this.bookings = bookings;
        this.slots = slots;
        this.formatter = formatter;
        this.centreClock = centreClock;
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<BookingResult> book(@RequestBody BookRequest body) {
        MemberView me = current.require();
        BookingResult result = service.book(me.id(), body.slotId());
        return result.booked()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    /** All of the signed-in member's bookings. "date" is the day the class is on, for grouping. */
    @GetMapping("/api/bookings/mine")
    public List<MyBooking> mine() {
        MemberView me = current.require();
        return bookings.forMember(me.id()).stream()
                .map(b -> {
                    SlotDto slot = slots.find(b.slotId()).map(formatter::toDto).orElseThrow();
                    LocalDate day = slot.startsAt().atZone(centreClock.zone()).toLocalDate();
                    return new MyBooking(b.id(), b.status(), b.amount(), day, slot);
                })
                .sorted((a, b) -> a.slot().startsAt().compareTo(b.slot().startsAt()))
                .toList();
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        MemberView me = current.require();
        double refund = service.cancel(me, id);
        return Map.of("bookingId", id, "refund", refund);
    }
}
