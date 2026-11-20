package com.slotbook.modules.reminders;

import com.slotbook.contract.BookingConfirmed;
import com.slotbook.contract.CentreClock;
import com.slotbook.contract.Notifier;
import com.slotbook.contract.Slots;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
class RemindersListener {

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH);

    private final JdbcClient jdbc;
    private final ReminderSettings settings;
    private final Slots slots;
    private final Notifier notifier;
    private final CentreClock clock;

    RemindersListener(JdbcClient jdbc, ReminderSettings settings, Slots slots, Notifier notifier, CentreClock clock) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.slots = slots;
        this.notifier = notifier;
        this.clock = clock;
    }

    @EventListener
    void on(BookingConfirmed event) {
        ReminderSettings.Settings mine = settings.forMember(event.memberId());
        if (!mine.enabled()) {
            return;
        }
        int firstTime = jdbc.sql("insert into reminders_confirmations (event_id) values (:id) on conflict do nothing")
                .param("id", event.eventId())
                .update();
        if (firstTime == 0) {
            return;
        }
        slots.find(event.slotId()).ifPresent(slot -> notifier.notify(event.memberId(),
                "Booked: " + slot.batchName() + ", " + WHEN.format(slot.startsAt().atZone(clock.zone()))
                        + ". We'll remind you " + mine.minutesBefore() + " minutes before."));
    }
}
