import { useEffect, useState } from "react";
import { useAuth } from "../../app/authStore";
import { ThemePicker } from "../../app/ThemePicker";
import { authApi } from "../users/authApi";
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
      <strong className="biz">{business?.name ?? "POS"}</strong>
      <span className="sep" />
      <span className={session ? "chip ok" : "chip warn"}>{session ? `Day ${session.businessDate}` : "Day not started"}</span>
      <span className="chip">{open ? `Sale · ${open.orderType === "DINE_IN" ? "Dine in" : "Takeaway"}` : sale?.receiptNo ? `Last receipt ${sale.receiptNo}` : "Ready"}</span>
      <span className="spacer" />
      <ThemePicker />
      <span className="who">
        {user?.displayName} <span className="badge">{user?.role}</span>
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
    </header>
  );
}
