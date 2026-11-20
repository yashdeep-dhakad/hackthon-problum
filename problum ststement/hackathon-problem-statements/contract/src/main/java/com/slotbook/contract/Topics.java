package com.slotbook.contract;

/**
 * Broker topics core publishes to. Every module that subscribes gets its own copy of every message.
 * Listen with {@code @JmsListener(destination = Topics.PAYMENTS, containerFactory = Topics.LISTENER_FACTORY,
 * subscription = "<your module name>")}. See CONTRACT.md, "Messages".
 */
public final class Topics {

    /** One {@link PaymentRecorded} per charge or refund. */
    public static final String PAYMENTS = "slotbook.payments";

    /** One {@link MemberMessage} per message a member sends the front desk. */
    public static final String INBOX = "slotbook.inbox";

    /** The listener container factory core provides for topic subscriptions. */
    public static final String LISTENER_FACTORY = "slotbookTopicListener";

    /** JMS string property on every message naming its type, for example "PaymentRecorded". */
    public static final String TYPE_PROPERTY = "type";

    private Topics() {
    }
}
