CREATE TABLE seats (
    seat_id    UUID PRIMARY KEY,
    event_id   UUID NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'FREE',
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_seats_event_id ON seats(event_id);
CREATE INDEX idx_seats_event_status ON seats(event_id, status);

CREATE TABLE reservations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    seat_id     UUID NOT NULL REFERENCES seats(seat_id),
    event_id    UUID NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'HELD',
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_status_expires ON reservations(status, expires_at);