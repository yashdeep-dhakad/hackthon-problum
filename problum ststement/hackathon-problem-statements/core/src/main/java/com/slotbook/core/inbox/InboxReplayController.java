package com.slotbook.core.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.MemberMessage;
import com.slotbook.contract.Topics;
import com.slotbook.core.broker.BrokerPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Front-desk inbox. Messages members send arrive on {@link Topics#INBOX}. For development, an admin can replay a file
 * of messages: POST the file's lines (one JSON object per line: {"id": "m007", "memberEmail": "...", "text": "..."}) as the body.
 * A message's messageId is the number in its id ("m007" is 7); lines without an id are numbered in order.
 */
@RestController
public class InboxReplayController {

    private final CurrentMember current;
    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final BrokerPublisher broker;
    private final AtomicLong nextId = new AtomicLong(1);

    public InboxReplayController(CurrentMember current, JdbcClient jdbc, ObjectMapper json, BrokerPublisher broker) {
        this.current = current;
        this.jdbc = jdbc;
        this.json = json;
        this.broker = broker;
    }

    @PostMapping(value = "/api/admin/inbox/replay", consumes = "*/*")
    public Map<String, Object> replay(@RequestBody String lines) {
        current.requireAdmin();
        int sent = 0;
        int skipped = 0;
        for (String line : lines.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode row;
            try {
                row = json.readTree(line);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not JSON: " + line);
            }
            Optional<Long> member = jdbc.sql("select id from members where lower(email) = lower(:e)")
                    .param("e", row.path("memberEmail").asText()).query(Long.class).optional();
            if (member.isEmpty() || row.path("text").asText().isBlank()) {
                skipped++;
                continue;
            }
            // Number messages to match the file ("m007" is message 7), so decisions can be checked against it later.
            String digits = row.path("id").asText().replaceAll("\\D", "");
            long messageId = digits.isEmpty() ? nextId.getAndIncrement() : Long.parseLong(digits);
            broker.send(Topics.INBOX, "MemberMessage", new MemberMessage(UUID.randomUUID(), messageId,
                    member.get(), row.path("text").asText(), Instant.now()));
            sent++;
        }
        return Map.of("sent", sent, "skipped", skipped);
    }
}
