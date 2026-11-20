# SlotBook

SlotBook is a booking app for a coaching centre. Members book seats in class slots, pay in rupees, and cancel for a refund. Admins see attendance and refund reports.

Java 21 + Spring Boot + Postgres on the backend, React (Vite) on the frontend.

**Read [TICKETS.md](TICKETS.md) for today's work and [CONTRACT.md](CONTRACT.md) before you write a module.**

## Run it

You need JDK 21 and Node 20 or later. No Docker: Postgres starts with the app.

Backend, from the repo root (first run downloads dependencies):

```
./mvnw -pl app -am spring-boot:run          # macOS / Linux
mvnw.cmd -pl app -am spring-boot:run        # Windows
```

The API is on http://localhost:8080. **The database starts empty on every run**: Flyway migrations create the schema and the demo data each time, so anything you change by hand is gone after a restart. Data fixes belong in a migration.

Frontend, in a second terminal:

```
cd web
npm install
npm run dev
```

Open http://localhost:5173. The dev server forwards `/api` to the backend.

## Demo logins

Every demo password is `demo`.

| Email | Who |
| --- | --- |
| `meera@slotbook.test` | A member with bookings |
| `rohan@slotbook.test` | A member who signed up last week |
| `admin@slotbook.test` | An admin |

Other seeded members follow the pattern `firstname.lastname@slotbook.test` (for example `diya.patel@slotbook.test`).

## Tests

```
./mvnw verify                                  # everything
./mvnw -pl core -am verify                     # core only
./mvnw -pl modules/waitlist -am verify         # the waitlist module's contract tests (fail until T2 is built)
```

The settlement chaos run (wave 2) takes about 5 minutes, so `./mvnw verify` skips it. Run it with `./chaos.sh` (Windows: `chaos.cmd`).

Tests start the whole app with a fresh embedded database. Shared helpers for tests are in `core/src/test/java/com/slotbook/core/testing/` (`@SlotBookTest`, `TestHost`).

## Looking at the database

On startup the backend logs a line like `Embedded Postgres is running: jdbc:postgresql://localhost:53412/postgres?user=postgres`. Connect any Postgres client to that URL (user `postgres`, no password). The port changes on every run.

Schema and demo data: `core/src/main/resources/db/migration/`.

## Layout

```
contract/            Java interfaces and events a module may use (the only SlotBook code a module compiles against)
core/                Spring Boot app: members, plans, batches, slots, bookings, simulated payments, admin reports
modules/reminders/   a working sample module
modules/waitlist/    an empty module, ready for T2
modules/settlement/  an empty module, for later
modules/inbox/       an empty module, for later
chaos/               the settlement chaos runner
app/                 the runnable app: core plus every module
web/                 React UI; module UI panels live in web/src/modules/
docs/                reference material
```

## API at a glance

All endpoints are under `/api`. Send `Authorization: Bearer <token>` after signing in.

| Method and path | What it does |
| --- | --- |
| `POST /api/auth/login` `{email, password}` | Sign in; returns `{token, member}` |
| `POST /api/auth/signup` `{name, email, password}` | Create an account; returns `{token, member}` |
| `GET /api/me` | The signed-in member |
| `GET /api/me/plan` | Their plan and how many classes they have used this month |
| `GET /api/slots?date=YYYY-MM-DD` | Classes on a day (or `?from=...&to=...`, both inclusive) |
| `GET /api/slots/{id}` | One class |
| `POST /api/bookings` `{slotId}` | Book and pay; `409` with an `outcome` when it can't |
| `GET /api/bookings/mine` | The member's bookings |
| `POST /api/bookings/{id}/cancel` | Cancel and refund; returns `{bookingId, refund}` |
| `GET /api/notifications` | The member's notifications |
| `GET /api/admin/slots/{id}/attendance` | Admin: who is booked into a class |
| `GET /api/admin/reports/refunds?month=YYYY-MM` | Admin: refunds paid in a month |
