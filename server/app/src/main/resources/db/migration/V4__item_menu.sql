-- ITEM menu: shown while a basket line is selected (qty +/-, set qty, void, ...).
ALTER TABLE menu DROP CONSTRAINT menu_code_check;
ALTER TABLE menu ADD CONSTRAINT menu_code_check CHECK (code IN ('SALE','TENDER','ITEM'));

INSERT INTO pos_function (code, name, input, required_permission, needs_selected_line, available) VALUES
  ('QTY_PLUS',      'Quantity +1', 'NONE', 'ORDER_CREATE', true,  true),
  ('QTY_MINUS',     'Quantity -1', 'NONE', 'ORDER_CREATE', true,  true),
  ('DESELECT_LINE', 'Done',        'NONE', NULL,           true,  true);

INSERT INTO menu (id, code, name) VALUES ('00000000-0000-7000-8000-0000000a0003', 'ITEM', 'Item');
INSERT INTO menu_folder (id, menu_id, parent_folder_id, name)
VALUES ('00000000-0000-7000-8000-0000000b0003', '00000000-0000-7000-8000-0000000a0003', NULL, 'Item');

INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, action_type, function_code) VALUES
  ('00000000-0000-7000-8000-0000000c0201', '00000000-0000-7000-8000-0000000b0003', 0, 0, 'Qty +1',         'FUNCTION', 'QTY_PLUS'),
  ('00000000-0000-7000-8000-0000000c0202', '00000000-0000-7000-8000-0000000b0003', 0, 1, 'Qty -1',         'FUNCTION', 'QTY_MINUS'),
  ('00000000-0000-7000-8000-0000000c0203', '00000000-0000-7000-8000-0000000b0003', 0, 2, 'Set qty',        'FUNCTION', 'QTY'),
  ('00000000-0000-7000-8000-0000000c0204', '00000000-0000-7000-8000-0000000b0003', 0, 3, 'Void line',      'FUNCTION', 'VOID_LINE'),
  ('00000000-0000-7000-8000-0000000c0205', '00000000-0000-7000-8000-0000000b0003', 1, 0, 'Price override', 'FUNCTION', 'PRICE_OVERRIDE'),
  ('00000000-0000-7000-8000-0000000c0206', '00000000-0000-7000-8000-0000000b0003', 1, 1, 'Line discount',  'FUNCTION', 'LINE_DISCOUNT'),
  ('00000000-0000-7000-8000-0000000c0207', '00000000-0000-7000-8000-0000000b0003', 2, 3, 'Done',           'FUNCTION', 'DESELECT_LINE');

-- Line functions now live in the ITEM menu; the SALE root keeps sale-level functions.
DELETE FROM menu_button WHERE id IN ('00000000-0000-7000-8000-0000000c0001', '00000000-0000-7000-8000-0000000c0002');
UPDATE menu_button SET grid_col = 0 WHERE id = '00000000-0000-7000-8000-0000000c0003';   -- Dine in / Takeaway
INSERT INTO menu_button (id, folder_id, grid_row, grid_col, label, action_type, function_code) VALUES
  ('00000000-0000-7000-8000-0000000c0005', '00000000-0000-7000-8000-0000000b0001', 2, 1, 'Open drawer', 'FUNCTION', 'OPEN_DRAWER');
