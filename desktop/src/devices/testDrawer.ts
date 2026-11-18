import { BrowserWindow } from "electron";

/**
 * Test cash drawer: a small window that represents the open drawer.
 * The promise resolves when the cashier "closes the drawer" (button or closing the window),
 * which is what a real drawer's closed-sensor would report.
 */
let current: Promise<void> | undefined;

export function openTestDrawer(info: { changeText?: string; totalText?: string }, parent?: BrowserWindow): Promise<void> {
  if (current) return current;              // already open: wait for the same close
  current = new Promise<void>((resolve) => {
    const win = new BrowserWindow({
      width: 380,
      height: 300,
      title: "Cash Drawer (test)",
      resizable: false,
      minimizable: false,
      alwaysOnTop: true,
      parent,
      autoHideMenuBar: true,
      webPreferences: { sandbox: true, contextIsolation: true, nodeIntegration: false },
    });
    const esc = (s = "") => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);
    const html = `<!doctype html><html><head><meta charset="utf-8"><title>Cash Drawer (test)</title>
      <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'">
      <style>
        body{margin:0;height:100vh;display:grid;place-items:center;font:15px system-ui,-apple-system,"Segoe UI",sans-serif;background:#f3f4f6;color:#111827}
        .box{width:300px;text-align:center}
        .state{display:inline-block;padding:4px 12px;border-radius:999px;background:#dcfce7;color:#166534;font-weight:700;letter-spacing:.08em;font-size:12px}
        .tray{margin:14px auto;height:54px;border:2px solid #9ca3af;border-radius:6px;background:repeating-linear-gradient(90deg,#e5e7eb 0 58px,#d1d5db 58px 60px)}
        .change{font-size:26px;font-weight:700;margin:6px 0}
        .muted{color:#6b7280;font-size:13px}
        button{margin-top:14px;width:100%;height:48px;border:0;border-radius:8px;background:#2563eb;color:#fff;font:600 15px system-ui;cursor:pointer}
      </style></head><body><div class="box">
        <span class="state">DRAWER OPEN</span>
        <div class="tray"></div>
        ${info.changeText ? `<div class="muted">Change to give</div><div class="change">${esc(info.changeText)}</div>` : ""}
        ${info.totalText ? `<div class="muted">Sale total ${esc(info.totalText)}</div>` : ""}
        <button id="close" autofocus>Close drawer</button>
      </div><script>document.getElementById('close').onclick=()=>window.close()</script></body></html>`;
    win.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(html));
    win.on("closed", () => {
      current = undefined;
      resolve();
    });
  });
  return current;
}
