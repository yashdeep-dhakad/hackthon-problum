package com.slotbook.core.config;

import com.slotbook.contract.CentreClock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Server time is UTC everywhere. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CentreClock centreClock(Clock clock, @Value("${slotbook.centre-zone}") String zone) {
        ZoneId centreZone = ZoneId.of(zone);
        return new CentreClock() {
            @Override
            public Instant now() {
                return clock.instant();
            }

            @Override
            public ZoneId zone() {
                return centreZone;
            }
        };
    }
}
