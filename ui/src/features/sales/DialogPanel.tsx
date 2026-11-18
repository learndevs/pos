import { useEffect } from "react";
import { useSaleScreen } from "./saleStore";
import { usePosDispatch } from "./actions";
import { useSaleData } from "./useSaleData";

const KEYS = ["7", "8", "9", "4", "5", "6", "1", "2", "3", "0", "00"];

/** Prompt + input display + keypad. Also reacts to the physical keyboard. */
export function DialogPanel() {
  const ui = useSaleScreen();
  const { session, sessionLoaded, money } = useSaleData();
  const { dispatch, cancelPrompt } = usePosDispatch();
  const selected = useSaleData().sale?.lines.find((l) => l.id === ui.selectedLineId);

  const noDay = sessionLoaded && !session;
  const prompt =
    noDay ? "Start the day: enter the opening float in the drawer, then press ENTER"
    : ui.mode.kind === "INPUT" || ui.mode.kind === "CONFIRM" ? ui.mode.prompt
    : ui.menu === "TENDER" ? "Type an amount, then a tender. No amount = exact."
    : ui.menu === "ITEM" && selected ? `Line ${selected.lineNo} selected: ${selected.name ?? selected.tenderMethod}. Choose an action, or Done.`
    : "Type a quantity, then a product";

  const asMoney = noDay || ui.menu === "TENDER";
  const display = ui.mode.kind === "CONFIRM" ? "ENTER = yes · C = no" : ui.keypad === "" ? "" : asMoney ? money(Number(ui.keypad)) : ui.keypad;

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement) return;
      if (/^\d$/.test(e.key)) ui.press(e.key);
      else if (e.key === "Backspace") ui.backspace();
      else if (e.key === "Escape") cancelPrompt();
      else if (e.key === "Enter") void dispatch({ type: "enter" });
      else return;
      e.preventDefault();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <section className="dialog-panel">
      <div className="readout">
        <div className={`prompt ${noDay ? "warn" : ""}`}>{prompt}</div>
        <div className="display" aria-live="polite">{display || <span className="placeholder">0</span>}</div>
        <div className={`message ${ui.message?.tone ?? ""}`} role="status">{ui.message?.text ?? "\u00a0"}</div>
      </div>
      <div className="keypad">
        {KEYS.map((k) => (
          <button key={k} type="button" className="k" onClick={() => ui.press(k)}>
            {k}
          </button>
        ))}
        <button type="button" className="k fn" onClick={() => ui.backspace()} aria-label="Backspace">⌫</button>
        <button type="button" className="k fn" onClick={cancelPrompt}>Clear</button>
        <button type="button" className="k enter" onClick={() => void dispatch({ type: "enter" })}>Enter</button>
      </div>
    </section>
  );
}
