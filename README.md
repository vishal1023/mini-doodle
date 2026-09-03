# Mini Doodle — Meeting Scheduling Platform

A backend coding challenge: a meeting scheduling platform ("mini Doodle") built with Spring
Boot and Java. Users manage time slots on a personal calendar, convert slots into meetings
with participants, and query an aggregated free/busy view for a time frame.

## Prerequisites

- Docker and Docker Compose (this is the only requirement to run the app end-to-end).
- Java 21 and the bundled Gradle wrapper (`./gradlew`) — only needed if you want to run the app
  from an IDE instead of fully dockerized, or run the test suite.
- `curl` and `jq` — only needed for the API walkthrough / seed script below.

## Running locally

```
docker compose up --build
```

This boots Postgres and the application together. Postgres binds to host port `5432` by
default (override with `DB_HOST_PORT` if that's already taken on your machine); the app is on
`8080`. Flyway applies all migrations automatically on startup.

```
curl http://localhost:8080/actuator/health
```

To run the app locally against a dockerized Postgres only (e.g. from an IDE):

```
docker compose up -d postgres
./gradlew bootRun
```

`application.yml`'s defaults (`localhost:5432`, `doodle`/`doodle`/`doodle`) match the compose
file's Postgres exactly, so no extra configuration is needed.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/users` | Create a user |
| GET | `/api/v1/users/{userId}` | Get a user |
| POST | `/api/v1/users/{userId}/slots` | Create a time slot (start time + duration) |
| GET | `/api/v1/users/{userId}/slots` | List a user's slots, paginated, optional time-range/status filter |
| PATCH | `/api/v1/users/{userId}/slots/{slotId}` | Modify a slot (only while `FREE`) |
| DELETE | `/api/v1/users/{userId}/slots/{slotId}` | Delete a slot (only while `FREE`) |
| POST | `/api/v1/users/{userId}/slots/{slotId}/meetings` | Book a slot into a meeting |
| GET | `/api/v1/meetings/{meetingId}` | Get a meeting |
| PATCH | `/api/v1/meetings/{meetingId}` | Modify a meeting's title/description/participants (only while `SCHEDULED`) |
| DELETE | `/api/v1/meetings/{meetingId}` | Cancel a meeting (soft-delete, reverts the slot to `FREE`) |
| GET | `/api/v1/users/{userId}/meetings` | List a user's meetings, filterable by `role` (`ORGANIZER`/`PARTICIPANT`/`ANY`) |
| GET | `/api/v1/users/{userId}/availability` | Aggregated free/busy view for a time window |

Interactive docs: `http://localhost:8080/swagger-ui.html`. Raw contract:
[`api-definition.yml`](api-definition.yml).

### Consuming the API — a full walkthrough

```bash
BASE=http://localhost:8080/api/v1

# Create two users
ORGANIZER=$(curl -s -X POST $BASE/users -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com"}' | jq -r .id)
PARTICIPANT=$(curl -s -X POST $BASE/users -H "Content-Type: application/json" \
  -d '{"name":"Charles Babbage","email":"charles@example.com"}' | jq -r .id)

# Ada creates a slot: 2026-09-01 09:00, 60 minutes
SLOT=$(curl -s -X POST $BASE/users/$ORGANIZER/slots -H "Content-Type: application/json" \
  -d '{"startTime":"2026-09-01T09:00:00Z","durationMinutes":60}' | jq -r .id)

# Ada books that slot into a meeting with Charles
MEETING=$(curl -s -X POST $BASE/users/$ORGANIZER/slots/$SLOT/meetings \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Design sync\",\"participantUserIds\":[\"$PARTICIPANT\"]}" | jq -r .id)

# Query Ada's aggregated availability for the day - shows one BUSY interval
curl -s "$BASE/users/$ORGANIZER/availability?from=2026-09-01T00:00:00Z&to=2026-09-02T00:00:00Z"

# Charles lists meetings he's a participant in
curl -s "$BASE/users/$PARTICIPANT/meetings?role=PARTICIPANT"

# Ada cancels the meeting - the slot reverts to FREE, and can be rebooked
curl -s -X DELETE $BASE/meetings/$MEETING
curl -s "$BASE/users/$ORGANIZER/availability?from=2026-09-01T00:00:00Z&to=2026-09-02T00:00:00Z"
```

A ready-to-run version of a similar flow — 4 users, several slots, two bookings, one
cancellation — is available as a script:

```
./scripts/seed-demo-data.sh
```

## Testing

```
./gradlew test
```

Each integration test class spins up its own Testcontainers Postgres. Running many
Testcontainers-backed classes in the same JVM back-to-back can hit host resource limits on a
constrained machine (observed on a Docker Desktop VM capped at ~4GB RAM); if a full-suite run
shows a `Connection refused` failure, re-run the affected class in isolation — the tests
themselves are deterministic, this is a local resource ceiling, not flakiness in the code.

## Observability

- `GET /actuator/health`
- `GET /actuator/prometheus` — Micrometer metrics in Prometheus exposition format, including
  per-endpoint HTTP timing (`http_server_requests_seconds`), JVM, and HikariCP connection pool
  metrics, all tagged with `application=doodle-scheduler`. No Prometheus/Grafana containers are
  included in `docker-compose.yml` — the scrape endpoint is exposed for an external Prometheus
  instance to consume.
