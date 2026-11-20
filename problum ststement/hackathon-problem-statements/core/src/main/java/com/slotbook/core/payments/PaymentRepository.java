package com.slotbook.core.payments;

import com.slotbook.contract.PaymentRecorded;
import com.slotbook.core.events.EventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Repository
public class PaymentRepository {

    private final JdbcClient jdbc;
    private final EventPublisher events;
    private final Clock clock;

    public PaymentRepository(JdbcClient jdbc, EventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
    }

    /** Records a charge or refund and announces it on the payments topic. */
    public void record(long bookingId, String kind, double amount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into payments (booking_id, kind, amount) values (:booking, :kind, :amount)")
                .param("booking", bookingId)
                .param("kind", kind)
                .param("amount", amount)
                .update(keys, "id");
        long paymentId = keys.getKey().longValue();

        Map<String, Object> booking = jdbc.sql("select slot_id, member_id from bookings where id = :id")
                .param("id", bookingId)
                .query()
                .singleRow();
        events.publish(new PaymentRecorded(UUID.randomUUID(), paymentId, bookingId,
                ((Number) booking.get("slot_id")).longValue(), ((Number) booking.get("member_id")).longValue(),
                kind, BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString(), clock.instant()));
    }
}
