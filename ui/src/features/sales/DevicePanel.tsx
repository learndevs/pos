import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { create } from "zustand";
import { api } from "../../api/client";
import { useAuth } from "../../app/authStore";

export interface DeviceInfo { code: string; name: string | null; terminalId: string | null; connected: boolean; adminUrl: string | null; fromConfig: boolean }

export const useDevicePanel = create<{ isOpen: boolean; open(): void; close(): void }>((set) => ({
  isOpen: false, open: () => set({ isOpen: true }), close: () => set({ isOpen: false }),
}));

export const useDevice = () => useQuery({ queryKey: ["device"], queryFn: () => api<DeviceInfo>("/api/device"), refetchInterval: 30_000 });

/** This till: its code (from admin) and its connection to the admin app (address + terminal token). */
export function DevicePanel() {
  const { isOpen, close } = useDevicePanel();
  const qc = useQueryClient();
  const device = useDevice();
  const canManage = !!useAuth((s) => s.user)?.permissions.includes("SETTINGS_MANAGE");
  const [url, setUrl] = useState("");
  const [token, setToken] = useState("");
  const done = (d: DeviceInfo) => { qc.setQueryData(["device"], d); qc.invalidateQueries({ queryKey: ["sync-status"] }); setToken(""); };
  const connect = useMutation({ mutationFn: () => api<DeviceInfo>("/api/device/connection", { method: "PUT", body: JSON.stringify({ adminUrl: url || device.data?.adminUrl, token }) }), onSuccess: done });
  const disconnect = useMutation({ mutationFn: () => api<DeviceInfo>("/api/device/connection", { method: "DELETE" }), onSuccess: done });
  if (!isOpen) return null;
  const d = device.data;
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && close()}>
      <div className="modal device" role="dialog" aria-label="This device">
        <header className="device-head">
          <div className="device-code">{d?.code ?? "..."}</div>
          <div><h3>{d?.name ?? "This till"}</h3><div className="muted small">{d?.connected ? `Connected to ${d.adminUrl}` : "Stand-alone (not connected to admin)"}</div></div>
          <button type="button" className="nav-btn" onClick={close}>Close</button>
        </header>
        <div className="device-body">
          <p className="muted small">Each till is its own POS. Add it under <strong>Terminals</strong> in the admin app, then enter the admin address and the terminal token here. The till code and name come from admin.</p>
          {d?.fromConfig && <p className="muted small">Currently connected using the installer configuration. Connecting here overrides it.</p>}
          <label className="dfield">Admin address<input placeholder="http://192.168.1.10:8081" value={url} onChange={(e) => setUrl(e.target.value)} disabled={!canManage} /></label>
          <label className="dfield">Terminal token<input type="password" placeholder="Token shown when the terminal was created" value={token} onChange={(e) => setToken(e.target.value)} disabled={!canManage} /></label>
          {!canManage && <p className="error">Only an owner can change the connection.</p>}
          {(connect.error || disconnect.error) && <p className="error">{(connect.error ?? disconnect.error)?.message}</p>}
          {connect.isSuccess && <p className="ok-text">Connected as {connect.data.code}{connect.data.name ? ` · ${connect.data.name}` : ""}.</p>}
        </div>
        <footer className="device-foot">
          {d?.connected && !d.fromConfig && <button type="button" className="nav-btn" disabled={!canManage || disconnect.isPending} onClick={() => disconnect.mutate()}>Disconnect</button>}
          <span className="spacer" />
          <button type="button" className="btn-primary" disabled={!canManage || !token || (!url && !d?.adminUrl) || connect.isPending} onClick={() => connect.mutate()}>
            {connect.isPending ? "Connecting..." : "Connect"}
          </button>
        </footer>
      </div>
    </div>
  );
}
