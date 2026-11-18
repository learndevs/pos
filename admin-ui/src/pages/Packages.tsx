import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ENTITY_LABEL, when, type Entity, type Package } from "../api";
import { ErrorText, Notice, PageHead, Segmented, Status, Tabs, Toggle } from "../ui";
import { TillPicker } from "./TillPicker";

const GROUPS: [string, string, Entity[]][] = [
  ["tax", "Tax rates", ["TAX_RATE"]], ["cat", "Categories", ["CATEGORY"]], ["prod", "Products", ["PRODUCT"]], ["menu", "Menus", ["MENU_FOLDER", "MENU_BUTTON"]],
];
/** Local calendar date (not UTC). */
const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const counts = (p: Package) => Object.entries(p.counts).filter(([, n]) => n > 0).map(([k, n]) => `${n} ${ENTITY_LABEL[k as Entity].toLowerCase()}`).join(", ");

/** Advanced package builder (entities + date range or snapshot + tills) and the package history. */
export function Packages() {
  const [tab, setTab] = useState<"new" | "history">("new");
  const packages = useQuery({ queryKey: ["packages"], queryFn: () => api.get<Package[]>("/admin/api/packages"), refetchInterval: 5000 });
  return (
    <>
      <PageHead title="Packages" sub="Bundle configuration into a zip for chosen tills. Tills fetch, apply and report back." />
      <Tabs value={tab} onChange={setTab} tabs={[["new", "New package"], ["history", "History", packages.data?.length]]} />
      <div className="tab-panel">{tab === "new" ? <Builder onDone={() => setTab("history")} /> : <History packages={packages.data ?? []} />}</div>
    </>
  );
}

function Builder({ onDone }: { onDone(): void }) {
  const qc = useQueryClient();
  const [groups, setGroups] = useState<string[]>(GROUPS.map((g) => g[0]));
  const [scope, setScope] = useState<"DATE_RANGE" | "FULL">("DATE_RANGE");
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [tills, setTills] = useState<string[]>([]);
  const [note, setNote] = useState("");
  const [result, setResult] = useState<Package>();
  const entities = GROUPS.filter((g) => groups.includes(g[0])).flatMap((g) => g[2]);
  const create = useMutation({
    mutationFn: () => api.post<Package>("/admin/api/packages", { entities, scope, dateFrom: scope === "FULL" ? null : from, dateTo: scope === "FULL" ? null : to, terminalIds: tills, note }),
    onSuccess: (p) => { setResult(p); setNote(""); qc.invalidateQueries({ queryKey: ["packages"] }); qc.invalidateQueries({ queryKey: ["overview"] }); },
  });
  return (
    <div className="builder">
      <section className="panel">
        <h3>1 · What to send</h3>
        <div className="toggle-list">
          {GROUPS.map(([k, label]) => (
            <Toggle key={k} label={label} checked={groups.includes(k)} onChange={(v) => setGroups(v ? [...groups, k] : groups.filter((x) => x !== k))} />
          ))}
        </div>
        <small className="muted">Anything the sent items refer to (a product's category, a button's product or folder) is added automatically.</small>
      </section>
      <section className="panel">
        <h3>2 · Which changes</h3>
        <Segmented value={scope} onChange={setScope} options={[["DATE_RANGE", "Changed between dates"], ["FULL", "Full snapshot"]]} />
        {scope === "DATE_RANGE" ? (
          <div className="dates"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /><span>to</span><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></div>
        ) : <p className="muted small">Everything currently configured for the chosen entities.</p>}
      </section>
      <section className="panel">
        <h3>3 · Send to tills</h3>
        <TillPicker value={tills} onChange={setTills} />
      </section>
      <section className="panel">
        <div className="pkg-actions">
          <input className="note" placeholder="Note (optional), e.g. new lunch prices" value={note} onChange={(e) => setNote(e.target.value)} />
          <button className="primary" disabled={create.isPending || entities.length === 0 || tills.length === 0} onClick={() => create.mutate()}>Create &amp; broadcast</button>
        </div>
        <ErrorText error={create.error} />
        {result && (
          <Notice>
            <strong>Package #{result.packageNo} sent</strong> · {counts(result)} <button className="link" onClick={onDone}>View status</button>
            {result.warnings.map((w) => <div key={w} className="warn">⚠ {w}</div>)}
          </Notice>
        )}
      </section>
    </div>
  );
}

function History({ packages }: { packages: Package[] }) {
  return (
    <table className="grid">
      <thead><tr><th>#</th><th>Created</th><th>Scope</th><th>Contents</th><th>Tills</th><th></th></tr></thead>
      <tbody>
        {packages.map((p) => (
          <tr key={p.packageNo}>
            <td className="strong">#{p.packageNo}</td>
            <td>{when(p.createdAt)}{p.note && <div className="muted small">{p.note}</div>}</td>
            <td>{p.scope === "FULL" ? "Full snapshot" : `${p.dateFrom} → ${p.dateTo}`}</td>
            <td className="muted">{counts(p)}</td>
            <td>{p.targets.map((t) => (
              <div key={t.terminalId} className="target" title={t.error ?? ""}>{t.terminalName} <Status value={t.status} />{t.error && <div className="error small">{t.error}</div>}</div>
            ))}</td>
            <td><button className="link" onClick={() => api.download(`/admin/api/packages/${p.packageNo}/zip`, `package-${p.packageNo}.zip`)}>Download zip</button></td>
          </tr>
        ))}
        {packages.length === 0 && <tr><td colSpan={6} className="empty">No packages yet.</td></tr>}
      </tbody>
    </table>
  );
}
