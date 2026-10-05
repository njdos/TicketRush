-- V1__create_payment_tables.sql

CREATE TABLE payments
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    reservation_id UUID        NOT NULL UNIQUE,
    user_id        UUID        NOT NULL,
    amount         NUMERIC(10, 2),
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE outbox_events
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_pay_outbox_pending ON outbox_events (status, created_at);