import { useSaleScreen } from "./saleStore";
import { usePosDispatch } from "./actions";
import { useSaleData } from "./useSaleData";

export function TotalButton() {
  const { sale, money } = useSaleData();
  const menu = useSaleScreen((s) => s.menu);
  const { dispatch } = usePosDispatch();
  const open = sale?.status === "OPEN" ? sale : null;
  const total = open?.totalMinor ?? 0;

  return (
    <button type="button" className={`total-button ${menu === "TENDER" ? "tendering" : ""}`} onClick={() => void dispatch({ type: "total" })} disabled={!open || total === 0}>
      <span className="total-text">
        <span className="total-label">{menu === "TENDER" ? "Back to sale" : "TOTAL"}</span>
        {open && open.paidMinor > 0 && <span className="total-due">Due {money(open.dueMinor)}</span>}
      </span>
      <span className="total-amount">{money(total)}</span>
    </button>
  );
}
