-- Module: admin-app / sync-server (admin database only).

CREATE TABLE admin_user (
    id             uuid PRIMARY KEY,
    username       text NOT NULL UNIQUE,
    display_name   text NOT NULL,
    password_hash  text NOT NULL,
    active         boolean NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now()
);

-- Releases: Publish turns all pending changes into release N.
CREATE TABLE config_release (
    release_no    serial PRIMARY KEY,
    published_at  timestamptz NOT NULL DEFAULT now(),
    published_by  uuid REFERENCES admin_user(id),
    note          text,
    change_count  int NOT NULL DEFAULT 0
);

-- Change feed: one row per master-data edit (full record as JSON, contract v1).
CREATE TABLE config_change (
    seq         bigserial PRIMARY KEY,
    release_no  int REFERENCES config_release(release_no),     -- NULL = pending (not visible to tills)
    entity      text NOT NULL CHECK (entity IN ('TAX_RATE','CATEGORY','PRODUCT')),
    entity_id   uuid NOT NULL,
    payload     jsonb NOT NULL,
    created_by  uuid REFERENCES admin_user(id),
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_config_change_pending ON config_change(seq) WHERE release_no IS NULL;

-- Tills allowed to pull the feed (token stored as SHA-256).
CREATE TABLE terminal (
    id            uuid PRIMARY KEY,
    name          text NOT NULL,
    token_sha256  text NOT NULL UNIQUE,
    active        boolean NOT NULL DEFAULT true,
    created_at    timestamptz NOT NULL DEFAULT now(),
    last_seen_at  timestamptz,
    last_seq      bigint
);
