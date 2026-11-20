package com.slotbook.core.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.contract.BookingCancelled;
import com.slotbook.contract.BookingConfirmed;
import com.slotbook.contract.PaymentRecorded;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Writes events to the outbox. {@link OutboxDispatcher} delivers them to listeners shortly afterwards. */
@Component
public class EventPublisher {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public EventPublisher(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void publish(BookingConfirmed event) {
        write(event.eventId(), event);
    }

    public void publish(BookingCancelled event) {
        write(event.eventId(), event);
    }

    public void publish(PaymentRecorded event) {
        write(event.eventId(), event);
    }

    private void write(UUID eventId, Object event) {
        try {
            jdbc.sql("insert into event_outbox (event_id, type, payload) values (:id, :type, :payload)")
                    .param("id", eventId)
                    .param("type", event.getClass().getName())
                    .param("payload", json.writeValueAsString(event))
                    .update();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise " + event, e);
        }
    }
}
