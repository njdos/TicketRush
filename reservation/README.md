# 🎟️ TicketRush — Reservation Service

The **Reservation Service** is the high-concurrency, mission-critical core of the **TicketRush** ecosystem. It is architected to handle massive spikes in traffic (Flash Sales) by leveraging memory-backed locks before persisting records, preventing double-booking and protecting relational database health.

## 🚀 Tech Stack

* **Java 17**
* **Spring Boot 3.4.0** (Spring Data JPA, Spring Web, Spring Validation, Spring Kafka)
* **PostgreSQL 16** (Primary Database)
* **Redis 7** (Distributed Locks, Rate Limiting, and Highload Seat Pre-caching)
* **Apache Kafka** (Event Broker for Async Pipeline and SAGA Orchestration)
* **Maven** (Build Tool)
* **Testcontainers** (Integration testing with live Docker containers)

---

## 🛠️ API Endpoints

All endpoints wrap their payload inside a standardized `ApiResponse<T>` structure to maintain a unified contract across the frontend and other microservices.

| Method | Endpoint | Description | Parameters / Headers / Body |
| :--- | :--- | :--- | :--- |
| **POST** | `/reservations` | Initiate a ticket reservation | Header: `X-User-Id` (UUID) <br> Body: `ReservationRequestDto` |
| **GET** | `/reservations/{id}` | Check the status of a reservation | Path: `id` (UUID of the reservation) |

---

## 🏗️ Core Highload & Reliability Patterns

To survive a "Black Friday" event ticket sales rush, the service implements advanced distributed system patterns:

1. **Redis Pipelining (Batch Loading)**
    * When a new event is created, this service consumes `SeatsGeneratedEvent` from Kafka. Instead of executing thousands of single network requests, it wraps commands into a single binary TCP pipeline stream. It initializes 50,000+ seats in **~20ms**, avoiding consumer rebalance timeouts.
2. **Redis Distributed Locks & Rate Limiting**
    * Before touching PostgreSQL, the reservation route checks seat availability and acquires atomic distributed locks (via Redisson). If two threads try to snatch the same seat at the same microsecond, only one wins; the other fails instantly with a low-overhead response.
3. **Transactional Outbox Pattern**
    * To solve the *Dual Write Problem*, the creation of a reservation (`PENDING` state) and the publishing of the `PaymentRequestedEvent` to Kafka are kept strictly inside a single database atomic transaction. A background `OutboxProcessor` scheduler polls the outbox log table and pushes billing requests to Kafka safely.
4. **SAGA Orchestration Consumer & Compensation Loop**
    * The service listens to payment outcomes. If a `PaymentCompleted` signal arrives, the row transitions to `CONFIRMED`. If a `PaymentFailed` blip returns, it updates the state to `REJECTED` and triggers an active **SAGA Compensation**: it clears the lock key in Redis, returning the seat back to `AVAILABLE` for other users.
5. **Idempotency Guard (Inbox Pattern)**
    * Every incoming Kafka event is evaluated against a unique event ID lock key map inside Redis to guarantee that duplicate network messages never re-initialize seats or corrupt transaction states.

---

## 🧪 Testing Strategy

The service implements a two-tiered testing strategy to ensure rapid feedback cycles and flawless environment compatibility:

### 1. Isolated Unit Tests (`CatalogEventListenerTest`, `ReservationControllerTest`)
* **Framework:** Pure `Mockito` extension bypassing heavy Spring application context initializing overhead.
* **Scope:** Verifies URL routing, controller validation constraints, serialization settings, and MockMvc state responses.

### 2. Full Distributed Integration Tests (`ReservationServiceImplIntegrationTest`)
* **Framework:** **Testcontainers** initializes real, ephemeral Docker containers running `postgres:16-alpine`, `redis:7-alpine`, and `confluentinc/cp-kafka` simultaneously on-demand during the build.
* **Connectivity:** Utilizes Spring Boot 3.4's modern `@ServiceConnection` annotation to bind dynamic containers to the datasource, cache, and message broker configurations automatically.
* **Scope:** Validates the entire async flow: API call ➔ Redis Lock evaluation ➔ PostgreSQL Transaction ➔ Outbox row processing ➔ Kafka Event Delivery ➔ SAGA state transition.

---

## 🏃 Getting Started

### Prerequisites
* **Docker Desktop** installed and running (mandatory for integration test suites running Postgres, Redis, and Kafka in containers).
* **JDK 17** & **Maven** configured locally.

### Executing Tests
To execute all unit and integration test components (including setting up the distributed container cluster on-the-fly), run:
```bash
mvn clean test
```

### Packaging the Application
To build and package the production-ready executable JAR artifact:
```bash
mvn clean package
```
