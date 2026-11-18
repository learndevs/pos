import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post } from "../../api/client";

interface SyncStatus {
  enabled: boolean;
  lastPackage: number;
  lastSyncAt: string | null;
  lastError: string | null;
  outboxPending: number;
}

const ago = (iso: string | null) => {
  if (!iso) return "never";
  const s = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  return s < 60 ? "just now" : s < 3600 ? `${Math.round(s / 60)} min ago` : `${Math.round(s / 3600)} h ago`;
};

/** Header chip: config package applied, last check, transactions waiting to upload. Tap = sync now. */
export function SyncChip() {
  const qc = useQueryClient();
  const status = useQuery({ queryKey: ["sync-status"], queryFn: () => api<SyncStatus>("/api/sync/status"), refetchInterval: 15_000 });
  const now = useMutation({ mutationFn: () => post<SyncStatus>("/api/sync/now", {}), onSuccess: (s) => qc.setQueryData(["sync-status"], s) });
  const lastPackage = useRef<number | undefined>(undefined);
  const [, tick] = useState(0);

  useEffect(() => {
    const t = setInterval(() => tick((n) => n + 1), 30_000); // keep "x min ago" fresh
    return () => clearInterval(t);
  }, []);

  // A new package changes products / prices: refresh menus and searches.
  useEffect(() => {
    const p = status.data?.lastPackage;
    if (p !== undefined && lastPackage.current !== undefined && p !== lastPackage.current) {
      void qc.invalidateQueries({ queryKey: ["menus"] });
      void qc.invalidateQueries({ queryKey: ["product-search"] });
    }
    lastPackage.current = p;
  }, [status.data?.lastPackage, qc]);

  const s = status.data;
  if (!s || !s.enabled) return null;
  const tone = s.lastError ? "warn" : "ok";
  return (
    <button type="button" className={`sync-chip ${tone}`} onClick={() => now.mutate()} disabled={now.isPending}
      title={s.lastError ?? "Tap to sync now"}>
      <span className="dot" />
      <span>{s.lastPackage ? `Config #${s.lastPackage}` : "No config yet"}</span>
      <span className="muted">· {now.isPending ? "syncing..." : s.lastError ? "sync error" : ago(s.lastSyncAt)}</span>
      {s.outboxPending > 0 && <span className="muted">· {s.outboxPending} to upload</span>}
    </button>
  );
}
