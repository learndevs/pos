-- Module: master-schema (shared by POS and admin). Menus become synced master data.
-- POS: tables exist (V3-V5), statements are no-ops except the two structural changes below.
-- Admin: creates the tables and the same base rows (generated from the POS schema state after V12).

CREATE TABLE IF NOT EXISTS pos_function (
    code                 text PRIMARY KEY,
    name                 text    NOT NULL,
    input                text    NOT NULL DEFAULT 'NONE' CHECK (input IN ('NONE','NUMBER','AMOUNT','CONFIRM')),
    required_permission  text,
    needs_selected_line  boolean NOT NULL DEFAULT false,
    available            boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS menu (
    id    uuid PRIMARY KEY,
    code  text NOT NULL UNIQUE CHECK (code IN ('SALE','TENDER','ITEM')),
    name  text NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_folder (
    id                uuid PRIMARY KEY,
    menu_id           uuid NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    parent_folder_id  uuid REFERENCES menu_folder(id) ON DELETE CASCADE,
    name              text NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_menu_one_root ON menu_folder(menu_id) WHERE parent_folder_id IS NULL;
-- folders are deactivated, not deleted (synced)
ALTER TABLE menu_folder ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;

CREATE TABLE IF NOT EXISTS menu_button (
    id                uuid PRIMARY KEY,
    folder_id         uuid    NOT NULL REFERENCES menu_folder(id) ON DELETE CASCADE,
    page              int     NOT NULL DEFAULT 0 CHECK (page >= 0),
    grid_row          int     NOT NULL CHECK (grid_row BETWEEN 0 AND 3),
    grid_col          int     NOT NULL CHECK (grid_col BETWEEN 0 AND 2),
    label             text    NOT NULL,
    color             text,
    action_type       text    NOT NULL CHECK (action_type IN ('PRODUCT','OPEN_FOLDER','TENDER','FUNCTION','SWITCH_MENU')),
    product_id        uuid    REFERENCES product(id),
    target_folder_id  uuid    REFERENCES menu_folder(id),
    tender_method     text    CHECK (tender_method IN ('CASH','CARD','OTHER')),
    amount_minor      bigint  CHECK (amount_minor > 0),
    function_code     text    REFERENCES pos_function(code),
    target_menu_id    uuid    REFERENCES menu(id),
    active            boolean NOT NULL DEFAULT true,
    CHECK ((action_type = 'PRODUCT')     = (product_id IS NOT NULL)),
    CHECK ((action_type = 'OPEN_FOLDER') = (target_folder_id IS NOT NULL)),
    CHECK ((action_type = 'TENDER')      = (tender_method IS NOT NULL)),
    CHECK (action_type = 'TENDER' OR amount_minor IS NULL),
    CHECK ((action_type = 'FUNCTION')    = (function_code IS NOT NULL)),
    CHECK ((action_type = 'SWITCH_MENU') = (target_menu_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_menu_button_folder ON menu_button(folder_id) WHERE active;
-- A removed (inactive) button frees its slot: the slot rule applies to active buttons only.
ALTER TABLE menu_button DROP CONSTRAINT IF EXISTS menu_button_slot_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_menu_button_slot_active ON menu_button(folder_id, page, grid_row, grid_col) WHERE active;

-- base rows (identical ids on POS and admin)
INSERT INTO pos_function (code, name, input, required_permission, needs_selected_line, available) VALUES
  ('CANCEL_TXN', 'Cancel sale', 'CONFIRM', 'ORDER_VOID', false, true),
  ('DESELECT_LINE', 'Done', 'NONE', NULL, true, true),
  ('HOLD_TXN', 'Hold', 'NONE', 'ORDER_CREATE', false, false),
  ('LINE_DISCOUNT', 'Line discount', 'NUMBER', 'ORDER_CREATE', true, true),
  ('OPEN_DRAWER', 'Open drawer', 'NONE', 'DRAWER_OPEN', false, true),
  ('PAID_IN', 'Paid in', 'AMOUNT', 'CASH_MOVEMENT', false, false),
  ('PAID_OUT', 'Paid out', 'AMOUNT', 'CASH_MOVEMENT', false, false),
  ('PRICE_OVERRIDE', 'Price override', 'AMOUNT', 'DISCOUNT_UNLIMITED', true, false),
  ('QTY', 'Quantity', 'NUMBER', 'ORDER_CREATE', true, true),
  ('QTY_MINUS', 'Quantity -1', 'NONE', 'ORDER_CREATE', true, true),
  ('QTY_PLUS', 'Quantity +1', 'NONE', 'ORDER_CREATE', true, true),
  ('RECALL_TXN', 'Recall', 'NONE', 'ORDER_CREATE', false, false),
  ('REFUND', 'Refund', 'NONE', 'ORDER_VOID', false, false),
  ('REPRINT', 'Reprint', 'NONE', 'ORDER_CREATE', false, false),
  ('SET_TABLE', 'Table', 'NUMBER', 'ORDER_CREATE', false, false),
  ('SIGN_OUT', 'Sign out', 'NONE', NULL, false, true),
  ('TAKEAWAY_TOGGLE', 'Dine in / Takeaway', 'NONE', 'ORDER_CREATE', false, true),
  ('TXN_DISCOUNT', 'Sale discount', 'NUMBER', 'ORDER_CREATE', false, false),
  ('VOID_LINE', 'Void line', 'CONFIRM', 'ORDER_CREATE', true, true)
ON CONFLICT DO NOTHING;
INSERT INTO menu (id, code, name) VALUES
  ('00000000-0000-7000-8000-0000000a0003', 'ITEM', 'Item'),
  ('00000000-0000-7000-8000-0000000a0001', 'SALE', 'Sale'),
  ('00000000-0000-7000-8000-0000000a0002', 'TENDER', 'Tender')
ON CONFLICT DO NOTHING;
INSERT INTO menu_folder (id, menu_id, parent_folder_id, name) VALUES
  ('00000000-0000-7000-8000-0000000b0001', '00000000-0000-7000-8000-0000000a0001', NULL, 'Sale'),
  ('00000000-0000-7000-8000-0000000b0002', '00000000-0000-7000-8000-0000000a0002', NULL, 'Tender'),
  ('00000000-0000-7000-8000-0000000b0003', '00000000-0000-7000-8000-0000000a0003', NULL, 'Item')
ON CONFLICT DO NOTHING;
INSERT INTO menu_button (id, folder_id, page, grid_row, grid_col, label, color, action_type, product_id, target_folder_id, tender_method, amount_minor, function_code, target_menu_id, active) VALUES
  ('00000000-0000-7000-8000-0000000c0003', '00000000-0000-7000-8000-0000000b0001', 0, 2, 2, 'Dine in / Takeaway', 'grey', 'FUNCTION', NULL, NULL, NULL, NULL, 'TAKEAWAY_TOGGLE', NULL, true),
  ('00000000-0000-7000-8000-0000000c0004', '00000000-0000-7000-8000-0000000b0001', 0, 3, 2, 'Cancel sale', 'red', 'FUNCTION', NULL, NULL, NULL, NULL, 'CANCEL_TXN', NULL, true),
  ('00000000-0000-7000-8000-0000000c0005', '00000000-0000-7000-8000-0000000b0001', 0, 3, 0, 'Open drawer', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'OPEN_DRAWER', NULL, true),
  ('00000000-0000-7000-8000-0000000c0101', '00000000-0000-7000-8000-0000000b0002', 0, 0, 0, 'Cash', 'green', 'TENDER', NULL, NULL, 'CASH', NULL, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0102', '00000000-0000-7000-8000-0000000b0002', 0, 0, 1, 'Card', 'blue', 'TENDER', NULL, NULL, 'CARD', NULL, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0103', '00000000-0000-7000-8000-0000000b0002', 0, 0, 2, 'Other', 'blue', 'TENDER', NULL, NULL, 'OTHER', NULL, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0104', '00000000-0000-7000-8000-0000000b0002', 0, 1, 1, 'Cash 10', 'green', 'TENDER', NULL, NULL, 'CASH', 1000, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0105', '00000000-0000-7000-8000-0000000b0002', 0, 1, 2, 'Cash 20', 'green', 'TENDER', NULL, NULL, 'CASH', 2000, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0106', '00000000-0000-7000-8000-0000000b0002', 0, 2, 0, 'Cash 50', 'green', 'TENDER', NULL, NULL, 'CASH', 5000, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0107', '00000000-0000-7000-8000-0000000b0002', 0, 2, 1, 'Cash 100', 'green', 'TENDER', NULL, NULL, 'CASH', 10000, NULL, NULL, true),
  ('00000000-0000-7000-8000-0000000c0108', '00000000-0000-7000-8000-0000000b0002', 0, 2, 2, 'Open drawer', 'grey', 'FUNCTION', NULL, NULL, NULL, NULL, 'OPEN_DRAWER', NULL, true),
  ('00000000-0000-7000-8000-0000000c0109', '00000000-0000-7000-8000-0000000b0002', 0, 3, 1, 'Cancel sale', 'red', 'FUNCTION', NULL, NULL, NULL, NULL, 'CANCEL_TXN', NULL, true),
  ('00000000-0000-7000-8000-0000000c0110', '00000000-0000-7000-8000-0000000b0002', 0, 3, 2, 'Back to sale', 'grey', 'SWITCH_MENU', NULL, NULL, NULL, NULL, NULL, '00000000-0000-7000-8000-0000000a0001', true),
  ('00000000-0000-7000-8000-0000000c0201', '00000000-0000-7000-8000-0000000b0003', 0, 0, 0, 'Qty +1', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'QTY_PLUS', NULL, true),
  ('00000000-0000-7000-8000-0000000c0202', '00000000-0000-7000-8000-0000000b0003', 0, 0, 1, 'Qty -1', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'QTY_MINUS', NULL, true),
  ('00000000-0000-7000-8000-0000000c0203', '00000000-0000-7000-8000-0000000b0003', 0, 0, 2, 'Set qty', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'QTY', NULL, true),
  ('00000000-0000-7000-8000-0000000c0204', '00000000-0000-7000-8000-0000000b0003', 0, 1, 0, 'Void line', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'VOID_LINE', NULL, true),
  ('00000000-0000-7000-8000-0000000c0205', '00000000-0000-7000-8000-0000000b0003', 0, 1, 1, 'Price override', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'PRICE_OVERRIDE', NULL, true),
  ('00000000-0000-7000-8000-0000000c0206', '00000000-0000-7000-8000-0000000b0003', 0, 1, 2, 'Line discount', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'LINE_DISCOUNT', NULL, true),
  ('00000000-0000-7000-8000-0000000c0207', '00000000-0000-7000-8000-0000000b0003', 0, 3, 2, 'Done', NULL, 'FUNCTION', NULL, NULL, NULL, NULL, 'DESELECT_LINE', NULL, true)
ON CONFLICT DO NOTHING;
