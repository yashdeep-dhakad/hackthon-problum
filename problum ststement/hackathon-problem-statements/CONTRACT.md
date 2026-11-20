# The module contract

A module adds a feature to SlotBook without core knowing it exists. This page is the rulebook. A module that breaks a rule here is wrong, even if it works on your screen and passes the tests you can see.

## What a module is

- A folder `modules/<name>/`, built as its own Maven artifact.
- It compiles against `contract/` and nothing else from SlotBook. Spring Boot (web, JDBC) is provided by the host.
- It registers itself in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, naming its configuration class. The host loads every module on its classpath; core never refers to one.
- Its own tables come from Flyway migrations in `src/main/resources/db/modules/`.
- Its UI, if any, lives in `web/src/modules/<name>/` and is registered with one entry in `web/src/modules/index.js`.

`modules/reminders/` is a complete example.

## What core gives you

Inject these (all in `com.slotbook.contract`):

| Interface | Use it to |
| --- | --- |
| `CurrentMember` | Find out who is calling your endpoint (`require()`), or insist on an admin (`requireAdmin()`) |
| `Slots` | Look up a class slot: time, capacity, seats taken |
| `Bookings` | Book a seat for a member (charges them exactly as a normal booking does), cancel a member's own booking (refunds under the policy), look up bookings |
| `Members` | Look up a member |
| `Notifier` | Send a member a notification |
| `CentreClock` | The current time, and the centre's time zone |

Core publishes these events. Receive them with Spring's `@EventListener`:

| Event | When |
| --- | --- |
| `BookingConfirmed` | A seat was booked and paid for |
| `BookingCancelled` | A booking was cancelled and its seat freed |

You may also inject Spring's `JdbcClient` (or `DataSource`) for your own tables.

## Messages

Core also publishes to a message broker (Apache ActiveMQ Artemis, running inside the app). Unlike the Spring events above, a topic gives every subscribing module its own copy of every message.

| Topic | Message |
| --- | --- |
| `Topics.PAYMENTS` | `PaymentRecorded`, one per charge or refund, as JSON text with the JMS property `type` = `"PaymentRecorded"` |
| `Topics.INBOX` | `MemberMessage`, one per message a member sends the front desk, with `type` = `"MemberMessage"` |

Subscribe like this, using your module's name as the subscription:

```java
@JmsListener(destination = Topics.PAYMENTS, containerFactory = Topics.LISTENER_FACTORY, subscription = "settlement")
void on(jakarta.jms.Message message) { ... }
```

If your listener throws, the broker delivers the message again (up to 5 attempts, then it goes to the `DLQ` queue). If it returns normally, the message is gone for good.

## Rules

1. **Compile against `contract/` only.** Never import `com.slotbook.core` in module code. (Tests may use core's test helpers.)
2. **Own your data.** Your tables start with your module's name (`waitlist_...`) and are created by your migrations. Migration versions are per module: reminders uses `100.x`, waitlist uses `200.x` (for example `V200_1__waitlist.sql`). Never read or write core's tables (`members`, `slots`, `bookings`, `payments`, `notifications`, and the rest); use the interfaces above.
3. **Events and messages arrive at least once, in any order.** The same event or message, with the same `eventId`, can be delivered more than once. In development the host redelivers about one event in five on purpose, and the broker repeats some messages and holds others back for a few seconds, so a refund can arrive before its charge. Handling one twice must have exactly the same effect as handling it once.
4. **Members' details are private.** A member may see their own name, email and bookings. Never show one member's name, email or bookings to another member. Admins may see everything.
5. **Every endpoint checks who is calling.** Module endpoints live under `/api/modules/<name>/`. Each one calls `CurrentMember.require()`, or `requireAdmin()` for admin endpoints, before doing anything.
6. **Money moves only through core.** To give someone a seat, call `Bookings.book`; it charges them. Never create bookings or payments any other way.
7. **Time means the centre's time.** Days, "today" and times shown to people are in `CentreClock.zone()`.
8. **Talk to members only through `Notifier`.**
9. **Stay in your folder.** A module changes nothing outside `modules/<name>/`, `web/src/modules/<name>/` and its one line in `web/src/modules/index.js`.

## Testing a module

Module tests run inside the real host with a fresh database. Annotate a test class with `@SlotBookTest`, inject `TestHost` and `MockMvc`, and use `TestHost` to create members and slots, book, cancel, sign in (`auth(member)` gives an `Authorization` header value) and deliver pending events (`deliverEvents()`). Events are not delivered automatically in tests. See `modules/reminders/src/test/` for an example.
