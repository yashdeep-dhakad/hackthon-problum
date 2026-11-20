package com.slotbook.core.admin;

import com.slotbook.contract.CentreClock;
import com.slotbook.contract.CurrentMember;
import com.slotbook.core.config.Db;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
public class AdminController {

    public record AttendanceRow(long bookingId, long memberId, String memberName, String email, String status, Instant bookedAt) {
    }

    private final CurrentMember current;
    private final JdbcClient jdbc;
    private final CentreClock centreClock;

    public AdminController(CurrentMember current, JdbcClient jdbc, CentreClock centreClock) {
        this.current = current;
        this.jdbc = jdbc;
        this.centreClock = centreClock;
    }

    /** Everyone with a confirmed booking for a slot, in booking order. */
    @GetMapping("/api/admin/slots/{slotId}/attendance")
    public List<AttendanceRow> attendance(@PathVariable long slotId) {
        current.requireAdmin();
        return jdbc.sql("""
                        select b.id, b.member_id, m.name, m.email, b.status, b.created_at
                        from bookings b join members m on m.id = b.member_id
                        where b.slot_id = :slot and b.status = 'CONFIRMED'
                        order by b.created_at
                        """)
                .param("slot", slotId)
                .query((rs, i) -> new AttendanceRow(rs.getLong("id"), rs.getLong("member_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("status"), Db.instant(rs, "created_at")))
                .list();
    }

    /** Refunds paid out in a month (?month=2026-10), for reconciling with the payment gateway's report. */
    @GetMapping("/api/admin/reports/refunds")
    public Map<String, Object> refunds(@RequestParam String month) {
        current.requireAdmin();
        YearMonth ym = YearMonth.parse(month);
        Instant from = ym.atDay(1).atStartOfDay(centreClock.zone()).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(centreClock.zone()).toInstant();
        List<Double> amounts = jdbc.sql("""
                        select amount from payments
                        where kind = 'REFUND' and created_at >= :from and created_at < :to
                        """)
                .param("from", Db.ts(from))
                .param("to", Db.ts(to))
                .query(Double.class)
                .list();

        BigDecimal total = BigDecimal.ZERO;
        for (double amount : amounts) {
            total = total.add(BigDecimal.valueOf(amount));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        return Map.of("month", month, "refunds", amounts.size(), "total", total.doubleValue());
    }
}
