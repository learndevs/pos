import { contextBridge, ipcRenderer } from "electron";

/** The only surface the renderer gets. Keep it small and typed (mirrors ui/src/types/pos-bridge.d.ts). */
contextBridge.exposeInMainWorld("pos", {
  getConfig: (): Promise<{ apiToken: string; version: string }> => ipcRenderer.invoke("pos:config"),
});
