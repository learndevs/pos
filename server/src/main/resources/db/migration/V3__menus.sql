-- Button menus for the sale screen: menu -> folder tree -> 3x4 grid of buttons per page.

-- Catalogue of built-in POS functions. Behaviour lives in code; this table lists what a button may call.
CREATE TABLE pos_function (
    code                 text PRIMARY KEY,
    name                 text    NOT NULL,
    input                text    NOT NULL DEFAULT 'NONE' CHECK (input IN ('NONE','NUMBER','AMOUNT','CONFIRM')),
    required_permission  text,                                -- role_permission.permission; NULL = anyone signed in
    needs_selected_line  boolean NOT NULL DEFAULT false,
    available            boolean NOT NULL DEFAULT true        -- false = not implemented yet (button shown disabled)
);

CREATE TABLE menu (
    id    uuid PRIMARY KEY,
    code  text NOT NULL UNIQUE CHECK (code IN ('SALE','TENDER')),
    name  text NOT NULL
);

CREATE TABLE menu_folder (
    id                uuid PRIMARY KEY,
    menu_id           uuid NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    parent_folder_id  uuid REFERENCES menu_folder(id) ON DELETE CASCADE,   -- NULL = the menu's root
    name              text NOT NULL
);
CREATE UNIQUE INDEX ux_menu_one_root ON menu_folder(menu_id) WHERE parent_folder_id IS NULL;

CREATE TABLE menu_button (
    id                uuid PRIMARY KEY,
    folder_id         uuid    NOT NULL REFERENCES menu_folder(id) ON DELETE CASCADE,
    page              int     NOT NULL DEFAULT 0 CHECK (page >= 0),
    grid_row          int     NOT NULL CHECK (grid_row BETWEEN 0 AND 2),   -- 3 rows
    grid_col          int     NOT NULL CHECK (grid_col BETWEEN 0 AND 3),   -- 4 columns
    label             text    NOT NULL,
    color             text,                                                 -- token name, e.g. 'blue', 'green'
    action_type       text    NOT NULL CHECK (action_type IN ('PRODUCT','OPEN_FOLDER','TENDER','FUNCTION','SWITCH_MENU')),
    product_id        uuid    REFERENCES product(id),
    target_folder_id  uuid    REFERENCES menu_folder(id),
    tender_method     text    CHECK (tender_method IN ('CASH','CARD','OTHER')),
    amount_minor      bigint  CHECK (amount_minor > 0),                     -- preset tender amount; NULL = keypad or exact
    function_code     text    REFERENCES pos_function(code),
    target_menu_id    uuid    REFERENCES menu(id),
    active            boolean NOT NULL DEFAULT true,
    UNIQUE (folder_id, page, grid_row, grid_col),
    -- each action type fills exactly its own column(s)
    CHECK ((action_type = 'PRODUCT')     = (product_id IS NOT NULL)),
    CHECK ((action_type = 'OPEN_FOLDER') = (target_folder_id IS NOT NULL)),
    CHECK ((action_type = 'TENDER')      = (tender_method IS NOT NULL)),
    CHECK (action_type = 'TENDER' OR amount_minor IS NULL),
    CHECK ((action_type = 'FUNCTION')    = (function_code IS NOT NULL)),
    CHECK ((action_type = 'SWITCH_MENU') = (target_menu_id IS NOT NULL))
);
CREATE INDEX ix_menu_button_folder ON menu_button(folder_id) WHERE active;

INSERT INTO pos_function (code, name, input, required_permission, needs_selected_line, available) VALUES
  ('QTY',            'Quantity',        'NUMBER',  'ORDER_CREATE', true,  true),
  ('VOID_LINE',      'Void line',       'CONFIRM', 'ORDER_CREATE', true,  true),
  ('CANCEL_TXN',     'Cancel sale',     'CONFIRM', 'ORDER_VOID',   false, true),
  ('OPEN_DRAWER',    'Open drawer',     'NONE',    'DRAWER_OPEN',  false, true),
  ('SIGN_OUT',       'Sign out',        'NONE',    NULL,           false, true),
  ('TAKEAWAY_TOGGLE','Dine in / Takeaway','NONE',  'ORDER_CREATE', false, true),
  ('PRICE_OVERRIDE', 'Price override',  'AMOUNT',  'DISCOUNT_UNLIMITED', true, false),
  ('LINE_DISCOUNT',  'Line discount',   'NUMBER',  'ORDER_CREATE', true,  false),
  ('TXN_DISCOUNT',   'Sale discount',   'NUMBER',  'ORDER_CREATE', false, false),
  ('SET_TABLE',      'Table',           'NUMBER',  'ORDER_CREATE', false, false),
  ('HOLD_TXN',       'Hold',            'NONE',    'ORDER_CREATE', false, false),
  ('RECALL_TXN',     'Recall',          'NONE',    'ORDER_CREATE', false, false),
  ('REFUND',         'Refund',          'NONE',    'ORDER_VOID',   false, false),
  ('REPRINT',        'Reprint',         'NONE',    'ORDER_CREATE', false, false),
  ('PAID_IN',        'Paid in',         'AMOUNT',  'CASH_MOVEMENT', false, false),
  ('PAID_OUT',       'Paid out',        'AMOUNT',  'CASH_MOVEMENT', false, false);

-- Menus and root folders
INSERT INTO menu (id, code, name) VALUES
  ('00000000-0000-7000-8000-0000000a0001', 'SALE',   'Sale'),
  ('00000000-0000-7000-8000-0000000a0002', 'TENDER', 'Tender');
INSERT INTO menu_folder (id, menu_id, parent_folder_id, name) VALUES
  ('00000000-0000-7000-8000-0000000b0001', '00000000-0000-7000-8000-0000000a0001', NULL, 'Sale'),
  ('00000000-0000-7000-8000-0000000b0002', '00000000-0000-7000-8000-0000000a0002', NULL, 'Tender');

-- Sale root, bottom row: functions (rows 0-1 are left for categories / products)
INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, color, action_type, function_code) VALUES
  ('00000000-0000-7000-8000-0000000c0001', '00000000-0000-7000-8000-0000000b0001', 2, 0, 'Qty',         'grey', 'FUNCTION', 'QTY'),
  ('00000000-0000-7000-8000-0000000c0002', '00000000-0000-7000-8000-0000000b0001', 2, 1, 'Void line',   'grey', 'FUNCTION', 'VOID_LINE'),
  ('00000000-0000-7000-8000-0000000c0003', '00000000-0000-7000-8000-0000000b0001', 2, 2, 'Dine in / Takeaway', 'grey', 'FUNCTION', 'TAKEAWAY_TOGGLE'),
  ('00000000-0000-7000-8000-0000000c0004', '00000000-0000-7000-8000-0000000b0001', 2, 3, 'Cancel sale', 'red',  'FUNCTION', 'CANCEL_TXN');

-- Tender root
INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, color, action_type, tender_method, amount_minor) VALUES
  ('00000000-0000-7000-8000-0000000c0101', '00000000-0000-7000-8000-0000000b0002', 0, 0, 'Cash',   'green', 'TENDER', 'CASH', NULL),
  ('00000000-0000-7000-8000-0000000c0102', '00000000-0000-7000-8000-0000000b0002', 0, 1, 'Card',   'blue',  'TENDER', 'CARD', NULL),
  ('00000000-0000-7000-8000-0000000c0103', '00000000-0000-7000-8000-0000000b0002', 0, 2, 'Other',  'blue',  'TENDER', 'OTHER', NULL),
  ('00000000-0000-7000-8000-0000000c0104', '00000000-0000-7000-8000-0000000b0002', 1, 0, 'Cash 10',  'green', 'TENDER', 'CASH', 1000),
  ('00000000-0000-7000-8000-0000000c0105', '00000000-0000-7000-8000-0000000b0002', 1, 1, 'Cash 20',  'green', 'TENDER', 'CASH', 2000),
  ('00000000-0000-7000-8000-0000000c0106', '00000000-0000-7000-8000-0000000b0002', 1, 2, 'Cash 50',  'green', 'TENDER', 'CASH', 5000),
  ('00000000-0000-7000-8000-0000000c0107', '00000000-0000-7000-8000-0000000b0002', 1, 3, 'Cash 100', 'green', 'TENDER', 'CASH', 10000);
INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, color, action_type, function_code) VALUES
  ('00000000-0000-7000-8000-0000000c0108', '00000000-0000-7000-8000-0000000b0002', 2, 0, 'Open drawer', 'grey', 'FUNCTION', 'OPEN_DRAWER'),
  ('00000000-0000-7000-8000-0000000c0109', '00000000-0000-7000-8000-0000000b0002', 2, 2, 'Cancel sale', 'red',  'FUNCTION', 'CANCEL_TXN');
INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, color, action_type, target_menu_id) VALUES
  ('00000000-0000-7000-8000-0000000c0110', '00000000-0000-7000-8000-0000000b0002', 2, 3, 'Back to sale', 'grey', 'SWITCH_MENU', '00000000-0000-7000-8000-0000000a0001');
