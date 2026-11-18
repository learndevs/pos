-- Module: sync-server (admin database). Zip packages replace releases; transaction archive from tills.

ALTER TABLE config_change DROP COLUMN release_no;
DROP TABLE config_release;
CREATE INDEX ix_config_change_entity_time ON config_change(entity, created_at);

CREATE TABLE config_package (
    package_no   serial PRIMARY KEY,
    created_at   timestamptz NOT NULL DEFAULT now(),
    created_by   uuid REFERENCES admin_user(id),
    note         text,
    scope        text NOT NULL CHECK (scope IN ('FULL','DATE_RANGE')),
    date_from    date,
    date_to      date,
    entities     text NOT NULL,               -- comma separated: TAX_RATE,CATEGORY,PRODUCT
    item_counts  jsonb NOT NULL,              -- {"PRODUCT": 3, ...}
    sha256       text NOT NULL,               -- of the whole zip
    size_bytes   int  NOT NULL,
    zip          bytea NOT NULL,
    CHECK (scope = 'FULL' OR (date_from IS NOT NULL AND date_to IS NOT NULL AND date_from <= date_to))
);

CREATE TABLE package_target (
    package_no   int  NOT NULL REFERENCES config_package(package_no),
    terminal_id  uuid NOT NULL REFERENCES terminal(id),
    status       text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DOWNLOADED','APPLIED','FAILED')),
    error        text,
    updated_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (package_no, terminal_id)
);
CREATE INDEX ix_package_target_terminal ON package_target(terminal_id, status);

CREATE TABLE transaction_archive (
    id             uuid PRIMARY KEY,           -- POS transaction id (idempotent uploads)
    terminal_id    uuid NOT NULL REFERENCES terminal(id),
    txn_no         bigint NOT NULL,
    receipt_no     text,
    status         text NOT NULL,              -- COMPLETED | CANCELLED (| REFUND later)
    business_date  date NOT NULL,
    total_minor    bigint NOT NULL,
    finished_at    timestamptz,
    received_at    timestamptz NOT NULL DEFAULT now(),
    payload        jsonb NOT NULL
);
CREATE INDEX ix_archive_date ON transaction_archive(business_date DESC, txn_no DESC);
CREATE INDEX ix_archive_terminal ON transaction_archive(terminal_id, business_date DESC);
