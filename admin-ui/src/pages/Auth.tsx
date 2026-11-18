import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, useSession, type AdminUser } from "../api";
import { ErrorText, Field } from "../ui";

/** First run: create the admin account. Afterwards: username + password. */
export function Auth() {
  const setup = useQuery({ queryKey: ["setup"], queryFn: () => api.get<{ needsSetup: boolean }>("/admin/api/setup/status") });
  const session = useSession();
  const [f, setF] = useState({ username: "", displayName: "", password: "" });
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      if (setup.data?.needsSetup) await api.post("/admin/api/setup", f);
      const r = await api.post<{ token: string; user: AdminUser }>("/admin/api/login", { username: f.username, password: f.password });
      session.set(r.token, r.user);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  if (setup.isPending) return <div className="center muted">Connecting...</div>;
  if (setup.isError) return <div className="center"><ErrorText error={setup.error} /></div>;
  const first = setup.data.needsSetup;
  return (
    <div className="center">
      <form className="card auth" onSubmit={submit}>
        <div className="brand">POS <span>Admin</span></div>
        <h2>{first ? "Create the admin account" : "Sign in"}</h2>
        {first && <Field label="Your name"><input required value={f.displayName} onChange={(e) => setF({ ...f, displayName: e.target.value })} /></Field>}
        <Field label="Username"><input required autoFocus autoCapitalize="none" value={f.username} onChange={(e) => setF({ ...f, username: e.target.value })} /></Field>
        <Field label="Password" hint={first ? "At least 8 characters" : undefined}>
          <input required type="password" minLength={first ? 8 : undefined} value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
        </Field>
        <ErrorText error={error} />
        <button className="primary wide" disabled={busy}>{busy ? "Please wait..." : first ? "Create and sign in" : "Sign in"}</button>
      </form>
    </div>
  );
}
