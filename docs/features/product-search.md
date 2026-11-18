# Feature: Product search

## Why
Find items that are not on a menu button, or scan / type a SKU or barcode.

## Flow
Header **Product search** -> type (debounced 200 ms) -> `GET /api/products/search?q=` -> tap a result or press Enter (first result) -> same as pressing a product button (keypad quantity applies).

## Rules
Active products only; name contains (case-insensitive) or exact SKU / barcode; max 30 results.

## API
| Method | Path | Permission |
|---|---|---|
| GET | `/api/products/search?q=` | ORDER_CREATE |

## UI
`features/sales/ProductSearch.tsx` (modal), `HeaderActions.tsx`; dispatches `{ type: "product", productId }`.
