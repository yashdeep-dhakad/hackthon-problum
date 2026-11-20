package com.slotbook.contract;

import java.util.Optional;

/** Read access to class slots. Implemented by core. */
public interface Slots {

    Optional<SlotView> find(long slotId);
}
