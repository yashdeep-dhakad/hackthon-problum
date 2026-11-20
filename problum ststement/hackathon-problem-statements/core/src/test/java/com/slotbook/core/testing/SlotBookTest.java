package com.slotbook.core.testing;

import com.slotbook.core.SlotBookApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a test against the whole app (core plus every module on the classpath) with a fresh embedded database.
 * Events are not delivered automatically: call {@link TestHost#deliverEvents()} when you want them delivered.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = SlotBookApplication.class, properties = {
        "slotbook.events.auto-dispatch=false",
        "slotbook.events.redeliver-chance=0",
        "slotbook.payments.gateway-delay-ms=0",
        "slotbook.broker.chaos.duplicate-chance=0",
        "slotbook.broker.chaos.max-delay-ms=0"
})
@AutoConfigureMockMvc
public @interface SlotBookTest {
}
