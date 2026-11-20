# Status

## T1.1
- **What I found**:
  The application server runs with UTC clock (`Clock.systemUTC()`), whereas the coaching centre operates in `Asia/Kolkata` (`slotbook.centre-zone: Asia/Kolkata`, exposed via `CentreClock`).
  In `BookingController.mine()`, the grouping date was computed as `slot.startsAt().atZone(clock.getZone()).toLocalDate()`. Because `clock.getZone()` is UTC, Monday's 5:00 AM IST slot (`23:30:00 UTC` Sunday) was given Sunday's date.
  In `SlotController.list()`, date boundaries were calculated as `from.atStartOfDay(clock.getZone()).toInstant()`, where Monday 00:00 UTC equals Monday 05:30 IST. Consequently, Monday's 5:00 AM IST slot was excluded from Monday's "Today" query, while Tuesday's 5:00 AM IST slot (`Monday 23:30:00 UTC`) was included.
- **What I did**:
  1. Updated `BookingController.java` to inject `CentreClock` and compute `LocalDate day = slot.startsAt().atZone(centreClock.zone()).toLocalDate()`.
  2. Updated `SlotController.java` to inject `CentreClock` and compute start/end day bounds in `centreClock.zone()`, defaulting `from` to `LocalDate.now(centreClock.zone())`.
  3. Updated `web/src/pages/Classes.jsx` to group week slots using `slot.startsAtLocal ? slot.startsAtLocal.split(',')[0] : ...` so day headings always match the centre's formatted time regardless of client browser timezone.
- **How I know it is right**:
  Automated test `t1_1_morningClassGroupedUnderCentreTimezoneDay` in `FrontDeskIssuesTest.java` verifies that:
  - Querying `GET /api/slots?date=<Monday>` includes the 5:00 AM IST class.
  - Querying `GET /api/slots?date=<Sunday>` does not include it.
  - `GET /api/bookings/mine` returns the booking with `date` matching Monday.

## T1.2
- **What I found**:
  In `web/src/pages/Notifications.jsx` line 23, notification timestamps were formatted using `new Date(n.createdAt).toLocaleString('en-IN')`, which displays seconds (e.g. `20/11/2026, 1:40:17 pm`), creating cluttered time displays.
- **What I did**:
  Updated `web/src/pages/Notifications.jsx` to format timestamps using `new Date(n.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })`, rendering clean timestamps without seconds (e.g. `20 Nov 2026, 1:40 pm`).
- **How I know it is right**:
  Verified formatting output with Indian locale; verified frontend production build succeeds with `npm run build`.

## T1.3
- **What I found**:
  1. `BookingService.book()` lacked concurrency control and was not `@Transactional`. Capacity and existing booking checks were performed without row locking, allowing concurrent booking requests and double-clicks during payment gateway delays (~120ms) to slip past checks and create overbookings and duplicate bookings.
  2. The schema lacked a unique constraint preventing a member from holding multiple confirmed bookings for a slot. Seed data (`V2__seed.sql`) contained duplicate confirmed bookings for chess club (members 5 and 9) and spoken English class.
  3. `AdminController.attendance()` directly queried `bookings` for `status = 'CONFIRMED'`, which returned duplicate rows for the same member.
- **What I did**:
  1. Added Flyway migration `V4__fix_front_desk_issues.sql` to cancel duplicate confirmed bookings (keeping the earliest one) and created a unique partial index:
     `CREATE UNIQUE INDEX unique_confirmed_booking ON bookings (slot_id, member_id) WHERE status = 'CONFIRMED';`
  2. Added `findForUpdate(long id)` in `SlotRepository.java` to lock the slot row (`FOR UPDATE`) during booking.
  3. Added `@Transactional` to `BookingService.book()`, locking the slot with `findForUpdate` before checking capacity, serializing bookings per slot. Added idempotency keys (`booking-{slotId}-{memberId}`) to `gateway.charge()` and handled `DuplicateKeyException` gracefully.
  4. Updated `Classes.jsx` to disable the "Book" button and show a "Booking..." state while an API request is in progress to prevent double-clicks.
- **How I know it is right**:
  Automated test `t1_3_duplicateBookingPreventedAndAttendanceDeduplicated` verifies that duplicate bookings are rejected with `ALREADY_BOOKED` and that the admin attendance list shows each member only once. Flyway migration V4 applied successfully.

## T1.4
- **What I found**:
  The cancellation policy defined in `RefundCalculator.java` and `BookingService.java` specifies:
  "Cancellation policy: 90% back if the booking is cancelled at least 24 hours before the class starts, otherwise 50%."
  In `BookingService.java` line 131: `Duration.between(now, slot.startsAt()).toHours() >= 24;`.
  Because the user cancelled 20 hours before class start (< 24 hours), the cancellation fell under late cancellation and was correctly refunded at 50%.
- **What I did (or why I did nothing)**:
  Did nothing (no code change to the 24-hour threshold). The system functioned exactly as defined by the cancellation policy. Modifying the threshold would contradict the centre's published cancellation rules.
- **How I know it is right**:
  Confirmed policy statements across `RefundCalculator.java`, `RefundCalculatorTest.java`, and UI cancellation confirmations in `MyBookings.jsx`.

## T1.5
- **What I found**:
  1. `V1__schema.sql` defined default plans (`Starter` is marked `is_default = true`), but in table `members`, `plan_id` defaulted to `NULL`.
  2. `AuthController.signup()` called `MemberRepository.insert()` without specifying `plan_id`, leaving it `null`. Recent signups in `V2__seed.sql` (IDs 33, 34, 35) similarly had `plan_id = null`.
  3. When members with `plan_id = null` accessed `/api/me/plan`, `MeController.java` called `members.findPlan(member.planId()).orElseThrow()`, which threw `NoSuchElementException` (HTTP 500). `MyPlan.jsx` caught this error and rendered the error page.
- **What I did**:
  1. Updated `MemberRepository.java`:
     - Added `findDefaultPlan()` (`select * from plans where is_default = true limit 1`).
     - In `insert()`, set `plan_id = (select id from plans where is_default = true limit 1)`.
     - In `findPlan(Long id)`, returned `Optional.empty()` safely when `id == null`.
  2. In `MeController.java`, added fallback to `members.findDefaultPlan()` if `member.planId()` is null or not found.
  3. In Flyway migration `V4__fix_front_desk_issues.sql`, backfilled all existing members with `plan_id IS NULL AND role = 'MEMBER'` to the default Starter plan.
- **How I know it is right**:
  Automated test `t1_5_newMembersAndMembersWithNullPlanGetDefaultPlan` in `FrontDeskIssuesTest.java` signs up a new member and calls `/api/me/plan` (expecting HTTP 200 and Starter plan details), and verifies seeded member Rohan Gupta (id 33) gets the Starter plan without error.

## T1.6
- **What I found**:
  SlotBook models members individually (1 account = 1 member with unique email, at most 1 seat per slot via `hasConfirmed(memberId, slotId)`). There is no entity or logic for child accounts, dependent profiles, or multi-seat reservations under a single login.
- **What I did (or why I did nothing)**:
  Did nothing (no code change). This is an enhancement / product feature request requiring schema changes, business policy decisions, and privacy considerations (CONTRACT.md Rule 4), rather than a bug in existing functionality. Currently, parents must create a separate member account for each child.
- **How I know it is right**:
  Verified `V1__schema.sql`, `BookingService.java`, and `CONTRACT.md` Rule 4 ("Members' details are private").

## T1.7
- **What I found**:
  1. In `RefundCalculator.java`, `refundFor` computed `(long) (refund * 100) / 100.0`, truncating cents instead of rounding to the nearest paisa. For example, `135.45 * 0.9 = 121.905` became `121.90` instead of `121.91`, and `333.33 * 0.9 = 299.997` became `299.99` instead of `300.00`.
  2. In `V3__october_history.sql`, the 30 October refunds in `payments` were seeded with truncated values; 20 rows were each 1 paisa lower than the gateway (totaling ₹6853.30 instead of ₹6853.50).
  3. In `AdminController.refunds()`, date boundaries were computed using `ZoneOffset.UTC` instead of `centreClock.zone()` (Asia/Kolkata), and summing double values followed by `Math.floor(total * 100) / 100` introduced floating point truncation errors. It also lacked `requireAdmin()`.
  4. In `RefundCalculatorTest.java`, the test asserted the buggy truncated value `299.99` instead of `300.00`.
- **What I did**:
  1. Updated `RefundCalculator.java` to use `BigDecimal` with `RoundingMode.HALF_UP` to round to the nearest paisa.
  2. Updated `RefundCalculatorTest.java` to assert `300.00` and added assertions for other price points.
  3. In Flyway migration `V4__fix_front_desk_issues.sql`, updated the 20 truncated October refund payment records in table `payments` to match `docs/gateway-refunds-2026-10.csv`.
  4. Updated `AdminController.refunds()` to inject `CentreClock`, compute monthly boundaries in `centreClock.zone()`, enforce `requireAdmin()`, and compute totals using `BigDecimal` rounded to 2 decimal places.
- **How I know it is right**:
  Automated tests `t1_7_octoberRefundReportMatchesGatewayTotal` and `RefundCalculatorTest` passed. Calling `/api/admin/reports/refunds?month=2026-10` returns exactly 30 refunds and total `6853.50`, perfectly matching `docs/gateway-refunds-2026-10.csv`.
