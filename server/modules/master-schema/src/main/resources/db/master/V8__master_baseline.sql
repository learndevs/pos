-- Module: master-schema (shared by POS and admin). Master data both databases must have.
-- On existing POS databases these tables already exist (from V1), so every statement is IF NOT EXISTS (no-op).
-- On the admin database this creates them. Keep definitions identical to the POS ones.

CREATE TABLE IF NOT EXISTS tax_rate (
    id         uuid PRIMARY KEY,
    name       text    NOT NULL,
    rate_bp    int     NOT NULL CHECK (rate_bp >= 0 AND rate_bp <= 10000),
    inclusive  boolean NOT NULL DEFAULT true,
    active     boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS category (
    id          uuid PRIMARY KEY,
    name        text    NOT NULL UNIQUE,
    sort_order  int     NOT NULL DEFAULT 0,
    active      boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS product (
    id             uuid PRIMARY KEY,
    category_id    uuid    NOT NULL REFERENCES category(id),
    name           text    NOT NULL,
    item_class     text    NOT NULL DEFAULT 'MERCHANDISE' CHECK (item_class IN ('MERCHANDISE','NON_MERCHANDISE')),
    non_merch_type text    CHECK (non_merch_type IN ('SERVICE_CHARGE','DELIVERY','TIP','DEPOSIT','GIFT_CARD','OTHER')),
    sku            text    UNIQUE,
    barcode        text    UNIQUE,
    price_minor    bigint  NOT NULL CHECK (price_minor >= 0),
    tax_rate_id    uuid    REFERENCES tax_rate(id),
    kitchen_print  boolean NOT NULL DEFAULT false,
    track_stock    boolean NOT NULL DEFAULT false,
    sort_order     int     NOT NULL DEFAULT 0,
    active         boolean NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CHECK ((item_class = 'NON_MERCHANDISE') = (non_merch_type IS NOT NULL)),
    CHECK (item_class = 'MERCHANDISE' OR NOT track_stock)
);
CREATE INDEX IF NOT EXISTS ix_product_category ON product(category_id) WHERE active;
