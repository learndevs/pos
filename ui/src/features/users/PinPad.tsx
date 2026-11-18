import { useEffect } from "react";

const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "clear", "0", "back"] as const;

interface Props {
  value: string;
  onChange(pin: string): void;
  onSubmit(): void;
  maxLength?: number;
  disabled?: boolean;
}

/** Touch-first PIN entry. Also accepts the physical keyboard (digits, Backspace, Enter, Escape). */
export function PinPad({ value, onChange, onSubmit, maxLength = 8, disabled }: Props) {
  const press = (key: (typeof KEYS)[number]) => {
    if (disabled) return;
    if (key === "clear") onChange("");
    else if (key === "back") onChange(value.slice(0, -1));
    else if (value.length < maxLength) onChange(value + key);
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (disabled) return;
      if (/^\d$/.test(e.key)) press(e.key as (typeof KEYS)[number]);
      else if (e.key === "Backspace") press("back");
      else if (e.key === "Escape") press("clear");
      else if (e.key === "Enter") onSubmit();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <div className="pinpad">
      <div className="pin-dots" aria-label={`${value.length} digits entered`}>
        {Array.from({ length: Math.max(4, value.length) }, (_, i) => (
          <span key={i} className={i < value.length ? "dot filled" : "dot"} />
        ))}
      </div>
      <div className="pin-keys">
        {KEYS.map((k) => (
          <button key={k} type="button" className={`key ${k.length > 1 ? "key-fn" : ""}`} onClick={() => press(k)} disabled={disabled}>
            {k === "back" ? "⌫" : k === "clear" ? "Clear" : k}
          </button>
        ))}
      </div>
      <button type="button" className="btn-primary wide" onClick={onSubmit} disabled={disabled || value.length < 4}>
        Sign in
      </button>
    </div>
  );
}
