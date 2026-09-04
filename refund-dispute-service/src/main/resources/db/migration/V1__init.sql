CREATE TABLE payment_snapshots (
    payment_id        VARCHAR(36) PRIMARY KEY,
    merchant_id       VARCHAR(64) NOT NULL,
    captured_amount   NUMERIC(19,2) NOT NULL,
    refunded_amount   NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency          VARCHAR(3)  NOT NULL,
    gateway_reference VARCHAR(80) NOT NULL
);

CREATE TABLE refunds (
    id              VARCHAR(36) PRIMARY KEY,
    payment_id      VARCHAR(36) NOT NULL,
    merchant_id     VARCHAR(64) NOT NULL,
    amount          NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    status          VARCHAR(16) NOT NULL,
    reason          VARCHAR(80),
    idempotency_key VARCHAR(80),
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX ux_refunds_idempotency ON refunds (idempotency_key);
CREATE INDEX ix_refunds_payment ON refunds (payment_id);

CREATE TABLE disputes (
    id          VARCHAR(36) PRIMARY KEY,
    payment_id  VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    amount      NUMERIC(19,2) NOT NULL,
    currency    VARCHAR(3)  NOT NULL,
    reason_code VARCHAR(32) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    opened_at   TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);
CREATE INDEX ix_disputes_payment ON disputes (payment_id);

CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type   VARCHAR(64) NOT NULL,
    topic        VARCHAR(64) NOT NULL,
    payload      TEXT        NOT NULL,
    published    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX ix_outbox_unpublished ON outbox_events (published, created_at);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
