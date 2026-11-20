package com.slotbook.core.slots;

import com.slotbook.contract.SlotView;
import com.slotbook.contract.Slots;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SlotsImpl implements Slots {

    private final SlotRepository slots;

    public SlotsImpl(SlotRepository slots) {
        this.slots = slots;
    }

    @Override
    public Optional<SlotView> find(long slotId) {
        return slots.find(slotId).map(s -> new SlotView(
                s.id(), s.batchName(), s.instructor(), s.startsAt(), s.durationMinutes(), s.capacity(), s.booked()));
    }
}
