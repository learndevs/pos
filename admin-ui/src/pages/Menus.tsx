import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, money, type ActionType, type MenuButton, type MenuConfig, type MenuDef, type MenuFolder, type PosFunction, type Product } from "../api";
import { ConfirmButton, ErrorText, Field, Inspector, PageHead, Segmented, Tabs } from "../ui";

const ROWS = 4;
const COLS = 3;
type Slot = { page: number; gridRow: number; gridCol: number };
type Draft = Partial<MenuButton> & Slot & { folderId: string };

const ACTIONS: [ActionType, string][] = [["PRODUCT", "Product"], ["OPEN_FOLDER", "Folder"], ["TENDER", "Tender"], ["FUNCTION", "Function"], ["SWITCH_MENU", "Switch menu"]];
const COLORS: [string, string][] = [["", "Default"], ["blue", "Blue"], ["green", "Green"], ["orange", "Orange"], ["purple", "Purple"], ["red", "Red"], ["grey", "Grey"]];

/** Menu designer: SALE / TENDER / ITEM tabs, folder tree, page tabs, 4 x 3 grid, tabbed button inspector. */
export function Menus() {
  const qc = useQueryClient();
  const config = useQuery({ queryKey: ["menus"], queryFn: () => api.get<MenuConfig>("/admin/api/menus") });
  const [menuCode, setMenuCode] = useState<MenuDef["code"]>("SALE");
  const [folderId, setFolderId] = useState<string>();
  const [page, setPage] = useState(0);
  const [extraPages, setExtraPages] = useState(0);
  const [draft, setDraft] = useState<Draft | null>(null);

  const menu = config.data?.menus.find((m) => m.code === menuCode);
  const folders = useMemo(() => (config.data?.folders ?? []).filter((f) => f.menuId === menu?.id), [config.data, menu]);
  const currentFolder = folderId ?? menu?.rootFolderId;
  const buttons = (config.data?.buttons ?? []).filter((b) => b.folderId === currentFolder);
  const pages = Math.max(1, ...buttons.map((b) => b.page + 1)) + extraPages;
  useEffect(() => { setFolderId(undefined); setPage(0); setDraft(null); setExtraPages(0); }, [menuCode]);
  useEffect(() => { setPage(0); setDraft(null); setExtraPages(0); }, [folderId]);

  const refresh = () => { qc.invalidateQueries({ queryKey: ["menus"] }); qc.invalidateQueries({ queryKey: ["overview"] }); };
  const path = (id?: string): MenuFolder[] => {
    const f = folders.find((x) => x.id === id);
    return f ? [...(f.parentFolderId ? path(f.parentFolderId) : []), f] : [];
  };

  return (
    <>
      <PageHead title="Menus" sub="Design the till's button menus. Changes reach tills when you broadcast Menus." />
      <Tabs value={menuCode} onChange={setMenuCode} tabs={(config.data?.menus ?? []).slice().sort((a, b) => "SALE TENDER ITEM".indexOf(a.code) - "SALE TENDER ITEM".indexOf(b.code)).map((m) => [m.code, m.name])} />
      <ErrorText error={config.error} />
      {menu && currentFolder && (
        <div className={`designer ${draft ? "open" : ""}`}>
          <FolderTree menu={menu} folders={folders} selected={currentFolder} onSelect={setFolderId} onChanged={refresh} />
          <section className="canvas">
            <div className="canvas-head">
              <div className="crumbs">{path(currentFolder).map((f, i) => <span key={f.id}>{i > 0 && " › "}<button className="link" onClick={() => setFolderId(f.id)}>{f.name}</button></span>)}</div>
            </div>
            <div className="canvas-pages">
              <Tabs size="sm" value={String(page)} onChange={(v) => { setPage(Number(v)); setDraft(null); }}
                tabs={Array.from({ length: pages }, (_, i) => [String(i), `Page ${i + 1}`] as [string, string])} />
              <button className="link" onClick={() => { setExtraPages(extraPages + 1); setPage(pages); }}>+ Add page</button>
            </div>
            <div className="design-grid">
              {Array.from({ length: ROWS * COLS }, (_, i) => {
                const slot = { page, gridRow: Math.floor(i / COLS), gridCol: i % COLS };
                const b = buttons.find((x) => x.page === page && x.gridRow === slot.gridRow && x.gridCol === slot.gridCol);
                const selected = draft && draft.page === slot.page && draft.gridRow === slot.gridRow && draft.gridCol === slot.gridCol;
                return b ? (
                  <button key={i} className={`slot filled t-${b.actionType.toLowerCase()} ${selected ? "selected" : ""}`} onClick={() => setDraft({ ...b })}>
                    <span className="slot-label">{b.label}</span>
                    <span className="slot-kind">{kindText(b, config.data!)}</span>
                  </button>
                ) : (
                  <button key={i} className={`slot empty ${selected ? "selected" : ""}`} onClick={() => setDraft({ ...slot, folderId: currentFolder, actionType: "PRODUCT", label: "" })}>
                    <span>+</span>
                  </button>
                );
              })}
            </div>
            <p className="muted small">The till shows 4 rows x 3 columns per page. Click a slot to add or edit a button.</p>
          </section>
          {draft && <ButtonEditor key={`${currentFolder}-${draft.page}-${draft.gridRow}-${draft.gridCol}`} draft={draft} config={config.data!} menu={menu} folders={folders}
            onClose={() => setDraft(null)} onSaved={(b) => { refresh(); setDraft(b ? { ...draft, ...b } : null); }} />}
        </div>
      )}
    </>
  );
}

function kindText(b: MenuButton, c: MenuConfig): string {
  switch (b.actionType) {
    case "PRODUCT": return b.productPriceMinor != null ? money(b.productPriceMinor) : "Product";
    case "OPEN_FOLDER": return `Folder › ${c.folders.find((f) => f.id === b.targetFolderId)?.name ?? ""}`;
    case "TENDER": return `${(b.tenderMethod ?? "").toLowerCase()}${b.amountMinor ? ` ${money(b.amountMinor)}` : ""}`;
    case "FUNCTION": return `Function${b.functionAvailable === false ? " (not yet available)" : ""}`;
    case "SWITCH_MENU": return `Go to ${b.targetMenuCode?.toLowerCase()} menu`;
  }
}

function FolderTree({ menu, folders, selected, onSelect, onChanged }: { menu: MenuDef; folders: MenuFolder[]; selected: string; onSelect(id: string): void; onChanged(): void }) {
  const [adding, setAdding] = useState(false);
  const [renaming, setRenaming] = useState<string>();
  const [name, setName] = useState("");
  const create = useMutation({ mutationFn: () => api.post<MenuFolder>("/admin/api/menu-folders", { parentFolderId: selected, name }), onSuccess: (f) => { setAdding(false); setName(""); onChanged(); onSelect(f.id); } });
  const rename = useMutation({ mutationFn: (id: string) => api.put<MenuFolder>(`/admin/api/menu-folders/${id}`, { name }), onSuccess: () => { setRenaming(undefined); setName(""); onChanged(); } });
  const remove = useMutation({ mutationFn: (id: string) => api.del(`/admin/api/menu-folders/${id}`), onSuccess: () => { onChanged(); onSelect(menu.rootFolderId); } });
  const children = (parent: string | null) => folders.filter((f) => f.parentFolderId === parent);
  const sel = folders.find((f) => f.id === selected);

  const node = (f: MenuFolder, depth: number): React.ReactNode => (
    <div key={f.id}>
      {renaming === f.id ? (
        <div className="tree-edit" style={{ paddingLeft: 10 + depth * 14 }}>
          <input autoFocus value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === "Enter" && rename.mutate(f.id)} />
          <button className="primary sm" onClick={() => rename.mutate(f.id)}>Save</button>
        </div>
      ) : (
        <button className={`tree-node ${selected === f.id ? "active" : ""}`} style={{ paddingLeft: 10 + depth * 14 }} onClick={() => onSelect(f.id)}>
          <span className="folder-glyph">{f.parentFolderId ? "▸" : "▤"}</span>{f.parentFolderId ? f.name : `${menu.name} (root)`}
        </button>
      )}
      {children(f.id).map((c) => node(c, depth + 1))}
    </div>
  );

  return (
    <aside className="tree">
      <div className="tree-title">Folders</div>
      {children(null).map((f) => node(f, 0))}
      <div className="tree-actions">
        {adding ? (
          <div className="tree-edit">
            <input autoFocus placeholder={`New folder in ${sel?.parentFolderId ? sel.name : "root"}`} value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === "Enter" && name.trim() && create.mutate()} />
            <button className="primary sm" disabled={!name.trim()} onClick={() => create.mutate()}>Add</button>
            <button className="sm" onClick={() => setAdding(false)}>✕</button>
          </div>
        ) : <button className="link" onClick={() => { setAdding(true); setName(""); }}>+ New folder here</button>}
        {sel?.parentFolderId && !adding && (
          <div className="tree-row">
            <button className="link" onClick={() => { setRenaming(sel.id); setName(sel.name); }}>Rename</button>
            <ConfirmButton label="Remove folder" onConfirm={() => remove.mutate(sel.id)} />
          </div>
        )}
        <ErrorText error={create.error ?? rename.error ?? remove.error} />
      </div>
    </aside>
  );
}

function ButtonEditor({ draft, config, menu, folders, onClose, onSaved }: {
  draft: Draft; config: MenuConfig; menu: MenuDef; folders: MenuFolder[]; onClose(): void; onSaved(b: MenuButton | null): void;
}) {
  const [tab, setTab] = useState<"action" | "look">("action");
  const [d, setD] = useState<Draft>(draft);
  const [q, setQ] = useState("");
  const products = useQuery({ queryKey: ["products"], queryFn: () => api.get<Product[]>("/admin/api/products") });
  const functions = useQuery({ queryKey: ["pos-functions"], queryFn: () => api.get<PosFunction[]>("/admin/api/pos-functions") });
  const set = (patch: Partial<Draft>) => setD({ ...d, ...patch });
  const body = () => ({
    folderId: d.folderId, page: d.page, gridRow: d.gridRow, gridCol: d.gridCol, label: d.label ?? "", color: d.color || null,
    actionType: d.actionType, productId: d.productId ?? null, targetFolderId: d.targetFolderId ?? null, tenderMethod: d.tenderMethod ?? null,
    amountMinor: d.amountMinor ?? null, functionCode: d.functionCode ?? null,
    targetMenuId: d.actionType === "SWITCH_MENU" ? config.menus.find((m) => m.code === d.targetMenuCode)?.id ?? null : null,
  });
  const save = useMutation({ mutationFn: () => (d.id ? api.put<MenuButton>(`/admin/api/menu-buttons/${d.id}`, body()) : api.post<MenuButton>("/admin/api/menu-buttons", body())), onSuccess: (b) => onSaved(b) });
  const remove = useMutation({ mutationFn: () => api.del(`/admin/api/menu-buttons/${d.id}`), onSuccess: () => onSaved(null) });
  const productList = (products.data ?? []).filter((p) => p.active && p.name.toLowerCase().includes(q.toLowerCase())).slice(0, 40);
  const targetFolders = folders.filter((f) => f.parentFolderId && f.id !== d.folderId);

  return (
    <Inspector title={d.id ? d.label || "Button" : "New button"} subtitle={`${menu.name} · page ${d.page + 1} · row ${d.gridRow + 1}, column ${d.gridCol + 1}`} onClose={onClose}
      tabs={<Tabs size="sm" value={tab} onChange={setTab} tabs={[["action", "Action"], ["look", "Label & look"]]} />}
      footer={<>{d.id && <ConfirmButton label="Remove" onConfirm={() => remove.mutate()} />}<ErrorText error={save.error ?? remove.error} /><span className="spacer" />
        {save.isSuccess && <span className="saved">Saved</span>}<button onClick={onClose}>Close</button>
        <button className="primary" disabled={save.isPending || !d.label} onClick={() => save.mutate()}>Save</button></>}>
      {tab === "action" && (
        <>
          <Field label="What does it do?">
            <Segmented value={d.actionType ?? "PRODUCT"} onChange={(v) => set({ actionType: v })} options={ACTIONS} />
          </Field>
          {d.actionType === "PRODUCT" && (
            <Field label="Product">
              <input placeholder="Search products" value={q} onChange={(e) => setQ(e.target.value)} />
              <div className="pick-list">
                {productList.map((p) => (
                  <button key={p.id} type="button" className={`pick ${d.productId === p.id ? "on" : ""}`} onClick={() => set({ productId: p.id, label: d.label || p.name })}>
                    <span>{p.name}</span><span className="muted">{money(p.priceMinor)}</span>
                  </button>
                ))}
              </div>
            </Field>
          )}
          {d.actionType === "OPEN_FOLDER" && (
            <Field label="Folder to open" hint="Folders of this menu. Create new ones in the folder tree.">
              <select value={d.targetFolderId ?? ""} onChange={(e) => { const f = folders.find((x) => x.id === e.target.value); set({ targetFolderId: e.target.value, label: d.label || f?.name }); }}>
                <option value="" disabled>Choose a folder...</option>{targetFolders.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
              </select>
            </Field>
          )}
          {d.actionType === "TENDER" && (
            <>
              <Field label="Method"><Segmented value={d.tenderMethod ?? "CASH"} onChange={(v) => set({ tenderMethod: v })} options={[["CASH", "Cash"], ["CARD", "Card"], ["OTHER", "Other"]]} /></Field>
              <Field label="Preset amount" hint="Empty = typed amount, or the exact amount due.">
                <input inputMode="decimal" value={d.amountMinor ? (d.amountMinor / 100).toString() : ""} onChange={(e) => set({ amountMinor: e.target.value ? Math.round(Number(e.target.value) * 100) : null })} />
              </Field>
            </>
          )}
          {d.actionType === "FUNCTION" && (
            <Field label="Function">
              <div className="pick-list">
                {(functions.data ?? []).map((f) => (
                  <button key={f.code} type="button" className={`pick ${d.functionCode === f.code ? "on" : ""}`} onClick={() => set({ functionCode: f.code, label: d.label || f.name })}>
                    <span>{f.name}{!f.available && <span className="muted small"> · not yet available on the till</span>}</span>
                    <span className="muted small">{f.requiredPermission?.toLowerCase().replace("_", " ") ?? "everyone"}</span>
                  </button>
                ))}
              </div>
            </Field>
          )}
          {d.actionType === "SWITCH_MENU" && (
            <Field label="Switch to menu">
              <Segmented value={(d.targetMenuCode as MenuDef["code"]) ?? "SALE"} onChange={(v) => set({ targetMenuCode: v, label: d.label || `Back to ${v.toLowerCase()}` })}
                options={config.menus.filter((m) => m.code !== menu.code).map((m) => [m.code, m.name] as [MenuDef["code"], string])} />
            </Field>
          )}
        </>
      )}
      {tab === "look" && (
        <>
          <Field label="Label" hint="Shown on the till button (max 40 characters)."><input value={d.label ?? ""} maxLength={40} onChange={(e) => set({ label: e.target.value })} /></Field>
          <Field label="Colour" hint="Used by themes that colour buttons; Default follows the button type.">
            <select value={d.color ?? ""} onChange={(e) => set({ color: e.target.value })}>{COLORS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
          </Field>
          <div className="preview-wrap"><div className="muted small">Preview</div><div className={`slot filled t-${(d.actionType ?? "PRODUCT").toLowerCase()} preview`}><span className="slot-label">{d.label || "Label"}</span></div></div>
        </>
      )}
    </Inspector>
  );
}
