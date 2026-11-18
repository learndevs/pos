import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, isOnline, when, type Terminal } from "../api";
import { ConfirmButton, ErrorText, Facts, Field, FormPage, Inspector, Notice, PageHead, RowActions, Section, Split } from "../ui";
import { PlusIcon } from "../icons";

type Screen = { kind: "list"; viewing?: Terminal } | { kind: "create" } | { kind: "edit"; terminal: Terminal } | { kind: "created"; tillNo: number; code: string; name: string; token: string };
type Form = { tillNo: string; code: string; name: string; location: string };

/** Independent tills: each has a till number, a device code (receipts) and its own token. */
export function Terminals() {
  const qc = useQueryClient();
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals"), refetchInterval: 10000 });
  const [screen, setScreen] = useState<Screen>({ kind: "list" });
  const [token, setToken] = useState<{ code: string; name: string; token: string }>();
  const refresh = () => qc.invalidateQueries({ queryKey: ["terminals"] });
  const reset = useMutation({ mutationFn: (id: string) => api.post<{ code: string; name: string; token: string }>(`/admin/api/terminals/${id}/token`), onSuccess: (r) => { setToken(r); refresh(); } });
  const sendAll = useMutation({
    mutationFn: (id: string) => api.post<{ packageNo: number }>("/admin/api/packages", { entities: ["TAX_RATE", "CATEGORY", "PRODUCT", "MENU_FOLDER", "MENU_BUTTON"], scope: "FULL", terminalIds: [id], note: "Full configuration for one till" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["packages"] }),
  });
  const disable = useMutation({ mutationFn: (id: string) => api.del(`/admin/api/terminals/${id}`), onSuccess: () => { refresh(); setScreen({ kind: "list" }); } });
  const go = (s: Screen) => { setToken(undefined); reset.reset(); sendAll.reset(); setScreen(s); };

  if (screen.kind === "create" || screen.kind === "edit") return <TillForm terminal={screen.kind === "edit" ? screen.terminal : undefined} onCancel={() => go({ kind: "list", viewing: screen.kind === "edit" ? screen.terminal : undefined })}
    onCreated={(r) => { refresh(); setScreen({ kind: "created", ...r }); }} onSaved={async (id) => { await refresh(); const t = (await qc.fetchQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals") })).find((x) => x.id === id); go({ kind: "list", viewing: t }); }} />;
  if (screen.kind === "created") return (
    <div className="form-page">
      <button type="button" className="back-link" onClick={() => go({ kind: "list" })}>← Terminals</button>
      <div className="form-head"><div><h1>Till {screen.tillNo} created</h1><p className="muted">{screen.code} · {screen.name}</p></div></div>
      <div className="form-body"><Section title="Connect the till" hint="On the till: header ▸ Device ▸ enter the admin address and this token. It is shown only once.">
        <div className="token"><code>{screen.token}</code><button onClick={() => navigator.clipboard?.writeText(screen.token)}>Copy</button></div>
      </Section></div>
      <div className="form-bar"><span className="spacer" /><button className="primary" onClick={() => go({ kind: "list" })}>Done</button></div>
    </div>
  );

  const v = screen.viewing;
  return (
    <>
      <PageHead title="Terminals" sub="Every till is an independent POS with a till number, a device code (printed in receipt numbers) and its own token."
        actions={<button className="primary with-icon" onClick={() => go({ kind: "create" })}><PlusIcon />Add till</button>} />
      <ErrorText error={disable.error} />
      <Split open={!!v}>
        <table className="grid">
          <thead><tr><th className="num">Till no.</th><th>Code</th><th>Name / location</th><th>Status</th><th>Last seen</th><th>Last package</th><th>Till ID</th><th className="actions-col">Actions</th></tr></thead>
          <tbody>{(terminals.data ?? []).map((x) => (
            <tr key={x.id} className={`click ${v?.id === x.id ? "selected" : ""}`} onClick={() => go({ kind: "list", viewing: x })}>
              <td className="num strong">{x.tillNo}</td><td><span className="code-badge">{x.code}</span></td>
              <td className="strong">{x.name}{x.location && <div className="muted small">{x.location}</div>}</td>
              <td>{!x.active ? <span className="pill">Disabled</span> : isOnline(x) ? <span className="pill ok">● Online</span> : <span className="pill">Offline</span>}</td>
              <td>{when(x.lastSeenAt)}</td><td>{x.lastAppliedPackage ? `#${x.lastAppliedPackage}` : "-"}</td><td><code>{x.id.slice(-8)}</code></td>
              <td className="actions-col"><RowActions onView={() => go({ kind: "list", viewing: x })} onEdit={() => go({ kind: "edit", terminal: x })}
                deleteLabel="Disable" onDelete={x.active ? () => disable.mutate(x.id) : undefined} /></td>
            </tr>
          ))}</tbody>
        </table>
        {v && (
          <Inspector title={`Till ${v.tillNo} · ${v.code}`} subtitle={v.name} onClose={() => go({ kind: "list" })}
            footer={<><ConfirmButton label="Reset token" confirmLabel="Yes, new token" onConfirm={() => reset.mutate(v.id)} />
              <button disabled={!v.active || sendAll.isPending} onClick={() => sendAll.mutate(v.id)} title="Full snapshot of all configuration to this till only">Send full config</button><span className="spacer" />
              <button className="primary" onClick={() => go({ kind: "edit", terminal: v })}>Edit</button></>}>
            <Facts items={[["Till number", v.tillNo], ["Device code", <span className="code-badge">{v.code}</span>], ["Name", v.name], ["Location", v.location], ["Till ID", <code>{v.id}</code>],
              ["Status", !v.active ? "Disabled" : isOnline(v) ? "Online" : "Offline"], ["Last seen", when(v.lastSeenAt)], ["Last package", v.lastAppliedPackage ? `#${v.lastAppliedPackage}` : "-"], ["Added", when(v.createdAt)]]} />
            {token ? <><Notice>New token for {token.code}. Shown once; the previous token no longer works.</Notice><div className="token"><code>{token.token}</code><button onClick={() => navigator.clipboard?.writeText(token.token)}>Copy</button></div></>
              : <Notice tone="info">On the till: header ▸ Device ▸ enter the admin address and this till's token.</Notice>}
            {sendAll.isSuccess && <Notice>Package #{sendAll.data.packageNo} (full configuration) sent to {v.code}. It applies on the till's next check.</Notice>}
            <ErrorText error={reset.error ?? sendAll.error} />
          </Inspector>
        )}
      </Split>
    </>
  );
}

function TillForm({ terminal, onCancel, onCreated, onSaved }: { terminal?: Terminal; onCancel(): void;
  onCreated(r: { tillNo: number; code: string; name: string; token: string }): void; onSaved(id: string): void }) {
  const next = useQuery({ queryKey: ["terminals-next"], queryFn: () => api.get<{ tillNo: number; code: string }>("/admin/api/terminals/next"), enabled: !terminal });
  const [step, setStep] = useState<"details" | "review">("details");
  const [f, setF] = useState<Form>(terminal ? { tillNo: String(terminal.tillNo), code: terminal.code, name: terminal.name, location: terminal.location ?? "" } : { tillNo: "", code: "", name: "", location: "" });
  const tillNo = f.tillNo || String(next.data?.tillNo ?? "");
  const code = f.code || (f.tillNo ? `T${f.tillNo.padStart(2, "0")}` : next.data?.code ?? "");
  const body = { tillNo: Number(tillNo), code, name: f.name, location: f.location };
  const save = useMutation({
    mutationFn: () => (terminal ? api.put(`/admin/api/terminals/${terminal.id}`, body) : api.post<{ tillNo: number; code: string; name: string; token: string }>("/admin/api/terminals", body)),
    onSuccess: (r) => (terminal ? onSaved(terminal.id) : onCreated(r as { tillNo: number; code: string; name: string; token: string })),
  });
  const valid = !!f.name.trim() && Number(tillNo) >= 1 && /^[A-Za-z0-9]{1,6}$/.test(code);
  return (
    <FormPage backLabel="Terminals" onCancel={onCancel} mode={terminal ? "edit" : "create"} step={step} onStep={setStep} canContinue={valid}
      title={terminal ? `Edit till ${terminal.tillNo}` : "Add till"} subtitle={terminal ? `Till ID ${terminal.id}` : "Register an independent till. A token is issued when you save."}
      onSave={() => save.mutate()} saving={save.isPending} error={save.error}
      details={<>
        <Section title="Identity" hint="The till number is for staff; the device code is printed in receipt numbers (T01-20261119-0001).">
          <Field label="Till number *"><input type="number" min={1} max={999} placeholder={String(next.data?.tillNo ?? "")} value={f.tillNo} onChange={(e) => setF({ ...f, tillNo: e.target.value })} /></Field>
          <Field label="Device code *" hint="1-6 letters or digits"><input maxLength={6} placeholder={code} value={f.code} onChange={(e) => setF({ ...f, code: e.target.value.toUpperCase() })} /></Field>
        </Section>
        <Section title="Name & place">
          <Field label="Name *"><input autoFocus value={f.name} placeholder="e.g. Front counter" onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
          <Field label="Location"><input value={f.location} placeholder="e.g. Bar, upstairs" onChange={(e) => setF({ ...f, location: e.target.value })} /></Field>
        </Section>
      </>}
      review={<Section title="Review" hint="Save registers the till and shows its token once."><Facts items={[["Till number", tillNo], ["Device code", <span className="code-badge">{code}</span>], ["Name", f.name], ["Location", f.location || "-"]]} /></Section>} />
  );
}
