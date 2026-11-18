import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ENTITY_LABEL, when, type Entity, type Package, type Terminal } from "../api";
import { ErrorText, PageHead, Status } from "../ui";

const ENTITIES: Entity[] = ["TAX_RATE", "CATEGORY", "PRODUCT"];
/** Local calendar date (not UTC: just after midnight UTC and local dates differ). */
const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

/** Build a package (entities + date range or full snapshot + tills), broadcast it, follow its status per till. */
export function Packages() {
  const qc = useQueryClient();
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals") });
  const packages = useQuery({ queryKey: ["packages"], queryFn: () => api.get<Package[]>("/admin/api/packages"), refetchInterval: 5000 });
  const [entities, setEntities] = useState<Entity[]>(ENTITIES);
  const [scope, setScope] = useState<"DATE_RANGE" | "FULL">("DATE_RANGE");
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [tills, setTills] = useState<string[]>([]);
  const [note, setNote] = useState("");
  const [result, setResult] = useState<Package>();

  useEffect(() => {
    if (terminals.data && tills.length === 0) setTills(terminals.data.filter((t) => t.active).map((t) => t.id)); // all ticked by default
  }, [terminals.data]); // eslint-disable-line react-hooks/exhaustive-deps

  const create = useMutation({
    mutationFn: () => api.post<Package>("/admin/api/packages", { entities, scope, dateFrom: scope === "FULL" ? null : from, dateTo: scope === "FULL" ? null : to, terminalIds: tills, note }),
    onSuccess: (p) => { setResult(p); setNote(""); qc.invalidateQueries({ queryKey: ["packages"] }); },
  });
  const toggle = <T,>(list: T[], v: T) => (list.includes(v) ? list.filter((x) => x !== v) : [...list, v]);

  return (
    <>
      <PageHead title="Packages" sub="Bundle configuration changes into a zip and send it to tills. Tills fetch, apply and report back." />
      <section className="panel">
        <h3>New package</h3>
        <div className="pkg-form">
          <div>
            <div className="label">What to send</div>
            {ENTITIES.map((e) => (
              <label key={e} className="check"><input type="checkbox" checked={entities.includes(e)} onChange={() => setEntities(toggle(entities, e))} /> {ENTITY_LABEL[e]}</label>
            ))}
            <small className="muted">Categories and tax rates used by sent products are added automatically.</small>
          </div>
          <div>
            <div className="label">Which changes</div>
            <label className="check"><input type="radio" checked={scope === "DATE_RANGE"} onChange={() => setScope("DATE_RANGE")} /> Changed between dates</label>
            <div className={`dates ${scope === "FULL" ? "disabled" : ""}`}>
              <input type="date" value={from} disabled={scope === "FULL"} onChange={(e) => setFrom(e.target.value)} />
              <span>to</span>
              <input type="date" value={to} disabled={scope === "FULL"} onChange={(e) => setTo(e.target.value)} />
            </div>
            <label className="check"><input type="radio" checked={scope === "FULL"} onChange={() => setScope("FULL")} /> Full snapshot (everything)</label>
          </div>
          <div>
            <div className="label">Send to tills</div>
            {(terminals.data ?? []).filter((t) => t.active).map((t) => (
              <label key={t.id} className="check"><input type="checkbox" checked={tills.includes(t.id)} onChange={() => setTills(toggle(tills, t.id))} /> {t.name}</label>
            ))}
            {terminals.data?.length === 0 && <small className="muted">No tills yet: add one under Terminals.</small>}
          </div>
        </div>
        <div className="pkg-actions">
          <input className="note" placeholder="Note (optional), e.g. new lunch prices" value={note} onChange={(e) => setNote(e.target.value)} />
          <button className="primary" disabled={create.isPending || entities.length === 0 || tills.length === 0} onClick={() => create.mutate()}>Create &amp; broadcast</button>
        </div>
        <ErrorText error={create.error} />
        {result && (
          <div className="result">
            <strong>Package #{result.packageNo} sent</strong> · {Object.entries(result.counts).filter(([, n]) => n > 0).map(([k, n]) => `${n} ${ENTITY_LABEL[k as Entity].toLowerCase()}`).join(", ")}
            {result.warnings.map((w) => <div key={w} className="warn">⚠ {w}</div>)}
          </div>
        )}
      </section>

      <table className="grid">
        <thead><tr><th>#</th><th>Created</th><th>Scope</th><th>Contents</th><th>Tills</th><th></th></tr></thead>
        <tbody>
          {(packages.data ?? []).map((p) => (
            <tr key={p.packageNo}>
              <td className="strong">#{p.packageNo}</td>
              <td>{when(p.createdAt)}{p.note && <div className="muted small">{p.note}</div>}</td>
              <td>{p.scope === "FULL" ? "Full snapshot" : `${p.dateFrom} → ${p.dateTo}`}</td>
              <td className="muted">{Object.entries(p.counts).filter(([, n]) => n > 0).map(([k, n]) => `${n} ${ENTITY_LABEL[k as Entity].toLowerCase()}`).join(", ")}</td>
              <td>{p.targets.map((t) => (
                <div key={t.terminalId} className="target" title={t.error ?? ""}>{t.terminalName} <Status value={t.status} />{t.error && <span className="error small"> {t.error}</span>}</div>
              ))}</td>
              <td><button className="link" onClick={() => api.download(`/admin/api/packages/${p.packageNo}/zip`, `package-${p.packageNo}.zip`)}>Download zip</button></td>
            </tr>
          ))}
          {packages.data?.length === 0 && <tr><td colSpan={6} className="empty">No packages yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
