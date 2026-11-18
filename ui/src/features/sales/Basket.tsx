import { useEffect, useRef } from "react";
import { useSaleScreen } from "./saleStore";
import { usePosDispatch } from "./actions";
import { useSaleData } from "./useSaleData";

export function Basket() {
  const { sale, money } = useSaleData();
  const selectedLineId = useSaleScreen((s) => s.selectedLineId);
  const { dispatch } = usePosDispatch();
  const end = useRef<HTMLDivElement>(null);
  const lines = sale?.lines ?? [];
  const saleLines = lines.filter((l) => l.kind === "SALE");
  const tenders = lines.filter((l) => l.kind === "TENDER");
  const finished = sale && sale.status !== "OPEN";

  useEffect(() => {
    void end.current?.scrollIntoView({ block: "end" });
  }, [lines.length]);

  return (
    <section className="basket">
      <div className="basket-head">
        <span>#</span>
        <span>Item</span>
        <span className="c">Qty</span>
        <span className="num">Amount</span>
      </div>
      <div className={`basket-lines ${finished ? "finished" : ""}`}>
        {lines.length === 0 && <p className="muted empty">Basket is empty. Tap a product to start a sale.</p>}
        {saleLines.map((l) => (
          <button
            type="button"
            key={l.id}
            className={`line ${l.id === selectedLineId ? "selected" : ""}`}
            onClick={() => !finished && void dispatch({ type: "line", lineId: l.id })}
          >
            <span className="no">{l.lineNo}</span>
            <span className="line-name">
              {l.name}
              <small className="muted">
                {money(l.unitPriceMinor ?? 0)} each{l.itemClass === "NON_MERCHANDISE" ? " · charge" : ""}
              </small>
              {!!l.discountMinor && (
                <small className="discount">
                  -{l.discountPercent}% · -{money(l.discountMinor)}
                </small>
              )}
            </span>
            <span className="c">{l.quantity}</span>
            <span className="num">{money(l.amountMinor)}</span>
          </button>
        ))}
        {tenders.length > 0 && <div className="divider">Payments</div>}
        {tenders.map((l) => (
          <button
            type="button"
            key={l.id}
            className={`line tender ${l.id === selectedLineId ? "selected" : ""}`}
            onClick={() => !finished && void dispatch({ type: "line", lineId: l.id })}
          >
            <span className="no">{l.lineNo}</span>
            <span className="line-name">
              {l.tenderMethod === "CASH" ? "Cash" : l.tenderMethod === "CARD" ? "Card" : "Other"}
              {l.tenderedMinor != null && l.tenderedMinor !== l.amountMinor && (
                <small className="muted">
                  given {money(l.tenderedMinor)} · change {money(l.changeMinor ?? 0)}
                </small>
              )}
            </span>
            <span />
            <span className="num">-{money(l.amountMinor)}</span>
          </button>
        ))}
        <div ref={end} />
      </div>
      <div className="basket-foot">
        <div><span>Subtotal</span><span className="num">{money(sale?.subtotalMinor ?? 0)}</span></div>
        <div className="muted"><span>Tax included</span><span className="num">{money(sale?.taxMinor ?? 0)}</span></div>
        {(sale?.paidMinor ?? 0) > 0 && <div><span>Paid</span><span className="num">{money(sale!.paidMinor)}</span></div>}
        {finished && sale?.status === "COMPLETED" && (
          <div className="done">
            <span>Completed · Receipt {sale.receiptNo}</span>
            <span className="num">{sale.changeMinor > 0 ? `Change ${money(sale.changeMinor)}` : ""}</span>
          </div>
        )}
      </div>
    </section>
  );
}
