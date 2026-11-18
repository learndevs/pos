import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "../../app/authStore";
import { authApi, type UserTile } from "./authApi";
import { PinPad } from "./PinPad";

export function SignIn() {
  const signIn = useAuth((s) => s.signIn);
  const users = useQuery({ queryKey: ["auth-users"], queryFn: authApi.users });
  const [selected, setSelected] = useState<UserTile>();
  const [pin, setPin] = useState("");

  const login = useMutation({
    mutationFn: () => authApi.login(selected!.username, pin),
    onSuccess: (r) => signIn(r.token, r.user),
    onError: () => setPin(""),
  });

  const submit = () => {
    if (selected && pin.length >= 4 && !login.isPending) login.mutate();
  };

  return (
    <div className="center-screen">
      <div className="card signin">
        <h1>Sign in</h1>

        {!selected ? (
          <>
            <p className="muted">Who is using the till?</p>
            {users.isPending && <p className="muted">Loading...</p>}
            {users.isError && <p className="error">{users.error.message}</p>}
            <div className="user-tiles">
              {users.data?.map((u) => (
                <button key={u.id} type="button" className="user-tile" onClick={() => setSelected(u)}>
                  <span className="avatar">{u.displayName.slice(0, 1).toUpperCase()}</span>
                  {u.displayName}
                </button>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="selected-user">
              <span className="avatar">{selected.displayName.slice(0, 1).toUpperCase()}</span>
              <strong>{selected.displayName}</strong>
              <button
                type="button"
                className="link"
                onClick={() => {
                  setSelected(undefined);
                  setPin("");
                  login.reset();
                }}
              >
                Change
              </button>
            </div>
            <p className="muted">Enter your PIN</p>
            <PinPad value={pin} onChange={setPin} onSubmit={submit} disabled={login.isPending} />
            {login.isError && <p className="error" role="alert">{login.error.message}</p>}
          </>
        )}
      </div>
    </div>
  );
}
