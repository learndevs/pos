import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, type Terminal } from "../api";

/** Chips to choose target tills; all active tills selected by default. */
export function TillPicker({ value, onChange }: { value: string[]; onChange(ids: string[]): void }) {
  const terminals = useQuery({ queryKey: ["terminals"], queryFn: () => api.get<Terminal[]>("/admin/api/terminals") });
  const active = (terminals.data ?? []).filter((t) => t.active);
  useEffect(() => {
    if (active.length && value.length === 0) onChange(active.map((t) => t.id));
  }, [terminals.data]); // eslint-disable-line react-hooks/exhaustive-deps
  if (terminals.data && active.length === 0) return <span className="muted small">No tills yet: add one under Terminals.</span>;
  const all = value.length === active.length;
  return (
    <div className="chips">
      <button type="button" className="chip link-chip" onClick={() => onChange(all ? [] : active.map((t) => t.id))}>{all ? "Clear" : "Select all"}</button>
      {active.map((t) => {
        const on = value.includes(t.id);
        return (
          <button key={t.id} type="button" className={`chip ${on ? "on" : ""}`} aria-pressed={on}
            onClick={() => onChange(on ? value.filter((x) => x !== t.id) : [...value, t.id])}>
            <span className="tick">{on ? "✓" : ""}</span><span className="muted">{t.tillNo} ·</span>&nbsp;<strong>{t.code}</strong>&nbsp;{t.name}
          </button>
        );
      })}
    </div>
  );
}
