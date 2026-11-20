package com.slotbook.core.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.contract.Topics;
import jakarta.annotation.PreDestroy;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Sends messages to broker topics as JSON text, with the type in the "type" property.
 * Like a real network, delivery is at least once and in no particular order: in development some messages
 * are sent twice and some are held back for a few seconds (slotbook.broker.chaos.*).
 */
@Component
public class BrokerPublisher {

    private final JmsTemplate jms;
    private final ObjectMapper json;
    private final double duplicateChance;
    private final long maxDelayMs;
    private final ScheduledExecutorService later = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "broker-delay");
        t.setDaemon(true);
        return t;
    });

    public BrokerPublisher(JmsTemplate jms, ObjectMapper json,
                           @Value("${slotbook.broker.chaos.duplicate-chance:0}") double duplicateChance,
                           @Value("${slotbook.broker.chaos.max-delay-ms:0}") long maxDelayMs) {
        this.jms = jms;
        this.json = json;
        this.duplicateChance = duplicateChance;
        this.maxDelayMs = maxDelayMs;
    }

    public void send(String topic, String type, Object payload) {
        String body;
        try {
            body = json.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise " + payload, e);
        }
        sendRaw(topic, type, body);
    }

    /** Sends a body as-is, with the same duplication and delay as {@link #send}. */
    public void sendRaw(String topic, String type, String body) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int copies = random.nextDouble() < duplicateChance ? 2 : 1;
        for (int i = 0; i < copies; i++) {
            long delay = maxDelayMs > 0 ? random.nextLong(maxDelayMs + 1) : 0;
            if (delay == 0) {
                deliver(topic, type, body);
            } else {
                later.schedule(() -> deliver(topic, type, body), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void deliver(String topic, String type, String body) {
        jms.send(topic, session -> {
            TextMessage message = session.createTextMessage(body);
            message.setStringProperty(Topics.TYPE_PROPERTY, type);
            return message;
        });
    }

    @PreDestroy
    void stop() {
        later.shutdownNow();
    }
}
