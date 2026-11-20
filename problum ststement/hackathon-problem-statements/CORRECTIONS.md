# Corrections Log

## Refund calculation using double arithmetic with Math.round vs BigDecimal
What the AI did: Proposed using `Math.round(paid * (early ? EARLY_REFUND : LATE_REFUND) * 100) / 100.0` in `RefundCalculator.java:18`.
How I noticed: Ran a verification script with sample amounts (e.g. 135.45) in Node.js and Java. Due to IEEE 754 binary floating-point representation, `135.45 * 0.9` evaluates to `121.90499999999999`, which `Math.round` rounded down to `121.90` instead of `121.91`, failing to match the payment gateway report.
What I did: Implemented `BigDecimal.valueOf(paid).multiply(early ? EARLY_REFUND : LATE_REFUND).setScale(2, RoundingMode.HALF_UP).doubleValue()` in `RefundCalculator.java:18`, which accurately produces `121.91` and `67.73`.

## Missing BookingResult import in BookingController
What the AI did: In `BookingController.java:3`, replaced `Clock` with `CentreClock` in imports and constructor, but inadvertently omitted `import com.slotbook.contract.BookingResult;`.
How I noticed: Executed `.\mvnw.cmd -pl core -am test` and the Maven compiler failed with `cannot find symbol: class BookingResult` at `BookingController.java:[48,27]`.
What I did: Re-added `import com.slotbook.contract.BookingResult;` to `BookingController.java:3` and recompiled successfully.

## Verification of CentreClock timezone for slot listing and bookings
What the AI did: Claimed that Monday 5:00 AM yoga in Asia/Kolkata corresponds to Sunday 23:30 UTC, causing `BookingController` to report Sunday and `SlotController` to miss the 5:00 AM batch when queried with UTC date bounds.
How I noticed: Checked `application.yml` (`centre-zone: Asia/Kolkata`), `CONTRACT.md` Rule 7 ("Time means the centre's time"), and verified the UTC conversion of `2026-11-16 05:00:00+05:30` is `2026-11-15 23:30:00Z`.
What I did: Confirmed that injecting `CentreClock` and using `centreClock.zone()` in `BookingController.java:64`, `SlotController.java:45-46`, and `AdminController.java:54-55` fixed the queries, and verified with automated test `FrontDeskIssuesTest.java:35`.
