import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, money, type Category, type Product, type TaxRate } from "../api";
import { ErrorText, Facts, Field, FormPage, Inspector, Notice, PageHead, RowActions, Section, Segmented, Split, Toggle } from "../ui";
import { PlusIcon } from "../icons";

const useCatalog = () => ({
  taxRates: useQuery({ queryKey: ["tax-rates"], queryFn: () => api.get<TaxRate[]>("/admin/api/tax-rates") }),
  categories: useQuery({ queryKey: ["categories"], queryFn: () => api.get<Category[]>("/admin/api/categories") }),
  products: useQuery({ queryKey: ["products"], queryFn: () => api.get<Product[]>("/admin/api/products") }),
});

/** list (+ quick view panel) or a full-page create / edit form. */
type Screen<T> = { kind: "list"; viewing?: T } | { kind: "create" } | { kind: "edit"; record: T };

function useRecordApi<T>(path: string, keys: string[]) {
  const qc = useQueryClient();
  const refresh = () => [...keys, "overview", "menus"].forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
  return {
    save: useMutation({ mutationFn: ({ id, body }: { id?: string; body: unknown }) => (id ? api.put<T>(`${path}/${id}`, body) : api.post<T>(path, body)), onSuccess: refresh }),
    remove: useMutation({ mutationFn: (id: string) => api.del(`${path}/${id}`), onSuccess: refresh }),
  };
}

const NON_MERCH = ["SERVICE_CHARGE", "DELIVERY", "TIP", "DEPOSIT", "GIFT_CARD", "OTHER"];
const pretty = (s: string) => s.charAt(0) + s.slice(1).toLowerCase().replace("_", " ");
const yes = (b: boolean | undefined) => (b ? "Yes" : "No");
const shortId = (id?: string) => (id ? id.slice(-8) : "");
const changedNote = "Saved changes reach the tills when you broadcast them (Dashboard or Packages).";
const deleteNote = "Delete removes it here and from the tills on the next broadcast; past sales keep their copy.";

// =============== products ===============
export function Products() {
  const { products, categories, taxRates } = useCatalog();
  const { save, remove } = useRecordApi<Product>("/admin/api/products", ["products"]);
  const [screen, setScreen] = useState<Screen<Product>>({ kind: "list" });
  const [q, setQ] = useState("");
  const catName = (id?: string) => categories.data?.find((c) => c.id === id)?.name ?? "";
  const tax = (id?: string | null) => taxRates.data?.find((t) => t.id === id);
  const go = (s: Screen<Product>) => { save.reset(); setScreen(s); };

  if (screen.kind !== "list") {
    return <ProductForm key={screen.kind === "edit" ? screen.record.id : "new"} record={screen.kind === "edit" ? screen.record : undefined}
      categories={categories.data ?? []} taxRates={taxRates.data ?? []} save={save} catName={catName} taxName={(id) => tax(id)?.name ?? "No tax"}
      onCancel={() => go({ kind: "list", viewing: screen.kind === "edit" ? screen.record : undefined })}
      onSaved={(p) => go({ kind: "list", viewing: p })} />;
  }
  const rows = (products.data ?? []).filter((p) => `${p.name} ${p.sku ?? ""} ${p.barcode ?? ""}`.toLowerCase().includes(q.toLowerCase()));
  const v = screen.viewing;
  return (
    <>
      <PageHead title="Products" sub={changedNote} actions={<>
        <input className="search" placeholder="Filter by name, SKU, barcode" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="primary with-icon" onClick={() => go({ kind: "create" })}><PlusIcon />New product</button>
      </>} />
      <ErrorText error={products.error ?? remove.error} />
      <Split open={!!v}>
        <table className="grid">
          <thead><tr><th>Name</th><th>Category</th><th>Type</th><th>SKU / barcode</th><th>Tax</th><th className="num">Price</th><th>Kitchen</th><th className="actions-col">Actions</th></tr></thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} onClick={() => go({ kind: "list", viewing: p })} className={`click ${v?.id === p.id ? "selected" : ""}`}>
                <td className="strong">{p.name}</td><td>{catName(p.categoryId)}</td>
                <td className="muted">{p.itemClass === "MERCHANDISE" ? `Merchandise${p.trackStock ? " · stock" : ""}` : pretty(p.nonMerchType ?? "OTHER")}</td>
                <td className="muted small">{p.sku ?? "-"}{p.barcode && <div>{p.barcode}</div>}</td>
                <td className="muted">{tax(p.taxRateId)?.name ?? "No tax"}</td>
                <td className="num">{money(p.priceMinor)}</td><td className="muted">{p.kitchenPrint ? "Yes" : "-"}</td>
                <td className="actions-col"><RowActions onView={() => go({ kind: "list", viewing: p })} onEdit={() => go({ kind: "edit", record: p })}
                  onDelete={() => remove.mutate(p.id, { onSuccess: () => v?.id === p.id && go({ kind: "list" }) })} /></td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={8} className="empty">No products.</td></tr>}
          </tbody>
        </table>
        {v && (
          <Inspector title={v.name} subtitle="Product" onClose={() => go({ kind: "list" })}
            footer={<><span className="spacer" /><button className="primary" onClick={() => go({ kind: "edit", record: v })}>Edit</button></>}>
            <Facts items={[["ID", <code>{shortId(v.id)}</code>], ["Category", catName(v.categoryId)],
              ["Type", v.itemClass === "MERCHANDISE" ? "Merchandise" : `Non-merchandise · ${pretty(v.nonMerchType ?? "OTHER")}`],
              ["Price", money(v.priceMinor)], ["Tax rate", tax(v.taxRateId)?.name ?? "No tax"], ["Print in kitchen", yes(v.kitchenPrint)],
              ["SKU", v.sku], ["Barcode", v.barcode], ["Track stock", yes(v.trackStock)], ["Sort order", v.sortOrder]]} />
            <Notice tone="info">{deleteNote}</Notice>
          </Inspector>
        )}
      </Split>
    </>
  );
}

function ProductForm({ record, categories, taxRates, save, catName, taxName, onCancel, onSaved }: {
  record?: Product; categories: Category[]; taxRates: TaxRate[]; save: ReturnType<typeof useRecordApi<Product>>["save"];
  catName(id?: string): string; taxName(id?: string | null): string; onCancel(): void; onSaved(p: Product): void;
}) {
  const [step, setStep] = useState<"details" | "review">("details");
  const [p, setP] = useState<Partial<Product>>(record ?? { itemClass: "MERCHANDISE", active: true, priceMinor: 0, sortOrder: 0, kitchenPrint: false, trackStock: false });
  const [price, setPrice] = useState(((record?.priceMinor ?? 0) / 100).toFixed(2));
  const set = (k: keyof Product, v: unknown) => setP({ ...p, [k]: v });
  const nonMerch = p.itemClass === "NON_MERCHANDISE";
  const priceMinor = Math.round(Number(price) * 100);
  const valid = !!p.name?.trim() && !!p.categoryId && Number.isFinite(priceMinor) && priceMinor >= 0;
  const submit = () => save.mutate({ id: record?.id, body: {
    categoryId: p.categoryId, taxRateId: p.taxRateId || null, name: p.name ?? "", itemClass: p.itemClass,
    nonMerchType: nonMerch ? p.nonMerchType : null, sku: p.sku || null, barcode: p.barcode || null, priceMinor,
    kitchenPrint: !!p.kitchenPrint, trackStock: !nonMerch && !!p.trackStock, sortOrder: Number(p.sortOrder ?? 0), active: true } },
    { onSuccess: (saved) => onSaved(saved as Product) });
  return (
    <FormPage backLabel="Products" onCancel={onCancel} mode={record ? "edit" : "create"} step={step} onStep={setStep} canContinue={valid}
      title={record ? `Edit ${record.name}` : "New product"} subtitle={record ? `ID ${shortId(record.id)}` : "Add the product details, continue to review, then save."}
      onSave={submit} saving={save.isPending} error={save.error}
      details={<>
        <Section title="General" hint="What the cashier sees on buttons and receipts.">
          <Field label="Name *"><input autoFocus value={p.name ?? ""} onChange={(e) => set("name", e.target.value)} /></Field>
          <Field label="Category *">
            <select value={p.categoryId ?? ""} onChange={(e) => set("categoryId", e.target.value)}>
              <option value="" disabled>Choose a category...</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Type"><Segmented value={p.itemClass ?? "MERCHANDISE"} onChange={(v) => setP({ ...p, itemClass: v, nonMerchType: v === "NON_MERCHANDISE" ? (p.nonMerchType ?? "OTHER") : null })}
            options={[["MERCHANDISE", "Merchandise"], ["NON_MERCHANDISE", "Non-merchandise"]]} /></Field>
          {nonMerch && <Field label="Kind of charge"><select value={p.nonMerchType ?? "OTHER"} onChange={(e) => set("nonMerchType", e.target.value)}>{NON_MERCH.map((t) => <option key={t} value={t}>{pretty(t)}</option>)}</select></Field>}
        </Section>
        <Section title="Price & tax">
          <Field label="Price *"><input inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} /></Field>
          <Field label="Tax rate"><select value={p.taxRateId ?? ""} onChange={(e) => set("taxRateId", e.target.value || null)}><option value="">No tax</option>{taxRates.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}</select></Field>
          <Toggle label="Print in kitchen" hint="Sends this item to the kitchen ticket." checked={!!p.kitchenPrint} onChange={(v) => set("kitchenPrint", v)} />
        </Section>
        <Section title="Codes & stock">
          <Field label="SKU"><input value={p.sku ?? ""} onChange={(e) => set("sku", e.target.value)} /></Field>
          <Field label="Barcode"><input value={p.barcode ?? ""} onChange={(e) => set("barcode", e.target.value)} /></Field>
          <Field label="Sort order"><input type="number" value={p.sortOrder ?? 0} onChange={(e) => set("sortOrder", e.target.value)} /></Field>
          <Toggle label="Track stock" disabled={nonMerch} hint={nonMerch ? "Only merchandise can track stock." : "Deducts recipe stock when sold."} checked={!nonMerch && !!p.trackStock} onChange={(v) => set("trackStock", v)} />
        </Section>
      </>}
      review={<Section title="Review" hint="Check the details, then Save. Use Back to change something.">
        <Facts items={[["Name", p.name], ["Category", catName(p.categoryId)], ["Type", nonMerch ? `Non-merchandise · ${pretty(p.nonMerchType ?? "OTHER")}` : "Merchandise"],
          ["Price", money(priceMinor)], ["Tax rate", taxName(p.taxRateId)], ["Print in kitchen", yes(p.kitchenPrint)], ["SKU", p.sku || "-"], ["Barcode", p.barcode || "-"],
          ["Track stock", yes(!nonMerch && p.trackStock)], ["Sort order", p.sortOrder ?? 0]]} />
      </Section>} />
  );
}

// =============== categories ===============
export function Categories() {
  const { categories, products } = useCatalog();
  const { save, remove } = useRecordApi<Category>("/admin/api/categories", ["categories"]);
  const [screen, setScreen] = useState<Screen<Category>>({ kind: "list" });
  const go = (s: Screen<Category>) => { save.reset(); setScreen(s); };
  const count = (id: string) => (products.data ?? []).filter((p) => p.categoryId === id).length;

  if (screen.kind !== "list") {
    const record = screen.kind === "edit" ? screen.record : undefined;
    return <SimpleForm key={record?.id ?? "new"} backLabel="Categories" title={record ? `Edit ${record.name}` : "New category"} record={record} save={save}
      initial={record ?? { name: "", sortOrder: 0 }} onCancel={() => go({ kind: "list", viewing: record })} onSaved={(c) => go({ kind: "list", viewing: c as Category })}
      body={(c) => ({ name: c.name ?? "", sortOrder: Number(c.sortOrder ?? 0), active: true })} valid={(c) => !!c.name?.trim()}
      fields={(c, set) => <Section title="Category">
        <Field label="Name *"><input autoFocus value={c.name ?? ""} onChange={(e) => set({ name: e.target.value })} /></Field>
        <Field label="Sort order" hint="Lower numbers come first."><input type="number" value={c.sortOrder ?? 0} onChange={(e) => set({ sortOrder: Number(e.target.value) })} /></Field>
      </Section>}
      facts={(c) => [["Name", c.name], ["Sort order", c.sortOrder ?? 0]]} />;
  }
  const v = screen.viewing;
  return (
    <>
      <PageHead title="Categories" sub={changedNote} actions={<button className="primary with-icon" onClick={() => go({ kind: "create" })}><PlusIcon />New category</button>} />
      <ErrorText error={remove.error} />
      <Split open={!!v}>
        <table className="grid">
          <thead><tr><th>Name</th><th className="num">Products</th><th className="num">Sort</th><th>ID</th><th className="actions-col">Actions</th></tr></thead>
          <tbody>{(categories.data ?? []).map((x) => (
            <tr key={x.id} className={`click ${v?.id === x.id ? "selected" : ""}`} onClick={() => go({ kind: "list", viewing: x })}>
              <td className="strong">{x.name}</td><td className="num">{count(x.id)}</td><td className="num">{x.sortOrder}</td><td><code>{shortId(x.id)}</code></td>
              <td className="actions-col"><RowActions onView={() => go({ kind: "list", viewing: x })} onEdit={() => go({ kind: "edit", record: x })} onDelete={() => remove.mutate(x.id, { onSuccess: () => go({ kind: "list" }) })} /></td>
            </tr>
          ))}</tbody>
        </table>
        {v && (
          <Inspector title={v.name} subtitle="Category" onClose={() => go({ kind: "list" })}
            footer={<><span className="spacer" /><button className="primary" onClick={() => go({ kind: "edit", record: v })}>Edit</button></>}>
            <Facts items={[["ID", <code>{shortId(v.id)}</code>], ["Name", v.name], ["Products", count(v.id)], ["Sort order", v.sortOrder]]} />
            <Notice tone="info">{deleteNote}</Notice>
          </Inspector>
        )}
      </Split>
    </>
  );
}

// =============== tax rates ===============
export function TaxRates() {
  const { taxRates, products } = useCatalog();
  const { save, remove } = useRecordApi<TaxRate>("/admin/api/tax-rates", ["tax-rates"]);
  const [screen, setScreen] = useState<Screen<TaxRate>>({ kind: "list" });
  const go = (s: Screen<TaxRate>) => { save.reset(); setScreen(s); };
  const count = (id: string) => (products.data ?? []).filter((p) => p.taxRateId === id).length;

  if (screen.kind !== "list") {
    const record = screen.kind === "edit" ? screen.record : undefined;
    return <SimpleForm key={record?.id ?? "new"} backLabel="Tax rates" title={record ? `Edit ${record.name}` : "New tax rate"} record={record} save={save}
      initial={record ?? { name: "", rateBp: 0, inclusive: true }} onCancel={() => go({ kind: "list", viewing: record })} onSaved={(t) => go({ kind: "list", viewing: t as TaxRate })}
      body={(t) => ({ name: t.name ?? "", rateBp: Math.round(Number(t.rateBp ?? 0)), inclusive: !!t.inclusive, active: true })} valid={(t) => !!t.name?.trim() && (t.rateBp ?? 0) >= 0 && (t.rateBp ?? 0) <= 10000}
      fields={(t, set) => <Section title="Tax rate">
        <Field label="Name *"><input autoFocus value={t.name ?? ""} onChange={(e) => set({ name: e.target.value })} /></Field>
        <Field label="Rate % *"><input inputMode="decimal" value={(t.rateBp ?? 0) / 100} onChange={(e) => set({ rateBp: Number(e.target.value) * 100 })} /></Field>
        <Field label="Prices"><Segmented value={t.inclusive ? "inc" : "exc"} onChange={(x) => set({ inclusive: x === "inc" })} options={[["inc", "Include tax"], ["exc", "Tax added on top"]]} /></Field>
      </Section>}
      facts={(t) => [["Name", t.name], ["Rate", `${((t.rateBp ?? 0) / 100).toFixed(2)}%`], ["Prices", t.inclusive ? "Include tax" : "Tax added on top"]]} />;
  }
  const v = screen.viewing;
  return (
    <>
      <PageHead title="Tax rates" sub={changedNote} actions={<button className="primary with-icon" onClick={() => go({ kind: "create" })}><PlusIcon />New tax rate</button>} />
      <ErrorText error={remove.error} />
      <Split open={!!v}>
        <table className="grid">
          <thead><tr><th>Name</th><th className="num">Rate</th><th>Prices</th><th className="num">Products</th><th>ID</th><th className="actions-col">Actions</th></tr></thead>
          <tbody>{(taxRates.data ?? []).map((x) => (
            <tr key={x.id} className={`click ${v?.id === x.id ? "selected" : ""}`} onClick={() => go({ kind: "list", viewing: x })}>
              <td className="strong">{x.name}</td><td className="num">{(x.rateBp / 100).toFixed(2)}%</td><td className="muted">{x.inclusive ? "Include tax" : "Tax added"}</td>
              <td className="num">{count(x.id)}</td><td><code>{shortId(x.id)}</code></td>
              <td className="actions-col"><RowActions onView={() => go({ kind: "list", viewing: x })} onEdit={() => go({ kind: "edit", record: x })} onDelete={() => remove.mutate(x.id, { onSuccess: () => go({ kind: "list" }) })} /></td>
            </tr>
          ))}</tbody>
        </table>
        {v && (
          <Inspector title={v.name} subtitle="Tax rate" onClose={() => go({ kind: "list" })}
            footer={<><span className="spacer" /><button className="primary" onClick={() => go({ kind: "edit", record: v })}>Edit</button></>}>
            <Facts items={[["ID", <code>{shortId(v.id)}</code>], ["Name", v.name], ["Rate", `${(v.rateBp / 100).toFixed(2)}%`], ["Prices", v.inclusive ? "Include tax" : "Tax added on top"], ["Products", count(v.id)]]} />
            <Notice tone="info">{deleteNote}</Notice>
          </Inspector>
        )}
      </Split>
    </>
  );
}

/** Full-page form for small records (one section + review). */
function SimpleForm<T extends { id: string }>({ backLabel, title, record, save, initial, onCancel, onSaved, body, valid, fields, facts }: {
  backLabel: string; title: string; record?: T; save: ReturnType<typeof useRecordApi<T>>["save"]; initial: Partial<T>;
  onCancel(): void; onSaved(saved: T): void; body(v: Partial<T>): unknown; valid(v: Partial<T>): boolean;
  fields(v: Partial<T>, set: (patch: Partial<T>) => void): React.ReactNode; facts(v: Partial<T>): [string, React.ReactNode][];
}) {
  const [step, setStep] = useState<"details" | "review">("details");
  const [v, setV] = useState<Partial<T>>(initial);
  return (
    <FormPage backLabel={backLabel} onCancel={onCancel} mode={record ? "edit" : "create"} step={step} onStep={setStep} canContinue={valid(v)}
      title={title} subtitle={record ? `ID ${shortId(record.id)}` : "Fill in the details, continue to review, then save."}
      onSave={() => save.mutate({ id: record?.id, body: body(v) }, { onSuccess: (s) => onSaved(s as T) })} saving={save.isPending} error={save.error}
      details={fields(v, (patch) => setV({ ...v, ...patch }))}
      review={<Section title="Review" hint="Check the details, then Save."><Facts items={facts(v)} /></Section>} />
  );
}
