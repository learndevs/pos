-- Module: sales / settings / sync-client (POS only). Independent tills: each till knows its device code
-- (from admin) and prints it in receipt numbers so they are unique across tills: T01-20261119-0001.
ALTER TABLE pos_transaction ADD COLUMN device_code text;

-- identity (refreshed from admin on every sync) and admin connection (set from the till's Device panel)
INSERT INTO app_setting (key, value) VALUES ('device', '{"code":"T01","name":"Till 1","terminalId":null}')
ON CONFLICT (key) DO NOTHING;

-- default receipt template: show the till
UPDATE receipt_template
SET html = replace(html,
    '<tr><td>Txn</td><td class="r">#{{sale.txnNo}}</td></tr>',
    '<tr><td>Till</td><td class="r">{{device.code}}{{#device.name}} · {{device.name}}{{/device.name}}</td></tr>
    <tr><td>Txn</td><td class="r">#{{sale.txnNo}}</td></tr>'),
    updated_at = now()
WHERE id = '00000000-0000-7000-8000-0000000e0001';
