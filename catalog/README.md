# TicketRush — Catalog Service

The **Catalog Service** is a core microservice responsible for managing the directory of events (concerts, festivals, shows) within the **TicketRush** ticket reservation system.

## 🚀 Tech Stack

* **Java 17**
* **Spring Boot 3.4.0** (Spring Data JPA, Spring Web, Spring Validation)
* **PostgreSQL 16** (Primary Database)
* **Maven** (Build Tool)
* **MapStruct & Lombok** (Object mapping and boilerplate reduction)
* **Testcontainers** (Integration testing using real Docker containers)

---

## 🛠️ API Endpoints

All endpoints wrap their payload inside a standardized `ApiResponse<T>` structure to maintain a unified contract across the frontend and other microservices.

| Method | Endpoint | Description | Parameters / Headers |
| :--- | :--- | :--- | :--- |
| **POST** | `/events` | Create a new event | Header: `X-User-Id` (UUID of the Organizer) |
| **GET** | `/events` | Retrieve a paged list of events | Query: `page` (default 0), `size` (default 20) |
| **GET** | `/events/{id}` | Fetch a specific event by ID | Path: `id` (UUID of the event) |
| **PUT** | `/events/{id}` | Update an existing event's details | Path: `id` (UUID of the event) + JSON Body |

---

## 🏗️ Layered Architecture

The project adheres to a clean, multi-layered architectural approach:
1. **Controllers (`web layer`)**: Expose REST endpoints, handle incoming query mapping, trigger payload validation (`@Valid`), and assemble the `ApiResponse`.
2. **Services (`business logic layer`)**: Orchestrate business logic, interact with data mapping utilities, and manage transaction scopes (`@Transactional`).
3. **Repositories (`data access layer`)**: Abstract SQL mechanics using Spring Data JPA interfaces.
4. **Mappers & DTOs**: Maintain a strict separation between persistent database entities (`Event`) and structural data transfers (`EventRequestDto`, `EventResponseDto`).

---

## 🧪 Testing Strategy

The service implements a two-tiered testing strategy to ensure rapid feedback cycles and flawless environment compatibility:

### 1. Isolated Unit Tests (`CatalogControllerTest`)
* **Framework:** Pure `Mockito` extension bypassing heavy Spring application context initializing overhead.
* **Scope:** Verifies URL routing, controller validation constraints, pagination parameter binding, and target JSON structure evaluations (`jsonPath`).
* **Performance:** Executes in milliseconds.

### 2. Live Database Integration Tests (`EventServiceImplIntegrationTest`)
* **Framework:** **Testcontainers** initializes an ephemeral, isolated Docker container running `postgres:16-alpine` on-demand during the build execution.
* **Connectivity:** Utilizes Spring Boot 3.4's modern `@ServiceConnection` annotation to bind the dynamic container instance to the datasource configuration automatically.
* **Scope:** Validates physical database schema auto-generation (`ddl-auto: update`), structural transaction constraints, entity mappings, repository logic, and custom error boundaries (`EventNotFoundException`).

---

## 🏃 Getting Started

### Prerequisites
* **Docker Desktop** installed and running (mandatory for integration test suites).
* **JDK 17** & **Maven** configured locally.

### Executing Tests
To execute all unit and integration test components (including setting up the Postgres container cluster on-the-fly), run:
```bash
mvn clean test
```

### Packaging the Application
To build and package the production-ready executable JAR artifact:
```bash
mvn clean package
```
