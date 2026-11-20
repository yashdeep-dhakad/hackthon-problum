package com.slotbook.modules.reminders;

import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.MemberView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/modules/reminders")
class RemindersController {

    private final CurrentMember current;
    private final ReminderSettings settings;

    RemindersController(CurrentMember current, ReminderSettings settings) {
        this.current = current;
        this.settings = settings;
    }

    @GetMapping("/settings")
    ReminderSettings.Settings get() {
        MemberView me = current.require();
        return settings.forMember(me.id());
    }

    @PutMapping("/settings")
    ReminderSettings.Settings put(@RequestBody ReminderSettings.Settings body) {
        MemberView me = current.require();
        if (body.minutesBefore() < 15 || body.minutesBefore() > 24 * 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minutesBefore must be between 15 and 1440");
        }
        settings.save(me.id(), body);
        return settings.forMember(me.id());
    }
}
