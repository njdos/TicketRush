# Observability

| What | Where |
|---|---|
| Metrics endpoint | `GET :8081/actuator/prometheus` |
| Prometheus | http://localhost:9090 (targets, alerts) |
| Grafana | http://localhost:3000, `admin/admin`, dashboard **Rush Ticket / Catalog Service** |

Run: `docker compose up -d --build` (from the project root).

## Logs
* Local: text, every line has `[traceId] [userId]`.
* `SPRING_PROFILES_ACTIVE=prod`: JSON (ECS), MDC fields are top-level keys.
* Every HTTP response carries `X-Request-Id`; the same value is `trace_id` in error bodies and `traceId` in logs.
  Send your own `X-Request-Id` (e.g. from the gateway) and it is reused.
* Background outbox batches get `traceId=outbox-xxxxxxxx` plus `outboxEventId` and `aggregateId`.

Find everything about one failed request: `docker compose logs catalog | grep <trace_id>`
(in prod JSON: filter `traceId == "<trace_id>"`).

## Custom metrics
| Metric | Meaning |
|---|---|
| `catalog_outbox_events{status}` | PENDING / FAILED outbox rows |
| `catalog_outbox_oldest_pending_age_seconds` | publishing lag |
| `catalog_outbox_publish_total{result}` | success / failure / circuit_open |

Resilience4j, HikariCP, JVM, Kafka producer and `http_server_requests` metrics come from Spring Boot automatically.

## Alerts (`prometheus/alerts.yml`)
Instance down, 5xx > 5%, p95 > 1s, circuit open, outbox lag > 60s, FAILED outbox events, Hikari > 90%, heap > 90%.
