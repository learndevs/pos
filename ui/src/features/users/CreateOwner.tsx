import { useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../app/authStore";
import { authApi } from "./authApi";

/** First run only: no users exist yet, so the owner creates their account and is signed in. */
export function CreateOwner() {
  const qc = useQueryClient();
  const signIn = useAuth((s) => s.signIn);
  const [form, setForm] = useState({ displayName: "", username: "", pin: "", confirm: "" });
  const [localError, setLocalError] = useState<string>();

  const create = useMutation({
    mutationFn: async () => {
      await authApi.createOwner({ username: form.username, displayName: form.displayName, pin: form.pin });
      return authApi.login(form.username, form.pin);
    },
    onSuccess: async (r) => {
      signIn(r.token, r.user);
      await qc.invalidateQueries();
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!/^\d{4,8}$/.test(form.pin)) return setLocalError("PIN must be 4 to 8 digits.");
    if (form.pin !== form.confirm) return setLocalError("PINs do not match.");
    if (!/^[A-Za-z0-9._-]+$/.test(form.username)) return setLocalError("Username: letters, numbers, dot, dash or underscore.");
    setLocalError(undefined);
    create.mutate();
  };

  const field = (key: keyof typeof form) => ({
    value: form[key],
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value }),
  });

  return (
    <div className="center-screen">
      <form className="card setup" onSubmit={submit}>
        <h1>Welcome</h1>
        <p className="muted">Create the owner account to finish setting up this till.</p>
        <label>
          Your name
          <input required maxLength={80} autoFocus {...field("displayName")} />
        </label>
        <label>
          Username
          <input required maxLength={50} autoCapitalize="none" {...field("username")} />
        </label>
        <label>
          PIN (4 to 8 digits)
          <input required type="password" inputMode="numeric" maxLength={8} {...field("pin")} />
        </label>
        <label>
          Confirm PIN
          <input required type="password" inputMode="numeric" maxLength={8} {...field("confirm")} />
        </label>
        {(localError || create.isError) && (
          <p className="error" role="alert">{localError ?? create.error?.message}</p>
        )}
        <button className="btn-primary wide" type="submit" disabled={create.isPending}>
          {create.isPending ? "Creating..." : "Create owner and sign in"}
        </button>
      </form>
    </div>
  );
}
