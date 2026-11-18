-- Module: sync-client (POS only). Cursor into the admin change feed.
CREATE TABLE sync_state (
    id            text PRIMARY KEY,               -- 'admin'
    last_seq      bigint NOT NULL DEFAULT 0,
    last_release  int    NOT NULL DEFAULT 0,
    last_sync_at  timestamptz,
    last_error    text,
    last_error_at timestamptz
);
INSERT INTO sync_state (id) VALUES ('admin');
