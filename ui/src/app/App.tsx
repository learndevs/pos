import { useQuery } from "@tanstack/react-query";
import { useAuth } from "./authStore";
import "./theme";
import { SignIn } from "../features/users/SignIn";
import { CreateOwner } from "../features/users/CreateOwner";
import { authApi } from "../features/users/authApi";
import { SaleScreen } from "../features/sales/SaleScreen";

export function App() {
  const user = useAuth((s) => s.user);
  const setup = useQuery({ queryKey: ["setup-status"], queryFn: authApi.setupStatus, enabled: !user });

  if (user) return <SaleScreen />;
  if (setup.isPending) return <div className="center-screen muted">Connecting to POS server...</div>;
  if (setup.isError)
    return (
      <div className="center-screen">
        <div className="card">
          <p className="error">{setup.error.message}</p>
          <button className="btn" type="button" onClick={() => setup.refetch()}>
            Retry
          </button>
        </div>
      </div>
    );
  return setup.data.needsOwner ? <CreateOwner /> : <SignIn />;
}
