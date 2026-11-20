package com.slotbook.core.members;

import com.slotbook.contract.CentreClock;
import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.MemberView;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
public class MeController {

    private final CurrentMember current;
    private final MemberRepository members;
    private final JdbcClient jdbc;
    private final CentreClock centreClock;

    public MeController(CurrentMember current, MemberRepository members, JdbcClient jdbc, CentreClock centreClock) {
        this.current = current;
        this.members = members;
        this.jdbc = jdbc;
        this.centreClock = centreClock;
    }

    @GetMapping("/api/me")
    public MemberView me() {
        return current.require();
    }

    @GetMapping("/api/me/plan")
    public Map<String, Object> myPlan() {
        MemberView me = current.require();
        Member member = members.findById(me.id()).orElseThrow();
        Plan plan = (member.planId() != null ? members.findPlan(member.planId()) : Optional.<Plan>empty())
                .or(members::findDefaultPlan)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No plan found"));

        LocalDate firstOfMonth = LocalDate.now(centreClock.zone()).withDayOfMonth(1);
        Instant from = firstOfMonth.atStartOfDay(centreClock.zone()).toInstant();
        Instant to = firstOfMonth.plusMonths(1).atStartOfDay(centreClock.zone()).toInstant();
        int used = jdbc.sql("""
                        select count(*) from bookings b join slots s on s.id = b.slot_id
                        where b.member_id = :member and b.status = 'CONFIRMED'
                          and s.starts_at >= :from and s.starts_at < :to
                        """)
                .param("member", me.id())
                .param("from", com.slotbook.core.config.Db.ts(from))
                .param("to", com.slotbook.core.config.Db.ts(to))
                .query(Integer.class)
                .single();

        return Map.of(
                "plan", plan.name(),
                "classesPerMonth", plan.classesPerMonth(),
                "monthlyPrice", plan.monthlyPrice(),
                "classesUsedThisMonth", used);
    }
}
