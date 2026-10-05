-- V1__create_payment_tables.sql

CREATE TABLE IF NOT EXISTS events (
                                      id UUID PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    total_seats INT NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    organizer_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                             );

CREATE INDEX IF NOT EXISTS idx_events_venue_starts ON events (venue, starts_at);
CREATE INDEX IF NOT EXISTS idx_events_name ON events (name);


CREATE TABLE IF NOT EXISTS outbox_events (
                                             id UUID PRIMARY KEY,
                                             aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
                               );

CREATE INDEX IF NOT EXISTS idx_outbox_pending_processing
    ON outbox_events (created_at)
    WHERE status = 'PENDING';
