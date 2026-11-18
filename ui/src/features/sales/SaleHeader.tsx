import { useEffect, useState } from "react";
import { useAuth } from "../../app/authStore";
import { ThemePicker } from "../../app/ThemePicker";
import { authApi } from "../users/authApi";
import { HeaderActions } from "./HeaderActions";
import { SyncChip } from "./SyncChip";
import { useSaleData } from "./useSaleData";

export function SaleHeader() {
  const { business, session, sale } = useSaleData();
  const { user, signOut } = useAuth();
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 15_000);
    return () => clearInterval(t);
  }, []);

  const open = sale?.status === "OPEN" ? sale : null;

  return (
    <header className="sale-header">
      <div className="hdr-left">
        <strong className="biz">{business?.name ?? "POS"}</strong>
        <div className="hdr-meta">
          <span className={session ? "chip ok" : "chip warn"}>{session ? `Day ${session.businessDate}` : "Day not started"}</span>
          {open && (
            <span className="txn" title="Current transaction">
              Txn <strong>#{open.txnNo}</strong>
              <span className="muted"> · {open.orderType === "DINE_IN" ? "Dine in" : "Takeaway"}</span>
            </span>
          )}
        </div>
      </div>
      <HeaderActions />
      <div className="hdr-right">
        <SyncChip />
        <ThemePicker />
        <span className="who">
          {user?.displayName}
          <span className="badge">{user?.role}</span>
        </span>
        <button
          type="button"
          className="btn ghost"
          onClick={async () => {
            await authApi.logout().catch(() => undefined);
            signOut();
          }}
        >
          Sign out
        </button>
        <span className="clock">{now.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
      </div>
    </header>
  );
}
