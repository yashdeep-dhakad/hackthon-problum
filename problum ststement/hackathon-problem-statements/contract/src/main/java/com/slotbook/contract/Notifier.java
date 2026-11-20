package com.slotbook.contract;

/** Sends a member a notification (shown on their Notifications page). Implemented by core. */
public interface Notifier {

    void notify(long memberId, String message);
}
