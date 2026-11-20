package com.slotbook.core.slots;

import com.slotbook.contract.CentreClock;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class SlotFormatter {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH);

    private final CentreClock centre;

    public SlotFormatter(CentreClock centre) {
        this.centre = centre;
    }

    public SlotDto toDto(Slot s) {
        return new SlotDto(s.id(), s.batchId(), s.batchName(), s.instructor(), s.startsAt(),
                LOCAL.format(s.startsAt().atZone(centre.zone())),
                s.durationMinutes(), s.capacity(), s.booked(), s.price(), s.notes());
    }
}
