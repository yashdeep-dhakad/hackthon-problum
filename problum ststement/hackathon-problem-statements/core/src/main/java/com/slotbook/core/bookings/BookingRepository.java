package com.slotbook.core.bookings;

import com.slotbook.core.config.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepository {

    private static final RowMapper<Booking> BOOKING = (rs, i) -> new Booking(
            rs.getLong("id"),
            rs.getLong("slot_id"),
            rs.getLong("member_id"),
            rs.getString("status"),
            rs.getDouble("amount"),
            Db.instant(rs, "created_at"),
            Db.instant(rs, "cancelled_at"));

    private final JdbcClient jdbc;

    public BookingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Booking> find(long id) {
        return jdbc.sql("select * from bookings where id = :id").param("id", id).query(BOOKING).optional();
    }

    public List<Booking> forMember(long memberId) {
        return jdbc.sql("select * from bookings where member_id = :member order by created_at desc")
                .param("member", memberId)
                .query(BOOKING)
                .list();
    }

    public int countConfirmed(long slotId) {
        return jdbc.sql("select count(*) from bookings where slot_id = :slot and status = 'CONFIRMED'")
                .param("slot", slotId)
                .query(Integer.class)
                .single();
    }

    public boolean hasConfirmed(long memberId, long slotId) {
        return jdbc.sql("select count(*) from bookings where slot_id = :slot and member_id = :member and status = 'CONFIRMED'")
                .param("slot", slotId)
                .param("member", memberId)
                .query(Integer.class)
                .single() > 0;
    }

    public long insert(long slotId, long memberId, double amount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into bookings (slot_id, member_id, status, amount) values (:slot, :member, 'CONFIRMED', :amount)")
                .param("slot", slotId)
                .param("member", memberId)
                .param("amount", amount)
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    public void markCancelled(long id, Instant at) {
        jdbc.sql("update bookings set status = 'CANCELLED', cancelled_at = :at where id = :id")
                .param("id", id)
                .param("at", Db.ts(at))
                .update();
    }
}
