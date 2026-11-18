-- Module: sync-server (admin database). Every till has a till number (1, 2, 3...) next to its code (T01).
ALTER TABLE terminal ADD COLUMN till_no int;
UPDATE terminal t SET till_no = x.n
FROM (SELECT id, row_number() OVER (ORDER BY created_at, id) AS n FROM terminal) x WHERE x.id = t.id;
ALTER TABLE terminal ALTER COLUMN till_no SET NOT NULL;
ALTER TABLE terminal ADD CONSTRAINT terminal_till_no_key UNIQUE (till_no);
ALTER TABLE terminal ADD CONSTRAINT terminal_till_no_check CHECK (till_no BETWEEN 1 AND 999);
