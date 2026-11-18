-- Module: sync-server (admin database). Menus are part of the change log.
ALTER TABLE config_change DROP CONSTRAINT config_change_entity_check;
ALTER TABLE config_change ADD CONSTRAINT config_change_entity_check
    CHECK (entity IN ('TAX_RATE','CATEGORY','PRODUCT','MENU_FOLDER','MENU_BUTTON'));
