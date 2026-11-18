-- Transaction number: shown in the header from the moment a transaction starts (gaps allowed:
-- cancelled transactions keep theirs). Receipt numbers stay separate and gap-free.
CREATE SEQUENCE pos_transaction_no_seq;
ALTER TABLE pos_transaction ADD COLUMN txn_no bigint NOT NULL DEFAULT nextval('pos_transaction_no_seq');
ALTER TABLE pos_transaction ADD CONSTRAINT pos_transaction_txn_no_key UNIQUE (txn_no);
ALTER SEQUENCE pos_transaction_no_seq OWNED BY pos_transaction.txn_no;

-- Receipt layout as an HTML (Mustache) template, editable without code changes.
CREATE TABLE receipt_template (
    id              uuid PRIMARY KEY,
    name            text    NOT NULL,
    html            text    NOT NULL,
    paper_width_mm  int     NOT NULL DEFAULT 80 CHECK (paper_width_mm BETWEEN 40 AND 120),
    active          boolean NOT NULL DEFAULT true,
    updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_receipt_template_one_active ON receipt_template ((true)) WHERE active;

INSERT INTO receipt_template (id, name, html) VALUES ('00000000-0000-7000-8000-0000000e0001', 'Default 80mm', $html$<!doctype html>
<html>
<head>
<meta charset="utf-8">
<style>
  @page { size: 80mm auto; margin: 0; }
  * { box-sizing: border-box; }
  body { width: 80mm; margin: 0; padding: 4mm 4mm 6mm; font: 11px/1.35 "Courier New", monospace; color: #000; }
  .c { text-align: center; }
  .r { text-align: right; }
  .b { font-weight: bold; }
  h1 { font-size: 15px; margin: 0 0 2px; text-align: center; }
  .muted { color: #333; }
  hr { border: 0; border-top: 1px dashed #000; margin: 6px 0; }
  table { width: 100%; border-collapse: collapse; }
  td { padding: 1px 0; vertical-align: top; }
  .qty { width: 9mm; }
  .amt { width: 20mm; text-align: right; }
  .total td { font-size: 14px; font-weight: bold; padding-top: 3px; }
</style>
</head>
<body>
  <h1>{{business.name}}</h1>
  {{#business.address}}<div class="c muted">{{business.address}}</div>{{/business.address}}
  {{#business.phone}}<div class="c muted">Tel {{business.phone}}</div>{{/business.phone}}
  {{#receipt.header}}<div class="c">{{receipt.header}}</div>{{/receipt.header}}
  <hr>
  <table>
    <tr><td>Receipt</td><td class="r b">{{sale.receiptNo}}</td></tr>
    <tr><td>Txn</td><td class="r">#{{sale.txnNo}}</td></tr>
    <tr><td>Date</td><td class="r">{{sale.date}} {{sale.time}}</td></tr>
    <tr><td>Cashier</td><td class="r">{{sale.cashier}}</td></tr>
    <tr><td>Order</td><td class="r">{{sale.orderType}}</td></tr>
  </table>
  <hr>
  <table>
    {{#lines}}
    <tr><td class="qty">{{qty}}x</td><td>{{name}}</td><td class="amt">{{amount}}</td></tr>
    {{#showUnit}}<tr><td></td><td class="muted">@ {{unitPrice}}</td><td></td></tr>{{/showUnit}}
    {{/lines}}
  </table>
  <hr>
  <table>
    <tr><td>Subtotal</td><td class="amt">{{totals.subtotal}}</td></tr>
    <tr class="total"><td>TOTAL</td><td class="amt">{{totals.total}}</td></tr>
    {{#tenders}}
    <tr><td>{{method}}</td><td class="amt">{{tendered}}</td></tr>
    {{/tenders}}
    {{#totals.hasChange}}<tr><td class="b">Change</td><td class="amt b">{{totals.change}}</td></tr>{{/totals.hasChange}}
    <tr><td class="muted">Tax included</td><td class="amt muted">{{totals.tax}}</td></tr>
  </table>
  <hr>
  <div class="c">{{receipt.footer}}</div>
</body>
</html>$html$);
