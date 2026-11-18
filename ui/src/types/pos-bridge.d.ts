/** API exposed by desktop/src/preload.ts. Absent when the UI runs in a plain browser (dev). */
export interface PosBridge {
  getConfig(): Promise<{ apiToken: string; version: string }>;
  /** Opens the cash drawer; resolves when it is closed again. */
  openDrawer(info: { changeText?: string; totalText?: string }): Promise<void>;
  /** Prints receipt HTML; resolves with where it went (test printer: PDF path). */
  printReceipt(html: string, name: string, paperWidthMm?: number): Promise<string>;
}

declare global {
  interface Window {
    pos?: PosBridge;
  }
}
