package com.slotbook.contract;

import java.time.Instant;
import java.time.ZoneId;

/** The current time, and the time zone the centre runs in. Implemented by core. */
public interface CentreClock {

    Instant now();

    /** The centre's local time zone. Class times, days and "today" mean this zone. */
    ZoneId zone();
}
