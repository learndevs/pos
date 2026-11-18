---
name: pos-admin
description: Admin panel conventions (admin-ui + admin-app): feature registry for the side nav and header search, page layout with titles, list + inspector with tabs (no pop-ups), view/edit/delete row icons, and how a new configurable entity is made broadcastable. Use for any admin screen or admin API work.
---

# Admin panel (admin-ui + admin-app)

## Shell
- `admin-ui/src/App.tsx` `FEATURES` is the single registry of configuration features: `key`, `title`, `category`
  (side-nav group: Overview, Catalog, Till layout, Sync, Reports), `description`, `keywords`, `icon`.
  The side nav, the header breadcrumb (`Category / Title`) and the header search (Ctrl/Cmd+K, inline dropdown, title
  matches ranked first) are all built from it. **A new screen = one entry here + one page component.**
- Every page starts with `PageHead` (title + one-line explanation + primary action).

## UI rules
- **No pop-up windows / modals.** Editing happens in an `Inspector` side panel next to the list (`Split`), or inline
  (tree rename, `ConfirmButton` / `RowActions` inline confirms). Dropdown lists (search results) are fine.
- **Tabs** (`Tabs`) for page sections (Menus: Sale / Tender / Item; Packages: New / History) and inside inspectors
  (Product: General / Price & tax / Codes & stock).
- **Records**: every row has `RowActions` icons: View (read-only `Facts`), Edit (form + Save / Cancel), Delete
  (inline confirm). Row click = View. After Save the inspector returns to view mode and shows "Saved".
- Controls: `Segmented` for 2-5 options, `Toggle` for booleans, `Field` for inputs, `Notice` for results/warnings.
- Style only with the CSS tokens in `admin-ui/src/styles.css`; icons from `src/icons.tsx` (inline SVG).

## Data rules
- Master data is edited through the **shared entity methods** (`Product.create/update`, `MenuButton.update`, ...);
  every save also calls `ChangeLog.record(entity, id, recordV1, adminUserId)` in the same transaction.
- **Delete = deactivate + change log** (tills drop it on the next broadcast; past sales keep their copy). Refuse
  deleting things still in use with a 409 and a clear message (category with products, tax rate on products).
- Admin lists show active records only.

## Make a new entity configurable + broadcastable (example: settings)
1. Shared table in `db/master` (next global version), entity create/update rules in its module.
2. Contract: `XxxV1` record + list in `PackageContent` + file in `PackageZip` (additive = still v1) + mapper.
3. Admin: edit service + controller (`/admin/api/...`), change log on every save/delete.
4. `PackageService`: collect (FULL + date range) incl. references; add to `Group` (dashboard card) and counts.
5. Till: `SyncApplier` applies it in reference order.
6. UI: page with Split/Inspector/RowActions + `FEATURES` entry; Packages builder group.
7. Tests (light): extend `AdminSyncFlowTest` / `PackageSyncTest`; then run the admin jar on `pos_admin_test` and
   drive it (see `tools/e2e`, hidden Electron window) before reporting done.
