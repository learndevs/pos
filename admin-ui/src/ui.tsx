import { useState, type ReactNode } from "react";
import { CheckIcon, EditIcon, EyeIcon, TrashIcon, XIcon } from "./icons";

/** Underlined tabs. `tabs` = [key, label, badge?]. */
export function Tabs<K extends string>({ tabs, value, onChange, size = "md" }: {
  tabs: [NoInfer<K>, string, ReactNode?][]; value: K; onChange: (k: NoInfer<K>) => void; size?: "md" | "sm";
}) {
  return (
    <div className={`tabs ${size}`} role="tablist">
      {tabs.map(([k, label, badge]) => (
        <button key={k} role="tab" aria-selected={value === k} className={value === k ? "active" : ""} onClick={() => onChange(k)}>
          {label}{badge !== undefined && badge !== null && badge !== false && <span className="tab-badge">{badge}</span>}
        </button>
      ))}
    </div>
  );
}

/** Right-hand editor panel (replaces pop-up windows): header, optional tabs, body, sticky footer. */
export function Inspector({ title, subtitle, onClose, tabs, footer, children }: {
  title: string; subtitle?: string; onClose(): void; tabs?: ReactNode; footer?: ReactNode; children: ReactNode;
}) {
  return (
    <aside className="inspector" aria-label={title}>
      <header>
        <div><h3>{title}</h3>{subtitle && <div className="muted small">{subtitle}</div>}</div>
        <button type="button" className="icon-btn" onClick={onClose} aria-label="Close panel">✕</button>
      </header>
      {tabs && <div className="inspector-tabs">{tabs}</div>}
      <div className="inspector-body">{children}</div>
      {footer && <footer>{footer}</footer>}
    </aside>
  );
}

/** List on the left, inspector on the right when open. */
export const Split = ({ children, open }: { children: ReactNode; open: boolean }) => <div className={`split ${open ? "open" : ""}`}>{children}</div>;

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function Toggle({ label, checked, onChange, disabled, hint }: { label: string; checked: boolean; onChange(v: boolean): void; disabled?: boolean; hint?: string }) {
  return (
    <label className={`toggle ${disabled ? "disabled" : ""}`}>
      <span><span className="toggle-label">{label}</span>{hint && <small className="muted">{hint}</small>}</span>
      <input type="checkbox" role="switch" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} />
    </label>
  );
}

/** Segmented control for small sets of options. */
export function Segmented<K extends string>({ options, value, onChange }: { options: [NoInfer<K>, string][]; value: K; onChange: (k: NoInfer<K>) => void }) {
  return (
    <div className="segmented" role="radiogroup">
      {options.map(([k, label]) => (
        <button key={k} type="button" role="radio" aria-checked={value === k} className={value === k ? "active" : ""} onClick={() => onChange(k)}>{label}</button>
      ))}
    </div>
  );
}

/** Inline two-step confirm instead of a confirm dialog. */
export function ConfirmButton({ label, confirmLabel = "Yes, remove", onConfirm, disabled }: { label: string; confirmLabel?: string; onConfirm(): void; disabled?: boolean }) {
  const [asking, setAsking] = useState(false);
  if (!asking) return <button type="button" className="danger-ghost" disabled={disabled} onClick={() => setAsking(true)}>{label}</button>;
  return (
    <span className="confirm">
      <span className="muted small">Sure?</span>
      <button type="button" className="danger" onClick={() => { setAsking(false); onConfirm(); }}>{confirmLabel}</button>
      <button type="button" onClick={() => setAsking(false)}>Cancel</button>
    </span>
  );
}

export function PageHead({ title, sub, actions }: { title: string; sub?: string; actions?: ReactNode }) {
  return (
    <div className="page-head">
      <div><h1>{title}</h1>{sub && <p className="muted">{sub}</p>}</div>
      <div className="actions">{actions}</div>
    </div>
  );
}

export const Status = ({ value }: { value: string }) => <span className={`status s-${value.toLowerCase()}`}>{value}</span>;
export const ErrorText = ({ error }: { error: unknown }) => (error ? <p className="error">{(error as Error).message}</p> : null);
export const Notice = ({ tone = "ok", children }: { tone?: "ok" | "warn" | "info"; children: ReactNode }) => <div className={`notice ${tone}`}>{children}</div>;

/** View / edit / delete icon buttons for a table row. Delete confirms inline (no dialog). */
export function RowActions({ onView, onEdit, onDelete, deleteLabel = "Delete" }: { onView?(): void; onEdit?(): void; onDelete?(): void; deleteLabel?: string }) {
  const [confirm, setConfirm] = useState(false);
  const stop = (f?: () => void) => (e: React.MouseEvent) => { e.stopPropagation(); f?.(); };
  if (confirm) {
    return (
      <span className="row-actions confirming" onClick={(e) => e.stopPropagation()}>
        <span className="small">{deleteLabel}?</span>
        <button className="icon-action danger" title={`Yes, ${deleteLabel.toLowerCase()}`} aria-label={`Confirm ${deleteLabel}`} onClick={stop(() => { setConfirm(false); onDelete?.(); })}><CheckIcon /></button>
        <button className="icon-action" title="Cancel" aria-label="Cancel" onClick={stop(() => setConfirm(false))}><XIcon /></button>
      </span>
    );
  }
  return (
    <span className="row-actions">
      {onView && <button className="icon-action" title="View" aria-label="View" onClick={stop(onView)}><EyeIcon /></button>}
      {onEdit && <button className="icon-action" title="Edit" aria-label="Edit" onClick={stop(onEdit)}><EditIcon /></button>}
      {onDelete && <button className="icon-action danger" title={deleteLabel} aria-label={deleteLabel} onClick={stop(() => setConfirm(true))}><TrashIcon /></button>}
    </span>
  );
}

/** Read-only key/value list for the inspector's view mode. */
export const Facts = ({ items }: { items: [string, ReactNode][] }) => (
  <dl className="facts">{items.map(([k, v]) => [<dt key={k + "t"}>{k}</dt>, <dd key={k + "d"}>{v ?? "-"}</dd>])}</dl>
);

/**
 * Full-page create / edit form (no side panel).
 * create: step 1 "Details" -> Continue -> step 2 "Review" -> Save.   edit: Save / Cancel directly.
 */
export function FormPage({ backLabel, onCancel, title, subtitle, mode, step, onStep, canContinue = true, onSave, saving, error, details, review }: {
  backLabel: string; onCancel(): void; title: string; subtitle?: string; mode: "create" | "edit";
  step: "details" | "review"; onStep(s: "details" | "review"): void; canContinue?: boolean;
  onSave(): void; saving: boolean; error: unknown; details: ReactNode; review: ReactNode;
}) {
  const creating = mode === "create";
  const onReview = creating && step === "review";
  return (
    <div className="form-page">
      <button type="button" className="back-link" onClick={onCancel}>← {backLabel}</button>
      <div className="form-head">
        <div><h1>{title}</h1>{subtitle && <p className="muted">{subtitle}</p>}</div>
        {creating && (
          <ol className="steps">
            <li className={step === "details" ? "active" : "done"}><span>1</span>Details</li>
            <li className={step === "review" ? "active" : ""}><span>2</span>Review &amp; save</li>
          </ol>
        )}
      </div>
      <div className="form-body">{onReview ? review : details}</div>
      <div className="form-bar">
        <ErrorText error={error} />
        <span className="spacer" />
        <button type="button" onClick={onCancel}>Cancel</button>
        {creating && step === "details" && <button type="button" className="primary" disabled={!canContinue} onClick={() => onStep("review")}>Continue</button>}
        {onReview && <button type="button" onClick={() => onStep("details")}>Back</button>}
        {(onReview || !creating) && <button type="button" className="primary" disabled={saving || !canContinue} onClick={onSave}>{saving ? "Saving..." : "Save"}</button>}
      </div>
    </div>
  );
}

/** A titled group of fields inside a form page. */
export const Section = ({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) => (
  <section className="form-section">
    <div className="form-section-head"><h3>{title}</h3>{hint && <p className="muted small">{hint}</p>}</div>
    <div className="form-section-body">{children}</div>
  </section>
);
