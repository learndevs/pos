import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, money, when, type Terminal, type Txn, type TxnSummary } from "../api";
import { Inspector, PageHead, Split, Status, Tabs } from "../ui";

/** Archive of every finished till transaction (completed and cancelled), uploaded by the tills. */
export function Transactions() {
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals") });
  const [f, setF] = useState({ terminalId: "", from: "", to: "", q: "" });
  const params = new URLSearchParams(Object.entries(f).filter(([, v]) => v) as [string, string][]).toString();
  const list = useQuery({ queryKey: ["transactions", params], queryFn: () => api.get<TxnSummary[]>(`/admin/api/transactions?${params}`), refetchInterval: 10000 });
  const [open, setOpen] = useState<string>();
  const tillName = (id: string) => terminals.data?.find((t) => t.id === id)?.name ?? "";
  return (
    <>
      <PageHead title="Transactions" sub="Every finished transaction from the tills (completed and cancelled)." />
      <div className="filters">
        <select value={f.terminalId} onChange={(e) => setF({ ...f, terminalId: e.target.value })}>
          <option value="">All tills</option>{terminals.data?.map((t) => <option key={t.id} value={t.id}>{t.code} · {t.name}</option>)}
        </select>
        <input type="date" value={f.from} onChange={(e) => setF({ ...f, from: e.target.value })} />
        <input type="date" value={f.to} onChange={(e) => setF({ ...f, to: e.target.value })} />
        <input className="search" placeholder="Receipt or txn no." value={f.q} onChange={(e) => setF({ ...f, q: e.target.value })} />
      </div>
      <Split open={!!open}>
        <table className="grid">
          <thead><tr><th>Date</th><th>Txn</th><th>Receipt</th><th>Till</th><th>Status</th><th className="num">Total</th></tr></thead>
          <tbody>
            {(list.data ?? []).map((t) => (
              <tr key={t.id} className={`click ${open === t.id ? "selected" : ""}`} onClick={() => setOpen(t.id)}>
                <td>{t.businessDate}</td><td className="strong">#{t.txnNo}</td><td>{t.receiptNo ?? "-"}</td><td><span className="code-badge">{t.deviceCode ?? "?"}</span> <span className="muted small">{tillName(t.terminalId)}</span></td>
                <td><Status value={t.status} /></td><td className="num">{money(t.totalMinor)}</td>
              </tr>
            ))}
            {list.data?.length === 0 && <tr><td colSpan={6} className="empty">No transactions.</td></tr>}
          </tbody>
        </table>
        {open && <Detail key={open} id={open} onClose={() => setOpen(undefined)} />}
      </Split>
    </>
  );
}

function Detail({ id, onClose }: { id: string; onClose(): void }) {
  const d = useQuery({ queryKey: ["transaction", id], queryFn: () => api.get<{ transaction: Txn }>(`/admin/api/transactions/${id}`) });
  const [tab, setTab] = useState<"summary" | "json">("summary");
  const t = d.data?.transaction;
  return (
    <Inspector title={t ? `Txn #${t.txnNo}` : "Transaction"} subtitle={t?.receiptNo ? `Receipt ${t.receiptNo}` : t?.status} onClose={onClose}
      tabs={<Tabs value={tab} onChange={setTab} size="sm" tabs={[["summary", "Summary"], ["json", "JSON"]]} />}>
      {t && tab === "summary" && (
        <>
          <dl className="facts">
            <dt>Status</dt><dd><Status value={t.status} />{t.reason && <span className="muted"> · {t.reason}</span>}</dd>
            <dt>Till</dt><dd>{t.deviceCode ?? "-"}</dd>
            <dt>Cashier</dt><dd>{t.cashierName ?? "-"}</dd>
            <dt>Order</dt><dd>{t.orderType === "DINE_IN" ? "Dine in" : "Takeaway"}</dd>
            <dt>Business day</dt><dd>{t.businessDate}</dd>
            <dt>Finished</dt><dd>{when(t.completedAt ?? t.cancelledAt)}</dd>
          </dl>
          <table className="grid compact">
            <tbody>
              {t.lines.map((l) => l.kind === "SALE" ? (
                <tr key={l.lineNo}><td>{l.quantity}×</td><td>{l.name}{!!l.discountMinor && <div className="muted small">-{l.discountPercent}% ({money(l.discountMinor)})</div>}</td><td className="num">{money(l.amountMinor)}</td></tr>
              ) : (
                <tr key={l.lineNo} className="tender"><td></td><td>{l.tenderMethod?.toLowerCase()}{l.changeMinor ? <div className="muted small">given {money(l.tenderedMinor)} · change {money(l.changeMinor)}</div> : null}</td><td className="num">-{money(l.amountMinor)}</td></tr>
              ))}
            </tbody>
          </table>
          <div className="totals"><span>Total</span><strong>{money(t.totalMinor)}</strong></div>
          <div className="muted small">Tax included {money(t.taxMinor)}</div>
        </>
      )}
      {t && tab === "json" && <pre className="json">{JSON.stringify(t, null, 2)}</pre>}
    </Inspector>
  );
}
