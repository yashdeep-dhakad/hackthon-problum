package com.slotbook.core.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.contract.PaymentRecorded;
import com.slotbook.contract.Topics;
import com.slotbook.core.broker.BrokerPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Delivers outbox events to Spring {@code @EventListener}s, at least once, and sends payments to the
 * {@link Topics#PAYMENTS} broker topic.
 * An event is redelivered if a listener throws, and (in development) at random, so modules
 * meet duplicate deliveries early instead of in production.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private record Row(long id, String type, String payload, int attempts) {
    }

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final ApplicationEventPublisher publisher;
    private final BrokerPublisher broker;
    private final boolean autoDispatch;
    private final double redeliverChance;

    public OutboxDispatcher(JdbcClient jdbc, ObjectMapper json, ApplicationEventPublisher publisher, BrokerPublisher broker,
                            @Value("${slotbook.events.auto-dispatch:true}") boolean autoDispatch,
                            @Value("${slotbook.events.redeliver-chance:0}") double redeliverChance) {
        this.jdbc = jdbc;
        this.json = json;
        this.publisher = publisher;
        this.broker = broker;
        this.autoDispatch = autoDispatch;
        this.redeliverChance = redeliverChance;
    }

    @Scheduled(fixedDelayString = "${slotbook.events.dispatch-interval-ms:1000}")
    public void tick() {
        if (autoDispatch) {
            dispatchPending();
        }
    }

    /** Delivers every pending event once. Returns how many deliveries were made. */
    public synchronized int dispatchPending() {
        List<Row> rows = jdbc.sql("""
                        select id, type, payload, attempts from event_outbox
                        where delivered_at is null order by id limit 100
                        """)
                .query((rs, i) -> new Row(rs.getLong("id"), rs.getString("type"), rs.getString("payload"), rs.getInt("attempts")))
                .list();

        int delivered = 0;
        for (Row row : rows) {
            try {
                Object event = json.readValue(row.payload(), Class.forName(row.type()));
                publisher.publishEvent(event);
                if (event instanceof PaymentRecorded payment) {
                    broker.send(Topics.PAYMENTS, "PaymentRecorded", payment);
                }
                delivered++;
            } catch (Exception e) {
                log.warn("Delivering event {} failed; it will be retried: {}", row.id(), e.toString());
                jdbc.sql("update event_outbox set attempts = attempts + 1 where id = :id").param("id", row.id()).update();
                continue;
            }

            boolean deliverAgain = row.attempts() == 0 && ThreadLocalRandom.current().nextDouble() < redeliverChance;
            if (deliverAgain) {
                jdbc.sql("update event_outbox set attempts = attempts + 1 where id = :id").param("id", row.id()).update();
            } else {
                jdbc.sql("update event_outbox set attempts = attempts + 1, delivered_at = now() where id = :id")
                        .param("id", row.id())
                        .update();
            }
        }
        return delivered;
    }
}
