/** API exposed by desktop/src/preload.ts. Absent when the UI runs in a plain browser (dev). */
export interface PosBridge {
  getConfig(): Promise<{ apiToken: string; version: string }>;
  print?(jobId: string): Promise<void>;
  openDrawer?(): Promise<void>;
}

declare global {
  interface Window {
    pos?: PosBridge;
  }
}
