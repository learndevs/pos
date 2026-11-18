import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, when, type Terminal } from "../api";
import { ErrorText, Field, Modal, PageHead } from "../ui";

export function Terminals() {
  const qc = useQueryClient();
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals"), refetchInterval: 10000 });
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState("");
  const [created, setCreated] = useState<{ name: string; token: string }>();
  const create = useMutation({
    mutationFn: () => api.post<{ name: string; token: string }>("/admin/api/terminals", { name }),
    onSuccess: (r) => { setCreated(r); setAdding(false); setName(""); qc.invalidateQueries({ queryKey: ["terminals"] }); },
  });
  return (
    <>
      <PageHead title="Terminals" sub="Tills allowed to receive packages and upload transactions." actions={<button className="primary" onClick={() => setAdding(true)}>Add terminal</button>} />
      <table className="grid">
        <thead><tr><th>Name</th><th>Last seen</th><th>Last applied package</th><th>Status</th></tr></thead>
        <tbody>{(terminals.data ?? []).map((t) => (
          <tr key={t.id}><td className="strong">{t.name}</td><td>{when(t.lastSeenAt)}</td><td>{t.lastAppliedPackage ? `#${t.lastAppliedPackage}` : "-"}</td>
            <td><span className={t.active ? "pill ok" : "pill"}>{t.active ? "Active" : "Disabled"}</span></td></tr>
        ))}</tbody>
      </table>
      {adding && (
        <Modal title="Add terminal" onClose={() => setAdding(false)} footer={<><ErrorText error={create.error} /><span className="spacer" />
          <button onClick={() => setAdding(false)}>Cancel</button><button className="primary" disabled={!name.trim()} onClick={() => create.mutate()}>Create</button></>}>
          <Field label="Name" hint="e.g. Front counter"><input autoFocus value={name} onChange={(e) => setName(e.target.value)} /></Field>
        </Modal>
      )}
      {created && (
        <Modal title={`Token for ${created.name}`} onClose={() => setCreated(undefined)} footer={<><span className="spacer" /><button className="primary" onClick={() => setCreated(undefined)}>Done</button></>}>
          <p>Set this on the till as <code>pos.sync.terminal-token</code>. It is shown only once.</p>
          <div className="token"><code>{created.token}</code><button onClick={() => navigator.clipboard?.writeText(created.token)}>Copy</button></div>
        </Modal>
      )}
    </>
  );
}
