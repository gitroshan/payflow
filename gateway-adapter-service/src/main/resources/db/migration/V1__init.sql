CREATE TABLE gateway_transactions (
    reference      VARCHAR(40) PRIMARY KEY,
    payment_id     VARCHAR(36) NOT NULL,
    operation      VARCHAR(16) NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    approved       BOOLEAN     NOT NULL,
    decline_reason VARCHAR(128),
    created_at     TIMESTAMPTZ NOT NULL
);
