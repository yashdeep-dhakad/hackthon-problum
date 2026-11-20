package com.slotbook.core.slots;

import com.slotbook.core.config.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class SlotRepository {

    private static final String SELECT = """
            select s.*, b.name as batch_name, b.instructor, b.price,
                   (select count(*) from bookings x where x.slot_id = s.id and x.status = 'CONFIRMED') as booked
            from slots s join batches b on b.id = s.batch_id
            """;

    private static final RowMapper<Slot> SLOT = (rs, i) -> new Slot(
            rs.getLong("id"),
            rs.getLong("batch_id"),
            rs.getString("batch_name"),
            rs.getString("instructor"),
            Db.instant(rs, "starts_at"),
            rs.getInt("duration_minutes"),
            rs.getInt("capacity"),
            rs.getString("notes"),
            rs.getDouble("price"),
            rs.getInt("booked"));

    private final JdbcClient jdbc;

    public SlotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Slot> find(long id) {
        return jdbc.sql(SELECT + " where s.id = :id").param("id", id).query(SLOT).optional();
    }

    public Optional<Slot> findForUpdate(long id) {
        return jdbc.sql(SELECT + " where s.id = :id for update of s").param("id", id).query(SLOT).optional();
    }

    public List<Slot> between(Instant from, Instant to) {
        return jdbc.sql(SELECT + " where s.starts_at >= :from and s.starts_at < :to order by s.starts_at")
                .param("from", Db.ts(from))
                .param("to", Db.ts(to))
                .query(SLOT)
                .list();
    }
}
