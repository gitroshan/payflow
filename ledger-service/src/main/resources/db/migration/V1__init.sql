CREATE TABLE accounts (
    id       VARCHAR(96) PRIMARY KEY,
    type     VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3)  NOT NULL,
    balance  NUMERIC(21,2) NOT NULL DEFAULT 0,
    version  BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_accounts_owner ON accounts (owner_id);

CREATE TABLE journal_entries (
    id             VARCHAR(36) PRIMARY KEY,
    reference_type VARCHAR(32) NOT NULL,
    reference_id   VARCHAR(40) NOT NULL,
    description    VARCHAR(160) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_journal_reference ON journal_entries (reference_id);

CREATE TABLE postings (
    id               BIGSERIAL PRIMARY KEY,
    journal_entry_id VARCHAR(36) NOT NULL REFERENCES journal_entries (id),
    account_id       VARCHAR(96) NOT NULL,
    direction        VARCHAR(6)  NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3)  NOT NULL
);
CREATE INDEX ix_postings_account ON postings (account_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
