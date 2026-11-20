package com.slotbook.core.notifications;

import com.slotbook.contract.Notifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements Notifier {

    private final JdbcClient jdbc;

    public NotificationService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void notify(long memberId, String message) {
        jdbc.sql("insert into notifications (member_id, message) values (:member, :message)")
                .param("member", memberId)
                .param("message", message)
                .update();
    }
}
