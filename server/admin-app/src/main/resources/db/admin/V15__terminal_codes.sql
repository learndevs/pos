-- Module: sync-server (admin database). Independent tills: every terminal gets a short device code
-- (T01, T02, ...) used on receipts and in the archive, plus an optional location.
ALTER TABLE terminal ADD COLUMN code text;
ALTER TABLE terminal ADD COLUMN location text;
UPDATE terminal t SET code = 'T' || lpad(n::text, 2, '0')
FROM (SELECT id, row_number() OVER (ORDER BY created_at, id) AS n FROM terminal) x WHERE x.id = t.id;
ALTER TABLE terminal ALTER COLUMN code SET NOT NULL;
ALTER TABLE terminal ADD CONSTRAINT terminal_code_key UNIQUE (code);
ALTER TABLE terminal ADD CONSTRAINT terminal_code_check CHECK (code ~ '^[A-Z0-9]{1,6}$');

-- which till produced the transaction, as the till called itself at the time
ALTER TABLE transaction_archive ADD COLUMN device_code text;
UPDATE transaction_archive a SET device_code = t.code FROM terminal t WHERE t.id = a.terminal_id;
