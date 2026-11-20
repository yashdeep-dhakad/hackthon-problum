package com.slotbook.core.notifications;

import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.MemberView;
import com.slotbook.core.config.Db;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class NotificationController {

    public record NotificationDto(long id, String message, Instant createdAt, boolean read) {
    }

    private final CurrentMember current;
    private final JdbcClient jdbc;

    public NotificationController(CurrentMember current, JdbcClient jdbc) {
        this.current = current;
        this.jdbc = jdbc;
    }

    @GetMapping("/api/notifications")
    public List<NotificationDto> mine() {
        MemberView me = current.require();
        return jdbc.sql("select * from notifications where member_id = :member order by created_at desc, id desc limit 50")
                .param("member", me.id())
                .query((rs, i) -> new NotificationDto(
                        rs.getLong("id"), rs.getString("message"), Db.instant(rs, "created_at"), rs.getBoolean("is_read")))
                .list();
    }

    @PostMapping("/api/notifications/read")
    public void markAllRead() {
        MemberView me = current.require();
        jdbc.sql("update notifications set is_read = true where member_id = :member").param("member", me.id()).update();
    }
}
