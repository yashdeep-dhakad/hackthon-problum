package com.slotbook.chaos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotbook.contract.PaymentRecorded;
import com.slotbook.contract.Topics;
import com.slotbook.core.SlotBookApplication;
import com.slotbook.core.broker.BrokerPublisher;
import com.slotbook.core.testing.TestHost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The settlement chaos run (wave 2). About 5 minutes. Run it with ./chaos.sh (or chaos.cmd).
 *
 * Sends 600 payments through a broker that repeats some messages and delays others by up to 8 seconds,
 * while some database statements on settlement tables fail. Then checks that every payment was settled
 * exactly once and every batch adds up. It reports every problem it finds, not just the first.
 */
@SpringBootTest(classes = SlotBookApplication.class, properties = {
        "slotbook.events.auto-dispatch=false",
        "slotbook.events.redeliver-chance=0",
        "slotbook.payments.gateway-delay-ms=0",
        "slotbook.broker.chaos.duplicate-chance=0.2",
        "slotbook.broker.chaos.max-delay-ms=8000",
        "slotbook.chaos.db-failure-chance=0.03",
        "slotbook.settlement.interval-seconds=15"
})
@AutoConfigureMockMvc
class SettlementChaosRun {

    static final int PAYMENTS = 600;
    static final long SPACING_MS = 250;
    static final Duration DRAIN = Duration.ofMinutes(3);
    static final String BASE = "/api/modules/settlement/admin";

    record Sent(UUID id, String instructor, String kind, BigDecimal amount) {
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    TestHost host;

    @Autowired
    BrokerPublisher broker;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ObjectMapper json;

    String admin;
    final List<String> problems = new ArrayList<>();
    final Map<Long, String> firstSeen = new HashMap<>();

    @Test
    void everyPaymentIsSettledExactlyOnce() throws Exception {
        admin = host.auth(host.admin());
        String run = UUID.randomUUID().toString().substring(0, 6);
        String[] instructors = {"Chaos Instructor A " + run, "Chaos Instructor B " + run, "Chaos Instructor C " + run, "Chaos Instructor D " + run};
        String[] prices = {"135.45", "333.33", "129.95", "599.00"};
        long[] slots = new long[instructors.length];
        for (int i = 0; i < instructors.length; i++) {
            slots[i] = newSlot(instructors[i], prices[i]);
        }
        long payer = host.newMember("Chaos Payer " + run).id();

        if (call(post(BASE + "/settle")).getStatus() == 404) {
            fail("POST " + BASE + "/settle not found: build the settlement module first (wave 2 ticket)");
        }

        Random random = new Random();
        Map<UUID, Sent> sent = new HashMap<>();
        List<Object[]> refundable = new ArrayList<>(); // {bookingId, slotIndex, amount}
        for (int k = 0; k < PAYMENTS; k++) {
            PaymentRecorded payment;
            int slot;
            if (k > 20 && !refundable.isEmpty() && random.nextDouble() < 0.25) {
                Object[] charge = refundable.remove(random.nextInt(refundable.size()));
                slot = (int) charge[1];
                BigDecimal share = random.nextBoolean() ? new BigDecimal("0.9") : new BigDecimal("0.5");
                BigDecimal amount = ((BigDecimal) charge[2]).multiply(share).setScale(2, RoundingMode.HALF_UP);
                payment = new PaymentRecorded(UUID.randomUUID(), 0, (long) charge[0], slots[slot], payer, "REFUND",
                        amount.toPlainString(), Instant.now());
            } else {
                slot = random.nextInt(slots.length);
                BigDecimal amount = new BigDecimal(prices[slot]);
                long booking = newBooking(slots[slot], payer, amount);
                refundable.add(new Object[]{booking, slot, amount});
                payment = new PaymentRecorded(UUID.randomUUID(), 0, booking, slots[slot], payer, "CHARGE",
                        amount.toPlainString(), Instant.now());
            }
            broker.send(Topics.PAYMENTS, "PaymentRecorded", payment);
            sent.put(payment.eventId(), new Sent(payment.eventId(), instructors[slot], payment.kind(), new BigDecimal(payment.amount())));
            Thread.sleep(SPACING_MS);
            if (k % 50 == 49) {
                snapshotBatches();
            }
        }

        Instant deadline = Instant.now().plus(DRAIN);
        Map<Long, JsonNode> batches;
        do {
            Thread.sleep(5000);
            call(post(BASE + "/settle"));
            batches = readBatches();
        } while (!settledIds(batches).containsAll(sent.keySet()) && Instant.now().isBefore(deadline));

        check(sent, batches);
        if (!problems.isEmpty()) {
            fail(problems.size() + " problem(s) found:\n  - " + String.join("\n  - ", problems.subList(0, Math.min(40, problems.size()))));
        }
    }

    void check(Map<UUID, Sent> sent, Map<Long, JsonNode> batches) {
        Map<UUID, List<Long>> where = new HashMap<>();
        for (var entry : batches.entrySet()) {
            for (JsonNode id : entry.getValue().path("paymentEventIds")) {
                UUID eventId = UUID.fromString(id.asText());
                where.computeIfAbsent(eventId, x -> new ArrayList<>()).add(entry.getKey());
                if (!sent.containsKey(eventId)) {
                    problems.add("batch " + entry.getKey() + " lists payment " + eventId + ", which was never sent");
                }
            }
        }
        long missing = sent.keySet().stream().filter(id -> !where.containsKey(id)).count();
        if (missing > 0) {
            problems.add(missing + " of " + sent.size() + " payments were never settled");
        }
        where.forEach((id, in) -> {
            if (in.size() > 1) {
                problems.add("payment " + id + " was settled " + in.size() + " times, in batches " + in);
            }
        });

        for (var entry : batches.entrySet()) {
            long batchId = entry.getKey();
            Map<String, BigDecimal[]> expected = new TreeMap<>();
            for (JsonNode id : entry.getValue().path("paymentEventIds")) {
                Sent s = sent.get(UUID.fromString(id.asText()));
                if (s == null) {
                    continue;
                }
                BigDecimal[] sums = expected.computeIfAbsent(s.instructor(), x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                if (s.kind().equals("CHARGE")) {
                    sums[0] = sums[0].add(s.amount());
                } else {
                    sums[1] = sums[1].add(s.amount());
                }
            }
            Map<String, JsonNode> lines = new HashMap<>();
            for (JsonNode line : entry.getValue().path("lines")) {
                lines.put(line.path("instructor").asText(), line);
            }
            expected.forEach((instructor, sums) -> {
                JsonNode line = lines.get(instructor);
                if (line == null) {
                    problems.add("batch " + batchId + " has payments for " + instructor + " but no line for them");
                    return;
                }
                compare(batchId, instructor, "charges", sums[0], line.path("charges"));
                compare(batchId, instructor, "refunds", sums[1], line.path("refunds"));
            });
            lines.keySet().stream().filter(i -> i.startsWith("Chaos Instructor") && !expected.containsKey(i))
                    .forEach(i -> problems.add("batch " + batchId + " has a line for " + i + " but none of their payments"));

            String before = firstSeen.get(batchId);
            if (before != null && !before.equals(canonical(entry.getValue()))) {
                problems.add("batch " + batchId + " changed after it was closed");
            }
        }
    }

    void compare(long batchId, String instructor, String field, BigDecimal expected, JsonNode actual) {
        BigDecimal value = decimal(actual);
        if (value == null || value.compareTo(expected) != 0) {
            problems.add("batch " + batchId + ", " + instructor + ": " + field + " is " + actual
                    + " but that batch's payments add up to " + expected.toPlainString());
        }
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    long newSlot(String instructor, String price) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into batches (name, instructor, price) values (:name, :instructor, :price)")
                .param("name", "Chaos Batch").param("instructor", instructor).param("price", Double.parseDouble(price))
                .update(keys, "id");
        long batch = keys.getKey().longValue();
        keys = new GeneratedKeyHolder();
        jdbc.sql("insert into slots (batch_id, starts_at, duration_minutes, capacity) values (:b, :at, 60, 100000)")
                .param("b", batch).param("at", OffsetDateTime.now(ZoneOffset.UTC).plusDays(5))
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    long newBooking(long slot, long member, BigDecimal amount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into bookings (slot_id, member_id, status, amount) values (:s, :m, 'CONFIRMED', :a)")
                .param("s", slot).param("m", member).param("a", amount.doubleValue())
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    void snapshotBatches() throws Exception {
        readBatches().forEach((id, batch) -> firstSeen.putIfAbsent(id, canonical(batch)));
    }

    Map<Long, JsonNode> readBatches() throws Exception {
        Map<Long, JsonNode> out = new TreeMap<>();
        MockHttpServletResponse list = call(get(BASE + "/batches"));
        if (list.getStatus() != 200) {
            return out;
        }
        for (JsonNode b : json.readTree(list.getContentAsString())) {
            long id = b.path("id").asLong();
            MockHttpServletResponse detail = call(get(BASE + "/batches/" + id));
            if (detail.getStatus() == 200) {
                out.put(id, json.readTree(detail.getContentAsString()));
            }
        }
        return out;
    }

    Set<UUID> settledIds(Map<Long, JsonNode> batches) {
        Set<UUID> ids = new HashSet<>();
        batches.values().forEach(b -> b.path("paymentEventIds").forEach(id -> ids.add(UUID.fromString(id.asText()))));
        return ids;
    }

    String canonical(JsonNode batch) {
        List<String> ids = new ArrayList<>();
        batch.path("paymentEventIds").forEach(id -> ids.add(id.asText()));
        ids.sort(null);
        List<String> lines = new ArrayList<>();
        batch.path("lines").forEach(l -> lines.add(l.path("instructor").asText() + "|" + norm(l.path("charges"))
                + "|" + norm(l.path("refunds")) + "|" + norm(l.path("payout"))));
        lines.sort(null);
        return ids + " " + lines;
    }

    static String norm(JsonNode n) {
        BigDecimal d = decimal(n);
        return d == null ? n.toString() : d.stripTrailingZeros().toPlainString();
    }

    static BigDecimal decimal(JsonNode n) {
        try {
            return n == null || n.isMissingNode() || n.isNull() ? null : new BigDecimal(n.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** One HTTP call as the admin, retried while the (chaotic) database makes it fail. */
    MockHttpServletResponse call(RequestBuilder request) throws Exception {
        MockHttpServletResponse response = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                response = mvc.perform(withAuth(request)).andReturn().getResponse();
                if (response.getStatus() < 500) {
                    return response;
                }
            } catch (Exception e) {
                // a request that throws inside MockMvc is the same as a 500 here
            }
            Thread.sleep(500);
        }
        return response == null ? new MockHttpServletResponse() {{ setStatus(500); }} : response;
    }

    RequestBuilder withAuth(RequestBuilder request) {
        return servletContext -> {
            var r = request.buildRequest(servletContext);
            r.addHeader("Authorization", admin);
            return r;
        };
    }
}
