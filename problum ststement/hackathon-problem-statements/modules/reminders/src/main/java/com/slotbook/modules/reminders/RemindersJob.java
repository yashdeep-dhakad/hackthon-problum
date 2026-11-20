package com.slotbook.modules.reminders;

import com.slotbook.contract.BookingView;
import com.slotbook.contract.Bookings;
import com.slotbook.contract.CentreClock;
import com.slotbook.contract.Notifier;
import com.slotbook.contract.SlotView;
import com.slotbook.contract.Slots;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class RemindersJob {

    private final JdbcClient jdbc;
    private final ReminderSettings settings;
    private final Bookings bookings;
    private final Slots slots;
    private final Notifier notifier;
    private final CentreClock clock;

    RemindersJob(JdbcClient jdbc, ReminderSettings settings, Bookings bookings, Slots slots, Notifier notifier,
                 CentreClock clock) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.bookings = bookings;
        this.slots = slots;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    void sendDueReminders() {
        Instant now = clock.now();
        for (long memberId : settings.membersWithRemindersOn()) {
            Duration lead = Duration.ofMinutes(settings.forMember(memberId).minutesBefore());
            for (BookingView booking : bookings.confirmedForMember(memberId)) {
                SlotView slot = slots.find(booking.slotId()).orElse(null);
                if (slot == null || slot.startsAt().isBefore(now) || slot.startsAt().isAfter(now.plus(lead))) {
                    continue;
                }
                int firstTime = jdbc.sql("insert into reminders_sent (booking_id) values (:id) on conflict do nothing")
                        .param("id", booking.id())
                        .update();
                if (firstTime == 1) {
                    notifier.notify(memberId, "Reminder: " + slot.batchName() + " starts at "
                            + RemindersListener.WHEN.format(slot.startsAt().atZone(clock.zone())) + ".");
                }
            }
        }
    }
}
