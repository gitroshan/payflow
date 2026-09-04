CREATE TABLE payments (
    id                   VARCHAR(36)  PRIMARY KEY,
    idempotency_key      VARCHAR(80)  NOT NULL,
    merchant_id          VARCHAR(64)  NOT NULL,
    amount               NUMERIC(19,2) NOT NULL,
    currency             VARCHAR(3)   NOT NULL,
    captured_amount      NUMERIC(19,2) NOT NULL DEFAULT 0,
    refunded_amount      NUMERIC(19,2) NOT NULL DEFAULT 0,
    status               VARCHAR(24)  NOT NULL,
    gateway_reference    VARCHAR(80),
    failure_reason       VARCHAR(255),
    payment_method_token VARCHAR(64),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_payments_idempotency ON payments (idempotency_key);

CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    event_id       VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(36) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    topic          VARCHAR(64) NOT NULL,
    payload        TEXT        NOT NULL,
    published      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX ix_outbox_unpublished ON outbox_events (published, created_at);
