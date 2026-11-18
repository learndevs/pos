-- Module: sync-client (POS only). Package-based config sync + transaction outbox for the admin archive.
ALTER TABLE sync_state DROP COLUMN last_release;
ALTER TABLE sync_state RENAME COLUMN last_seq TO last_package;
ALTER TABLE sync_state ADD COLUMN last_package_at timestamptz;

-- Finished transactions waiting to be uploaded to the admin archive (written in the same DB transaction).
CREATE TABLE txn_outbox (
    transaction_id  uuid PRIMARY KEY REFERENCES pos_transaction(id),
    payload         jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    attempts        int NOT NULL DEFAULT 0,
    last_error      text
);
CREATE INDEX ix_txn_outbox_pending ON txn_outbox(created_at) WHERE sent_at IS NULL;
