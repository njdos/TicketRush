# TicketRush

Event-driven microservices platform for flash-sale ticket booking, built to handle the classic hard problem: **thousands of users competing for the same seats at the same moment, with zero oversell.**

> Personal project to practice production-style backend patterns: Transactional Outbox, idempotent consumers, distributed locking, Saga-style compensation, resilience, observability. Polyglot: Java and Kotlin.

---

## The problem

1000 seats, 20 000 users hitting "buy" at the same second. The system must:

1. Never sell one seat twice (no oversell).
2. Hold a seat for 10 minutes, then release it automatically.
3. Roll back the hold if the payment fails (compensation).
4. Notify the user even if the notification service was down for a while.
5. Survive the peak and show *how* it behaves under load.

---

## Architecture

**Two kinds of communication, never mixed:**

- **Synchronous (REST)**: only client → service. The user waits for the answer right now.
- **Asynchronous (Kafka)**: all service ↔ service communication. Services never call each other over HTTP, so one service being down does not take the others with it.

**Database per service.** Each service owns its data. Reservation keeps its own copy of seats (populated from `SeatsGenerated`) instead of querying Catalog.

---

## Services

| Service | Stack | Responsibility |
|---|---|---|
| **catalog-service** | Java 17, Spring Boot 4 | Events and seat definitions. Source of truth for what exists to sell. |
| **reservation-service** | Java 17, Spring Boot 4, Redis | Core of the system: seat holds, Redis locks, reservation lifecycle, expiry. |
| **payment-service** | Kotlin, Spring Boot 4 | Mock payment processor with configurable failure rate. Triggered by Kafka events. |
| **notification-service** | Kotlin, Spring Boot 4 | Consumes events, delivers notifications, retries with DLQ fallback. |
| **gateway** | Spring Cloud Gateway | Single entry point, JWT check, rate limiting. *(planned)* |

---

## Happy path

1. Organizer: `POST /events` → Catalog saves the event and publishes `SeatsGenerated` through the outbox.
2. Reservation consumes it and creates its own seats (`FREE`).
3. User: `POST /reservations` → Redis lock `SET NX EX 600` on the seat → reservation `HELD` + outbox row, in one DB transaction. `201`.
4. User: `POST /reservations/{id}/pay` → `PaymentRequested` goes to the outbox. `202 Accepted` (async).
5. Payment consumes it, processes, publishes the result.
6. Reservation consumes the result → `CONFIRMED`, seat `SOLD`.
7. Notification consumes the final event and notifies the user.
8. The client polls `GET /reservations/{id}` until the status changes.

## Failure scenarios

| What breaks | What happens |
|---|---|
| Payment fails | `PaymentFailed` → reservation `RELEASED`, seat `FREE` (compensation) |
| User never pays | Redis key expires by TTL; a scheduler marks the reservation `EXPIRED` and frees the seat |
| Redis is down | Fallback to Postgres: optimistic locking (`@Version`) on the seat prevents oversell, just slower |
| Duplicate Kafka message | Idempotent consumers ignore it (deduplication keys + DB unique constraint) |
| Service crashes between DB write and Kafka publish | Transactional Outbox: the event is in the same transaction and is published after recovery |
| Notification service down for an hour | Messages wait in Kafka; failed deliveries go through retries and a dead-letter topic |

---

## Patterns used

- **Transactional Outbox**: reliable event publishing without distributed transactions
- **Idempotent consumer**: safe under at-least-once delivery
- **Distributed lock (Redis)**: atomic `SET NX EX` to acquire, Lua compare-and-delete to release
- **Saga (choreography)**: compensation on payment failure, no central orchestrator
- **Optimistic locking**: second line of defense behind the Redis lock
- **Cache-aside** for event reads
- **Database per service**, schema versioned with **Flyway**

> Redis is the fast filter, Postgres is the source of truth.

---

## API

### Catalog
| Method | Path | Description |
|---|---|---|
| POST | `/events` | Create an event (seats are generated automatically) |
| GET | `/events` | List events (paginated) |
| GET | `/events/{id}` | Get one event |
| PUT | `/events/{id}` | Update an event |

### Reservation
| Method | Path | Description |
|---|---|---|
| GET | `/availability/{eventId}` | Free / held / sold seats |
| POST | `/reservations` | Hold a seat for 10 minutes (`201`, or `409` if taken) |
| GET | `/reservations/{id}` | Reservation status |
| POST | `/reservations/{id}/pay` | Request payment (`202`) |
| DELETE | `/reservations/{id}` | Cancel and release the seat |

### Payment
| Method | Path | Description |
|---|---|---|
| GET | `/payments/{reservationId}` | Payment status |
| PUT | `/admin/payment-config` | Set mock failure rate (testing / demo) |

### Notification
| Method | Path | Description |
|---|---|---|
| GET | `/notifications` | Notification history for the current user |

Full OpenAPI specs: [`docs/openapi/`](docs/openapi/).

### Kafka topics

| Topic | Producer | Consumer |
|---|---|---|
| `seats-generated-topic` | Catalog | Reservation |
| `payment-requests-topic` | Reservation | Payment |
| `payment-results-topic` | Payment | Reservation |

---

## Tech stack

Java 17 · Kotlin · Spring Boot 4 · Spring Data JPA · PostgreSQL · Flyway · Apache Kafka · Redis · Docker · Docker Compose · Testcontainers · JUnit 5

Planned / in progress: Kubernetes · GitHub Actions · Resilience4j · Prometheus · Grafana · OpenTelemetry · k6

---

## Getting started

**Requirements:** Docker, Docker Compose, JDK 17 (for running tests locally).

```bash
git clone https://github.com/njdos/TicketRush.git
cd ticketrush
docker compose up --build
```

Quick check:

```bash
# 1. create an event (organizer)
curl -X POST localhost:8081/events \
  -H "Content-Type: application/json" -H "X-User-Id: <organizer-uuid>" \
  -d '{"name":"Rush Concert","venue":"Main Arena","startsAt":"2027-01-01T20:00:00","totalSeats":100,"price":50.00}'

# 2. reserve a seat
curl -X POST localhost:8082/reservations \
  -H "Content-Type: application/json" -H "X-User-Id: <user-uuid>" \
  -d '{"eventId":"<event-uuid>","seatId":"<seat-uuid>"}'

# 3. pay
curl -X POST localhost:8082/reservations/<reservation-uuid>/pay

# 4. check the result
curl localhost:8082/reservations/<reservation-uuid>
```

> Authentication is currently a stub (`X-User-Id` header). Gateway + JWT is on the roadmap.

### Run the tests

```bash
./mvnw test
```

Integration tests use **Testcontainers** with real PostgreSQL, Redis and Kafka instances (no H2, no mocks for infrastructure). Docker must be running.

---

## Testing

- **Concurrency test:** 50 threads attempt to reserve the same seat simultaneously, exactly one wins, 49 receive a conflict.
- **Integration tests** with Testcontainers (Postgres, Redis, Kafka).
- Planned: Redis-failure fallback test, outbox duplicate-publish test, DLQ test.


## Resilience

*Chaos experiments (kill Redis, kill Kafka, kill a pod under load) and their results: see [`docs/chaos.md`](docs/chaos.md).*

---

## Architecture decisions

Key decisions are documented as ADRs in [`docs/adr/`](docs/adr/):

- ADR-001: Redis lock instead of pessimistic DB lock for seat reservation
- ADR-002: Kafka instead of RabbitMQ for service communication
- ADR-003: Transactional Outbox instead of publishing directly from the service
- ADR-004: Database per service, no shared `common` library for event DTOs

---

## Project status

| Area                                        | Status |
|---------------------------------------------|---|
| Catalog Service                             | ✅ implemented |
| Reservation Service (locks, expiry, outbox) | ✅ implemented |
| Payment Service (mock, outbox, idempotency) | ✅ implemented |
| Notification Service (Kotlin)               | 🚧 in progress |
| Docker Compose environment                  | ✅ implemented  |
| CI (GitHub Actions, SonarQube)              | ✅ implemented  |
| Kubernetes manifests                        | ✅ implemented  |
| Migration (flyway)                          | ✅ implemented  |
| Resilience4j (circuit breaker, fallback)    | 🚧 in progress |
| Prometheus + Grafana                        | 🚧 in progress  |
| Distributed tracing (OpenTelemetry)         | 📋 planned |
| k6 load tests                               | 📋 planned |
| Gateway + JWT                               | 📋 planned |
| Elasticsearch event search                  | 📋 planned |

*(Update this table as things ship. Do not mark something done until it works.)*

---

## Known limitations

- Authentication is a stub header, not real auth.
- Payment is a mock with a configurable failure rate, no real provider.
- Kafka event envelope is simplified (no schema registry yet).
- Seat unlock on cancel relies on the Redis TTL rather than the stored lock token.

## Author

**Oleh Dolinin** · Java / Kotlin Fullstack Engineer · [LinkedIn](https://www.linkedin.com/in/oleh-dolinin) · [Telegram](https://t.me/oleh_dolinin)