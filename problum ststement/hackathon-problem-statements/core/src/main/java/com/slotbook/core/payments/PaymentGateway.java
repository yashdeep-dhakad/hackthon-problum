package com.slotbook.core.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in for the real payment gateway. Takes about as long as the real one to answer, and keeps its own record
 * of every charge, as the real one does ({@link #ledger()}).
 *
 * Like the real gateway, it can time out (slotbook.payments.timeout-chance, 0 in development): the charge is taken
 * but the answer never arrives. A charge sent with an idempotency key is taken at most once per key: a repeat with
 * the same key returns the original reference straight away and never times out.
 */
@Component
public class PaymentGateway {

    public record Charge(String reference, long memberId, double amount, String idempotencyKey, Instant at) {
    }

    private static final Logger log = LoggerFactory.getLogger(PaymentGateway.class);

    private final long delayMs;
    private final double timeoutChance;
    private final List<Charge> ledger = new ArrayList<>();
    private final Map<String, Charge> byKey = new ConcurrentHashMap<>();

    public PaymentGateway(@Value("${slotbook.payments.gateway-delay-ms:120}") long delayMs,
                          @Value("${slotbook.payments.timeout-chance:0}") double timeoutChance) {
        this.delayMs = delayMs;
        this.timeoutChance = timeoutChance;
    }

    /** Charges a member. Throws {@link GatewayTimeoutException} if the gateway does not answer in time. */
    public String charge(long memberId, double amount) {
        return charge(memberId, amount, null);
    }

    /** Charges a member at most once per idempotency key. */
    public String charge(long memberId, double amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            Charge earlier = byKey.get(idempotencyKey);
            if (earlier != null) {
                return earlier.reference();
            }
        }
        pause(delayMs);
        Charge charge = new Charge("SIM-" + UUID.randomUUID(), memberId, amount, idempotencyKey, Instant.now());
        synchronized (ledger) {
            if (idempotencyKey != null) {
                Charge raced = byKey.putIfAbsent(idempotencyKey, charge);
                if (raced != null) {
                    return raced.reference();
                }
            }
            ledger.add(charge);
        }
        log.debug("Charged member {} Rs {} ({})", memberId, amount, charge.reference());
        if (ThreadLocalRandom.current().nextDouble() < timeoutChance) {
            pause(delayMs);
            throw new GatewayTimeoutException("Payment gateway timed out charging member " + memberId);
        }
        return charge.reference();
    }

    public String refund(long bookingId, double amount) {
        pause(delayMs / 2);
        String reference = "SIM-" + UUID.randomUUID();
        log.debug("Refunded booking {} Rs {} ({})", bookingId, amount, reference);
        return reference;
    }

    /** Every charge the gateway has taken, oldest first. */
    public List<Charge> ledger() {
        synchronized (ledger) {
            return List.copyOf(ledger);
        }
    }

    private static void pause(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
