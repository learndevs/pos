import { contextBridge, ipcRenderer } from "electron";

/** The only surface the renderer gets. Keep it small and typed (mirrors ui/src/types/pos-bridge.d.ts). */
contextBridge.exposeInMainWorld("pos", {
  getConfig: (): Promise<{ apiToken: string; version: string }> => ipcRenderer.invoke("pos:config"),
  /** Opens the cash drawer; resolves when the drawer is closed again. */
  openDrawer: (info: { changeText?: string; totalText?: string }): Promise<void> => ipcRenderer.invoke("pos:drawer:open", info),
  /** Prints receipt HTML; resolves with where the output went (test printer: PDF path). */
  printReceipt: (html: string, name: string, paperWidthMm?: number): Promise<string> =>
    ipcRenderer.invoke("pos:printer:print", { html, name, paperWidthMm }),
});
