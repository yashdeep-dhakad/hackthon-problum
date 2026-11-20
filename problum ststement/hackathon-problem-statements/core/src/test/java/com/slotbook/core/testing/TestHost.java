package com.slotbook.core.testing;

import com.slotbook.contract.BookingResult;
import com.slotbook.contract.Bookings;
import com.slotbook.contract.MemberView;
import com.slotbook.contract.Members;
import com.slotbook.core.bookings.BookingService;
import com.slotbook.core.config.Db;
import com.slotbook.core.events.OutboxDispatcher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Helpers for tests: make members and slots, book, cancel, and deliver events on demand. */
@Component
public class TestHost {

    /** Batch used for test slots: Maths Olympiad, Rs 450.00. */
    public static final long TEST_BATCH = 4;

    private final JdbcClient jdbc;
    private final Bookings bookings;
    private final BookingService bookingService;
    private final Members members;
    private final OutboxDispatcher dispatcher;

    public TestHost(JdbcClient jdbc, Bookings bookings, BookingService bookingService, Members members,
                    OutboxDispatcher dispatcher) {
        this.jdbc = jdbc;
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.members = members;
        this.dispatcher = dispatcher;
    }

    public MemberView newMember(String name) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into members (name, email, password_hash, plan_id) values (:name, :email, 'x', 1)")
                .param("name", name)
                .param("email", name.toLowerCase().replace(' ', '.') + "." + UUID.randomUUID() + "@test.local")
                .update(keys, "id");
        return members.find(keys.getKey().longValue()).orElseThrow();
    }

    public MemberView admin() {
        return members.find(1).orElseThrow();
    }

    /** A value for the Authorization header, signed in as this member. */
    public String auth(MemberView member) {
        String token = UUID.randomUUID().toString();
        jdbc.sql("insert into sessions (token, member_id) values (:token, :member)")
                .param("token", token)
                .param("member", member.id())
                .update();
        return "Bearer " + token;
    }

    /** A slot three days from now. */
    public long newSlot(int capacity) {
        return newSlot(capacity, Instant.now().plus(Duration.ofDays(3)));
    }

    public long newSlot(int capacity, Instant startsAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into slots (batch_id, starts_at, duration_minutes, capacity) values (:batch, :at, 60, :cap)")
                .param("batch", TEST_BATCH)
                .param("at", Db.ts(startsAt))
                .param("cap", capacity)
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    public long book(MemberView member, long slotId) {
        BookingResult result = bookings.book(member.id(), slotId);
        if (!result.booked()) {
            throw new IllegalStateException("Could not book: " + result.outcome());
        }
        return result.bookingId();
    }

    /** Books new members into the slot until it is full. Returns their bookings' ids. */
    public List<Long> fill(long slotId) {
        List<Long> ids = new ArrayList<>();
        int capacity = jdbc.sql("select capacity from slots where id = :id").param("id", slotId).query(Integer.class).single();
        while (confirmedCount(slotId) < capacity) {
            ids.add(book(newMember("Filler " + ids.size()), slotId));
        }
        return ids;
    }

    /** Cancels a booking as an admin would. Returns the refund. */
    public double cancel(long bookingId) {
        return bookingService.cancel(admin(), bookingId);
    }

    /** Delivers every pending event to listeners. Returns the number delivered. */
    public int deliverEvents() {
        return dispatcher.dispatchPending();
    }

    public int confirmedCount(long slotId) {
        return jdbc.sql("select count(*) from bookings where slot_id = :slot and status = 'CONFIRMED'")
                .param("slot", slotId)
                .query(Integer.class)
                .single();
    }

    public boolean hasConfirmedBooking(MemberView member, long slotId) {
        return jdbc.sql("select count(*) from bookings where slot_id = :slot and member_id = :member and status = 'CONFIRMED'")
                .param("slot", slotId)
                .param("member", member.id())
                .query(Integer.class)
                .single() > 0;
    }

    public List<String> notificationsFor(MemberView member) {
        return jdbc.sql("select message from notifications where member_id = :member order by id")
                .param("member", member.id())
                .query(String.class)
                .list();
    }
}
