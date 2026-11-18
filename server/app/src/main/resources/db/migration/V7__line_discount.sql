-- Module: sales. Feature: line discount (docs/features/line-discount.md).

-- Percent discount kept on the line (basis points: 1000 = 10%) so a quantity change re-applies it.
-- discount_minor stays the computed amount.
ALTER TABLE sale_item ADD COLUMN discount_bp int CHECK (discount_bp BETWEEN 0 AND 10000);

UPDATE pos_function SET available = true WHERE code = 'LINE_DISCOUNT';

-- Default receipt template: show the discount under the item (only if the template still has the default unit-price row).
UPDATE receipt_template
SET html = replace(html,
    '{{#showUnit}}<tr><td></td><td class="muted">@ {{unitPrice}}</td><td></td></tr>{{/showUnit}}',
    '{{#showUnit}}<tr><td></td><td class="muted">@ {{unitPrice}}</td><td></td></tr>{{/showUnit}}
    {{#hasDiscount}}<tr><td></td><td class="muted">Discount {{discountPercent}}%</td><td class="amt">-{{discount}}</td></tr>{{/hasDiscount}}'),
    updated_at = now()
WHERE id = '00000000-0000-7000-8000-0000000e0001';
