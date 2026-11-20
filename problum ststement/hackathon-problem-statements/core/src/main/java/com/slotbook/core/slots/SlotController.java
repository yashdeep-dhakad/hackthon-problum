package com.slotbook.core.slots;

import com.slotbook.contract.CentreClock;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
public class SlotController {

    private final SlotRepository slots;
    private final SlotFormatter formatter;
    private final CentreClock centreClock;

    public SlotController(SlotRepository slots, SlotFormatter formatter, CentreClock centreClock) {
        this.slots = slots;
        this.formatter = formatter;
        this.centreClock = centreClock;
    }

    /** Slots on one day (?date=2026-11-16) or a range of days (?from=...&to=..., both inclusive). */
    @GetMapping("/api/slots")
    public List<SlotDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (date != null) {
            from = date;
            to = date;
        }
        if (from == null) {
            from = LocalDate.now(centreClock.zone());
        }
        if (to == null) {
            to = from;
        }
        Instant start = from.atStartOfDay(centreClock.zone()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(centreClock.zone()).toInstant();
        return slots.between(start, end).stream().map(formatter::toDto).toList();
    }

    @GetMapping("/api/slots/{id}")
    public SlotDto one(@PathVariable long id) {
        return slots.find(id)
                .map(formatter::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such slot"));
    }
}
