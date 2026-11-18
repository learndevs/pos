-- Menu grid changes from 3 rows x 4 columns to 4 rows x 3 columns (narrower right-hand panel).
-- Each page keeps 12 slots; buttons keep their reading order: slot = row * 4 + col  ->  row = slot / 3, col = slot % 3.
ALTER TABLE menu_button DROP CONSTRAINT menu_button_folder_id_page_grid_row_grid_col_key;
ALTER TABLE menu_button DROP CONSTRAINT menu_button_grid_row_check;
ALTER TABLE menu_button DROP CONSTRAINT menu_button_grid_col_check;

UPDATE menu_button SET grid_row = (grid_row * 4 + grid_col) / 3,
                       grid_col = (grid_row * 4 + grid_col) % 3;

ALTER TABLE menu_button ADD CONSTRAINT menu_button_grid_row_check CHECK (grid_row BETWEEN 0 AND 3);   -- 4 rows
ALTER TABLE menu_button ADD CONSTRAINT menu_button_grid_col_check CHECK (grid_col BETWEEN 0 AND 2);   -- 3 columns
ALTER TABLE menu_button ADD CONSTRAINT menu_button_slot_key UNIQUE (folder_id, page, grid_row, grid_col);
