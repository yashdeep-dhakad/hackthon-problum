package com.slotbook.modules.reminders;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class ReminderSettings {

    record Settings(boolean enabled, int minutesBefore) {
    }

    private static final Settings DEFAULT = new Settings(false, 120);

    private final JdbcClient jdbc;

    ReminderSettings(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Settings forMember(long memberId) {
        return jdbc.sql("select enabled, minutes_before from reminders_settings where member_id = :member")
                .param("member", memberId)
                .query((rs, i) -> new Settings(rs.getBoolean("enabled"), rs.getInt("minutes_before")))
                .optional()
                .orElse(DEFAULT);
    }

    void save(long memberId, Settings settings) {
        jdbc.sql("""
                        insert into reminders_settings (member_id, enabled, minutes_before) values (:member, :enabled, :minutes)
                        on conflict (member_id) do update set enabled = excluded.enabled, minutes_before = excluded.minutes_before
                        """)
                .param("member", memberId)
                .param("enabled", settings.enabled())
                .param("minutes", settings.minutesBefore())
                .update();
    }

    List<Long> membersWithRemindersOn() {
        return jdbc.sql("select member_id from reminders_settings where enabled").query(Long.class).list();
    }
}
