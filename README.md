# Mini Doodle — Meeting Scheduling Platform

A backend coding challenge: a high-performance simulation of a meeting scheduling platform
(a "mini Doodle") built with Spring Boot and Java. Users manage time slots on a personal
calendar, convert slots into meetings with participants, and query aggregated free/busy
availability for a time frame.

> This README will grow as the implementation progresses. This first commit captures the
> problem statement and the planned approach; design decisions will be documented in detail
> as each part of the system is built.

## Problem statement

- Users define available time slots with configurable duration.
- A slot can be booked into a meeting with a title, description, and participants.
- Users can modify or delete existing slots, and mark them busy/free.
- The system supports querying free/busy slots with an aggregated view for a selected time
  frame.
- Each user has a personal calendar, but **"calendar" is a domain concept only** — it is not
  exposed as an API resource. The API surface is `users`, `slots`, and `meetings`.
- Target scale: hundreds of users, thousands of slots. The design should hold up at that
  scale without over-engineering for a scale that isn't asked for.
- All data is persisted. The solution must run locally via `docker-compose` with all
  dependencies included.

## Planned approach

- **Stack**: Java 21, Spring Boot 3.x, Gradle (Groovy DSL), PostgreSQL, Flyway for schema
  migrations.
- **Architecture**: package-by-feature (`user`, `slot`, `meeting`), layered
  controller → service → domain → repository. `Calendar` exists only as an internal domain
  aggregate, never as a controller, DTO, or route.
- **Concurrency correctness** (the core engineering problem at this scale):
  - Overlapping slots for the same user are prevented atomically at the database level via a
    PostgreSQL `EXCLUDE` constraint (`btree_gist`) on the user's time range — not an
    app-level check-then-insert, which would race under concurrent requests.
  - Booking a slot into a meeting is a single-row critical section protected by a pessimistic
    lock, so two concurrent booking requests for the same slot can never both succeed.
  - A DB-level partial unique index backs up the invariant that a slot has at most one active
    meeting at a time.
- **Testing**: TDD throughout (tests written before implementation). Unit tests for domain
  logic (state transitions, free/busy merge algorithm), integration tests against a real
  Postgres instance via Testcontainers, and a dedicated concurrency test that fires parallel
  booking requests at the same slot to prove the locking strategy actually holds under
  contention.
- **Docs & consumption**: the API is documented via OpenAPI/Swagger UI, and this README will
  include a full curl walkthrough once the endpoints exist. Metrics are exposed via Spring
  Boot Actuator + Micrometer.
- **Running locally**: `docker-compose up` will boot the full stack (application + Postgres)
  with no external dependencies required.

## Status

Project scaffolding not yet started. Follow the commit history for incremental progress.
