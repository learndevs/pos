INSERT INTO role (id, code, name) VALUES
  ('00000000-0000-7000-8000-000000000001', 'OWNER',   'Owner'),
  ('00000000-0000-7000-8000-000000000002', 'MANAGER', 'Manager'),
  ('00000000-0000-7000-8000-000000000003', 'CASHIER', 'Cashier');

INSERT INTO role_permission (role_id, permission)
SELECT r.id, p.permission FROM role r JOIN (VALUES
  ('OWNER','USER_MANAGE'),('OWNER','SETTINGS_MANAGE'),('OWNER','LICENSE_MANAGE'),
  ('OWNER','PRODUCT_MANAGE'),('OWNER','STOCK_ADJUST'),('OWNER','ORDER_VOID'),
  ('OWNER','DISCOUNT_UNLIMITED'),('OWNER','REPORT_VIEW'),('OWNER','DAY_CLOSE'),
  ('OWNER','DAY_OPEN'),('OWNER','CASH_MOVEMENT'),('OWNER','DRAWER_OPEN'),('OWNER','ORDER_CREATE'),
  ('MANAGER','PRODUCT_MANAGE'),('MANAGER','STOCK_ADJUST'),('MANAGER','ORDER_VOID'),
  ('MANAGER','DISCOUNT_UNLIMITED'),('MANAGER','REPORT_VIEW'),('MANAGER','DAY_CLOSE'),
  ('MANAGER','DAY_OPEN'),('MANAGER','CASH_MOVEMENT'),('MANAGER','DRAWER_OPEN'),('MANAGER','ORDER_CREATE'),
  ('CASHIER','ORDER_CREATE'),('CASHIER','DAY_OPEN')
) AS p(role_code, permission) ON p.role_code = r.code;

INSERT INTO app_setting (key, value) VALUES
  ('business',  '{"name":"My Restaurant","address":"","phone":"","currency":"USD","minorUnitDigits":2}'),
  ('receipt',   '{"header":"","footer":"Thank you!","showTaxBreakdown":true}'),
  ('printer',   '{"transport":"SPOOLER","name":"","columns":48,"codePage":"CP437","drawerPin":0,"cut":true}'),
  ('discount',  '{"cashierMaxPercent":10}');
