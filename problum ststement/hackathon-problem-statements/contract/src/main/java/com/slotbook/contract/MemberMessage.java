package com.slotbook.contract;

import java.time.Instant;
import java.util.UUID;

/** A message a member sent the front desk. Sent on {@link Topics#INBOX} as JSON, JMS property type = "MemberMessage". */
public record MemberMessage(UUID eventId, long messageId, long memberId, String text, Instant receivedAt) {
}
