package com.slotbook.contract;

import java.util.Optional;

/** Read access to members. Implemented by core. */
public interface Members {

    Optional<MemberView> find(long memberId);
}
