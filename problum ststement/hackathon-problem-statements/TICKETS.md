# Tickets

Three tickets, in no particular order. Choosing the order is part of the job. There is more here than anyone can finish.

Anything unclear in a requirement? Ask the client on the Q&A board.

## Your corrections log (counts across every ticket)

We are hiring people who understand what their AI tool did, and fix it when it is wrong. So keep `CORRECTIONS.md` at the repo root, and add to it as you go, not at the end.

Each time you find something your AI tool got wrong (a fix that only hides the symptom, a claim that was not true, a rule it ignored, a test it changed to make it pass), add an entry. Also add one when you checked something it did and it was right.

```
## <short title>
What the AI did: <the change or claim, with the commit or file:line>
How I noticed: <what you ran, read or asked>
What I did: <your fix and its commit, or how you confirmed it was right>
```

We check every entry against your transcripts and your commits. An entry we can find there counts for you; an entry we cannot find, or that did not happen the way it says, counts against you. A short, true log beats a long one. Asking your AI to write this file for you will not help: it does not know what you noticed.

## Handing in your AI sessions

Your AI tool's sessions are how we check your corrections log, so hand them all in. From the repo, run:

```
node export-transcripts.mjs
git push
```

It finds your sessions for this repo (OpenAI Codex, Google Antigravity, Gemini CLI, Claude Code), copies them into `transcripts/` and commits them. Run it as often as you like, and once more just before 5:00. Using another tool? Export its chats into `transcripts/` yourself.

## T1. From the front desk

Everything the front desk passed on this week, as it came in. Each describes what people saw or asked for, not what is wrong in the code.

1. "I booked Monday's 5:00 AM yoga, and My Bookings lists it under Sunday. And the Today tab never shows the 5:00 AM batch, but it does show tomorrow's."
2. "The times on the Notifications page look messy, with seconds and everything."
3. "Saturday's 10:00 AM chess club has 20 seats. 22 people turned up with confirmed bookings. The admin attendance list also shows some members twice for the same class."
4. "I cancelled my class 20 hours before it started and only got half my money back. Please fix this."
5. "New members get an error page when they open My Plan. Older members are fine."
6. "Parents keep asking whether they can book classes for their children from their own account."
7. "Refunds are sometimes off by a paisa, and finance says the monthly refund total never matches the payment gateway's report." October's gateway report is in `docs/gateway-refunds-2026-10.csv`.

Deal with as many as you can. For each one you deal with, say in `STATUS.md` what you found, what you did (or why you did nothing), and how you know it is right. Number entries as here: `T1.1` to `T1.7`.

## T2. Waitlist module

Build a waitlist in `modules/waitlist/`. The module is already wired into the build and the app; it is empty. Use only `contract/`, follow `modules/reminders/` for the shape, and follow `CONTRACT.md`.

- When a slot is full, a member can join its waitlist instead of booking.
- A member can see their own position and leave the waitlist.
- When a booked seat is cancelled, the first member on the waitlist is booked into it automatically and notified.
- The slot page shows the waitlist with members' names, so people know who is ahead of them.
- Admins can see and clear any slot's waitlist.

Endpoints:

| Method and path | Who | Response |
| --- | --- | --- |
| `POST /api/modules/waitlist/slots/{slotId}/join` | member | `200 {"position": 2}`; `409` if the slot has free seats or they cannot join |
| `DELETE /api/modules/waitlist/slots/{slotId}/me` | member | `204` |
| `GET /api/modules/waitlist/slots/{slotId}` | member | `200 {"size": 3, "myPosition": 2, "entries": [...]}`; `myPosition` is `null` when they are not on it |
| `GET /api/modules/waitlist/admin/slots/{slotId}` | admin | `200 {"size": 3, "entries": [...]}` |
| `DELETE /api/modules/waitlist/admin/slots/{slotId}` | admin | `204` |

The module must pass its contract tests: `./mvnw -pl modules/waitlist -am verify`. A UI panel under `web/src/modules/waitlist/` (placement `slot`) is welcome if you have time; the reminders module shows how.

## T3. Booking assistant

Add a chat assistant that a signed-in member can use instead of the booking screens: "book me into tomorrow's evening yoga", "what have I got this week?", "cancel my Friday class". Give it tools for listing classes, booking, cancelling and listing the member's bookings.

- Endpoint: `POST /api/assistant/chat` with `{"messages": [{"role": "user", "content": "..."}]}` (the whole conversation so far, oldest first). Responds `200 {"reply": "..."}`.
- Put the code in core, under `com.slotbook.core.assistant`, and add any dependencies to `core/pom.xml`.
- Use any LLM provider. The free tier of the Gemini API is enough: use the key you created for the setup check, with a free model such as Gemma 4 or Gemini 3.5 Flash Lite. Read the key from the `LLM_API_KEY` environment variable.
- It acts only for the member who is chatting, never for anyone else.
- When a request is unclear, it does not guess.
- Add `ASSISTANT.md` at the repo root: the provider and model, how to try it, and what you did to keep it from doing the wrong thing.
