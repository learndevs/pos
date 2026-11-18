import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, money, type Category, type Product, type TaxRate } from "../api";
import { ErrorText, Field, Modal, PageHead } from "../ui";

const useCatalog = () => ({
  taxRates: useQuery({ queryKey: ["tax-rates"], queryFn: () => api.get<TaxRate[]>("/admin/api/tax-rates") }),
  categories: useQuery({ queryKey: ["categories"], queryFn: () => api.get<Category[]>("/admin/api/categories") }),
  products: useQuery({ queryKey: ["products"], queryFn: () => api.get<Product[]>("/admin/api/products") }),
});

function useSave<T>(path: string, keys: string[], onDone: () => void) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: unknown }) => (id ? api.put<T>(`${path}/${id}`, body) : api.post<T>(path, body)),
    onSuccess: () => { keys.forEach((k) => qc.invalidateQueries({ queryKey: [k] })); onDone(); },
  });
}

const NON_MERCH = ["SERVICE_CHARGE", "DELIVERY", "TIP", "DEPOSIT", "GIFT_CARD", "OTHER"];
const Active = ({ on }: { on: boolean }) => <span className={on ? "pill ok" : "pill"}>{on ? "Active" : "Inactive"}</span>;
const changedNote = "Changes are sent to tills in a package (Packages).";

// ---------------- products ----------------
export function Products() {
  const { products, categories, taxRates } = useCatalog();
  const [edit, setEdit] = useState<Partial<Product> | null>(null);
  const [q, setQ] = useState("");
  const catName = (id: string) => categories.data?.find((c) => c.id === id)?.name ?? "";
  const rows = (products.data ?? []).filter((p) => (p.name + (p.sku ?? "")).toLowerCase().includes(q.toLowerCase()));
  return (
    <>
      <PageHead title="Products" sub={changedNote} actions={<>
        <input className="search" placeholder="Search products" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="primary" onClick={() => setEdit({ itemClass: "MERCHANDISE", active: true, priceMinor: 0, sortOrder: 0, kitchenPrint: false, trackStock: false })}>New product</button>
      </>} />
      <ErrorText error={products.error} />
      <table className="grid">
        <thead><tr><th>Name</th><th>Category</th><th>Type</th><th className="num">Price</th><th>SKU</th><th>Status</th></tr></thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id} onClick={() => setEdit(p)} className="click">
              <td className="strong">{p.name}</td><td>{catName(p.categoryId)}</td>
              <td className="muted">{p.itemClass === "MERCHANDISE" ? "Merchandise" : `Non-merch · ${p.nonMerchType?.toLowerCase().replace("_", " ")}`}</td>
              <td className="num">{money(p.priceMinor)}</td><td className="muted">{p.sku}</td><td><Active on={p.active} /></td>
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={6} className="empty">No products.</td></tr>}
        </tbody>
      </table>
      {edit && <ProductForm initial={edit} categories={categories.data ?? []} taxRates={taxRates.data ?? []} onClose={() => setEdit(null)} />}
    </>
  );
}

function ProductForm({ initial, categories, taxRates, onClose }: { initial: Partial<Product>; categories: Category[]; taxRates: TaxRate[]; onClose(): void }) {
  const [p, setP] = useState(initial);
  const [price, setPrice] = useState(((initial.priceMinor ?? 0) / 100).toFixed(2));
  const save = useSave<Product>("/admin/api/products", ["products"], onClose);
  const set = (k: keyof Product, v: unknown) => setP({ ...p, [k]: v });
  const nonMerch = p.itemClass === "NON_MERCHANDISE";
  const submit = () => save.mutate({ id: initial.id, body: {
    categoryId: p.categoryId, taxRateId: p.taxRateId || null, name: p.name ?? "", itemClass: p.itemClass,
    nonMerchType: nonMerch ? p.nonMerchType : null, sku: p.sku || null, barcode: p.barcode || null,
    priceMinor: Math.round(Number(price) * 100), kitchenPrint: !!p.kitchenPrint, trackStock: !nonMerch && !!p.trackStock,
    sortOrder: Number(p.sortOrder ?? 0), active: !!p.active } });
  return (
    <Modal title={initial.id ? `Edit ${initial.name}` : "New product"} onClose={onClose} footer={<>
      <ErrorText error={save.error} /><span className="spacer" />
      <button onClick={onClose}>Cancel</button><button className="primary" disabled={save.isPending} onClick={submit}>Save</button></>}>
      <div className="form-grid">
        <Field label="Name"><input autoFocus value={p.name ?? ""} onChange={(e) => set("name", e.target.value)} /></Field>
        <Field label="Price"><input inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} /></Field>
        <Field label="Category">
          <select value={p.categoryId ?? ""} onChange={(e) => set("categoryId", e.target.value)}>
            <option value="" disabled>Choose...</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </Field>
        <Field label="Tax rate">
          <select value={p.taxRateId ?? ""} onChange={(e) => set("taxRateId", e.target.value || null)}>
            <option value="">No tax</option>{taxRates.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </Field>
        <Field label="Type">
          <select value={p.itemClass} onChange={(e) => setP({ ...p, itemClass: e.target.value as Product["itemClass"], nonMerchType: e.target.value === "NON_MERCHANDISE" ? (p.nonMerchType ?? "OTHER") : null })}>
            <option value="MERCHANDISE">Merchandise</option><option value="NON_MERCHANDISE">Non-merchandise</option>
          </select>
        </Field>
        {nonMerch ? (
          <Field label="Non-merchandise type">
            <select value={p.nonMerchType ?? "OTHER"} onChange={(e) => set("nonMerchType", e.target.value)}>
              {NON_MERCH.map((t) => <option key={t} value={t}>{t.toLowerCase().replace("_", " ")}</option>)}
            </select>
          </Field>
        ) : <Field label="Sort order"><input type="number" value={p.sortOrder ?? 0} onChange={(e) => set("sortOrder", e.target.value)} /></Field>}
        <Field label="SKU"><input value={p.sku ?? ""} onChange={(e) => set("sku", e.target.value)} /></Field>
        <Field label="Barcode"><input value={p.barcode ?? ""} onChange={(e) => set("barcode", e.target.value)} /></Field>
      </div>
      <div className="checks">
        <label><input type="checkbox" checked={!!p.kitchenPrint} onChange={(e) => set("kitchenPrint", e.target.checked)} /> Print in kitchen</label>
        <label className={nonMerch ? "disabled" : ""}><input type="checkbox" disabled={nonMerch} checked={!nonMerch && !!p.trackStock} onChange={(e) => set("trackStock", e.target.checked)} /> Track stock</label>
        <label><input type="checkbox" checked={!!p.active} onChange={(e) => set("active", e.target.checked)} /> Active</label>
      </div>
    </Modal>
  );
}

// ---------------- categories ----------------
export function Categories() {
  const { categories } = useCatalog();
  const [edit, setEdit] = useState<Partial<Category> | null>(null);
  const save = useSave<Category>("/admin/api/categories", ["categories"], () => setEdit(null));
  return (
    <>
      <PageHead title="Categories" sub={changedNote} actions={<button className="primary" onClick={() => setEdit({ sortOrder: 0, active: true })}>New category</button>} />
      <table className="grid">
        <thead><tr><th>Name</th><th className="num">Sort</th><th>Status</th></tr></thead>
        <tbody>{(categories.data ?? []).map((c) => (
          <tr key={c.id} className="click" onClick={() => setEdit(c)}><td className="strong">{c.name}</td><td className="num">{c.sortOrder}</td><td><Active on={c.active} /></td></tr>
        ))}</tbody>
      </table>
      {edit && (
        <Modal title={edit.id ? `Edit ${edit.name}` : "New category"} onClose={() => setEdit(null)} footer={<>
          <ErrorText error={save.error} /><span className="spacer" /><button onClick={() => setEdit(null)}>Cancel</button>
          <button className="primary" onClick={() => save.mutate({ id: edit.id, body: { name: edit.name ?? "", sortOrder: Number(edit.sortOrder ?? 0), active: !!edit.active } })}>Save</button></>}>
          <div className="form-grid">
            <Field label="Name"><input autoFocus value={edit.name ?? ""} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></Field>
            <Field label="Sort order"><input type="number" value={edit.sortOrder ?? 0} onChange={(e) => setEdit({ ...edit, sortOrder: Number(e.target.value) })} /></Field>
          </div>
          <div className="checks"><label><input type="checkbox" checked={!!edit.active} onChange={(e) => setEdit({ ...edit, active: e.target.checked })} /> Active</label></div>
        </Modal>
      )}
    </>
  );
}

// ---------------- tax rates ----------------
export function TaxRates() {
  const { taxRates } = useCatalog();
  const [edit, setEdit] = useState<Partial<TaxRate> | null>(null);
  const save = useSave<TaxRate>("/admin/api/tax-rates", ["tax-rates"], () => setEdit(null));
  return (
    <>
      <PageHead title="Tax rates" sub={changedNote} actions={<button className="primary" onClick={() => setEdit({ rateBp: 0, inclusive: true, active: true })}>New tax rate</button>} />
      <table className="grid">
        <thead><tr><th>Name</th><th className="num">Rate</th><th>Prices</th><th>Status</th></tr></thead>
        <tbody>{(taxRates.data ?? []).map((t) => (
          <tr key={t.id} className="click" onClick={() => setEdit(t)}><td className="strong">{t.name}</td><td className="num">{(t.rateBp / 100).toFixed(2)}%</td>
            <td className="muted">{t.inclusive ? "Include tax" : "Tax added"}</td><td><Active on={t.active} /></td></tr>
        ))}</tbody>
      </table>
      {edit && (
        <Modal title={edit.id ? `Edit ${edit.name}` : "New tax rate"} onClose={() => setEdit(null)} footer={<>
          <ErrorText error={save.error} /><span className="spacer" /><button onClick={() => setEdit(null)}>Cancel</button>
          <button className="primary" onClick={() => save.mutate({ id: edit.id, body: { name: edit.name ?? "", rateBp: Math.round(Number(edit.rateBp ?? 0)), inclusive: !!edit.inclusive, active: !!edit.active } })}>Save</button></>}>
          <div className="form-grid">
            <Field label="Name"><input autoFocus value={edit.name ?? ""} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></Field>
            <Field label="Rate %"><input inputMode="decimal" value={(edit.rateBp ?? 0) / 100} onChange={(e) => setEdit({ ...edit, rateBp: Number(e.target.value) * 100 })} /></Field>
          </div>
          <div className="checks">
            <label><input type="checkbox" checked={!!edit.inclusive} onChange={(e) => setEdit({ ...edit, inclusive: e.target.checked })} /> Prices include tax</label>
            <label><input type="checkbox" checked={!!edit.active} onChange={(e) => setEdit({ ...edit, active: e.target.checked })} /> Active</label>
          </div>
        </Modal>
      )}
    </>
  );
}
