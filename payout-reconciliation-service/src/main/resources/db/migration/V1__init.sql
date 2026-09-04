CREATE TABLE payment_snapshots (
    payment_id        VARCHAR(36) PRIMARY KEY,
    merchant_id       VARCHAR(64) NOT NULL,
    amount            NUMERIC(19,2) NOT NULL,
    currency          VARCHAR(3)  NOT NULL,
    gateway_reference VARCHAR(80) NOT NULL
);

CREATE TABLE payouts (
    id             VARCHAR(36) PRIMARY KEY,
    merchant_id    VARCHAR(64) NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    status         VARCHAR(16) NOT NULL,
    bank_reference VARCHAR(64),
    created_at     TIMESTAMPTZ NOT NULL,
    paid_at        TIMESTAMPTZ
);
CREATE INDEX ix_payouts_merchant ON payouts (merchant_id);

CREATE TABLE reconciliation_records (
    id              VARCHAR(36) PRIMARY KEY,
    batch_id        VARCHAR(40) NOT NULL,
    payment_id      VARCHAR(36),
    internal_amount NUMERIC(19,2),
    provider_amount NUMERIC(19,2),
    currency        VARCHAR(3),
    status          VARCHAR(24) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_recon_batch ON reconciliation_records (batch_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

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
