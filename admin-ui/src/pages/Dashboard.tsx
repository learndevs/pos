import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ENTITY_LABEL, isOnline, when, type Entity, type Group, type GroupStatus, type Package, type Terminal } from "../api";
import { ErrorText, Notice, PageHead, Status } from "../ui";
import { TillPicker } from "./TillPicker";

const ICON: Record<Group, string> = { TAX_RATES: "%", CATEGORIES: "▦", PRODUCTS: "◆", MENUS: "▤" };

/** Each entity can be broadcast on its own: changes since its last package go to the selected tills. */
export function Dashboard({ go }: { go(page: string): void }) {
  const qc = useQueryClient();
  const overview = useQuery({ queryKey: ["overview"], queryFn: () => api.get<GroupStatus[]>("/admin/api/sync/overview"), refetchInterval: 10000 });
  const packages = useQuery({ queryKey: ["packages"], queryFn: () => api.get<Package[]>("/admin/api/packages"), refetchInterval: 5000 });
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals"), refetchInterval: 10000 });
  const [tills, setTills] = useState<string[]>([]);
  const [last, setLast] = useState<Package[]>([]);
  const broadcast = useMutation({
    mutationFn: async (groups: Group[]) => {
      const out: Package[] = [];
      for (const group of groups) out.push(await api.post<Package>("/admin/api/sync/broadcast", { group, terminalIds: tills }));
      return out;
    },
    onSuccess: (p) => { setLast(p); qc.invalidateQueries({ queryKey: ["overview"] }); qc.invalidateQueries({ queryKey: ["packages"] }); },
  });
  const pendingGroups = (overview.data ?? []).filter((g) => g.pendingChanges > 0).map((g) => g.group);
  const counts = (p: Package) => Object.entries(p.counts).filter(([, n]) => n > 0).map(([k, n]) => `${n} ${ENTITY_LABEL[k as Entity].toLowerCase()}`).join(", ");

  return (
    <>
      <PageHead title="Dashboard" sub="Broadcast configuration to tills, one entity at a time or everything that changed."
        actions={<button className="primary" disabled={!pendingGroups.length || !tills.length || broadcast.isPending} onClick={() => broadcast.mutate(pendingGroups)}>
          Broadcast all changes{pendingGroups.length ? ` (${pendingGroups.length})` : ""}</button>} />
      <section className="panel slim">
        <div className="row"><span className="label">Send to</span><TillPicker value={tills} onChange={setTills} /></div>
      </section>
      <ErrorText error={broadcast.error ?? overview.error} />
      {last.length > 0 && (
        <Notice>
          {last.map((p) => <div key={p.packageNo}><strong>Package #{p.packageNo}</strong> sent: {counts(p)}{p.warnings.map((w) => <div key={w} className="warn">⚠ {w}</div>)}</div>)}
        </Notice>
      )}

      <div className="cards">
        {(overview.data ?? []).map((g) => (
          <div key={g.group} className={`card-entity ${g.pendingChanges ? "has-changes" : ""}`}>
            <div className="card-top">
              <span className="card-icon">{ICON[g.group]}</span>
              <div className="card-title">{g.label}</div>
              {g.pendingChanges > 0 ? <span className="badge-count">{g.pendingChanges} change{g.pendingChanges === 1 ? "" : "s"}</span> : <span className="pill ok">Up to date</span>}
            </div>
            <div className="card-meta muted small">
              {g.lastPackageNo ? <>Last broadcast <strong>#{g.lastPackageNo}</strong> · {when(g.lastPackageAt)}</> : "Never broadcast"}
            </div>
            <div className="card-actions">
              <button className="link" onClick={() => go(g.group === "MENUS" ? "menus" : g.group === "TAX_RATES" ? "taxRates" : g.group.toLowerCase())}>Configure</button>
              <span className="spacer" />
              <button className={g.pendingChanges || !g.lastPackageNo ? "primary" : ""} disabled={!tills.length || broadcast.isPending || (!g.pendingChanges && !!g.lastPackageNo)}
                onClick={() => broadcast.mutate([g.group])}>{g.lastPackageNo ? "Broadcast" : "Broadcast all"}</button>
            </div>
          </div>
        ))}
      </div>

      <h3 className="section-title">Tills</h3>
      <div className="tills">
        {(terminals.data ?? []).filter((t) => t.active).map((t) => (
          <button key={t.id} className="till-card" onClick={() => go("terminals")}>
            <span className="code-badge lg">{t.code}</span>
            <span className="till-text"><strong>{t.name}</strong><span className="muted small">{isOnline(t) ? "● Online" : `Offline · last seen ${when(t.lastSeenAt)}`}</span>
              <span className="muted small">Last package {t.lastAppliedPackage ? `#${t.lastAppliedPackage}` : "none yet"}</span></span>
          </button>
        ))}
      </div>

      <h3 className="section-title">Recent packages</h3>
      <table className="grid">
        <thead><tr><th>#</th><th>Created</th><th>Contents</th><th>Tills</th></tr></thead>
        <tbody>
          {(packages.data ?? []).slice(0, 6).map((p) => (
            <tr key={p.packageNo}><td className="strong">#{p.packageNo}</td><td>{when(p.createdAt)}{p.note && <div className="muted small">{p.note}</div>}</td>
              <td className="muted">{counts(p)}</td>
              <td>{p.targets.map((t) => <div key={t.terminalId} className="target">{t.terminalName} <Status value={t.status} /></div>)}</td></tr>
          ))}
          {packages.data?.length === 0 && <tr><td colSpan={4} className="empty">Nothing broadcast yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
