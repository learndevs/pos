import { app, BrowserWindow, dialog, ipcMain, shell } from "electron";
import path from "node:path";
import { Backend, startBackend } from "./backend";
import { logDir } from "./paths";
import { openTestDrawer } from "./devices/testDrawer";
import { printToTestPrinter } from "./devices/testPrinter";

const smoke = process.argv.includes("--smoke");   // start server, check health, quit (CI / install check)
const devUiUrl = process.env.POS_DEV ? (process.env.POS_UI_URL ?? "http://localhost:5173") : undefined;
if (process.env.POS_DEV && !process.env.POS_SERVER_URL) process.env.POS_SERVER_URL = "http://127.0.0.1:8080";

let backend: Backend | undefined;
let win: BrowserWindow | undefined;

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });
  app.whenReady().then(boot);
}

function splash(): BrowserWindow {
  const s = new BrowserWindow({ width: 360, height: 200, frame: false, resizable: false, show: true });
  s.loadURL(
    "data:text/html," +
      encodeURIComponent(
        '<body style="font-family:system-ui;display:grid;place-items:center;height:100vh;margin:0">Starting POS...</body>',
      ),
  );
  return s;
}

async function boot(): Promise<void> {
  const s = smoke ? undefined : splash();
  try {
    backend = await startBackend();
  } catch (err) {
    s?.close();
    if (smoke) {
      console.error("SMOKE FAIL:", err);
      app.exit(1);
      return;
    }
    dialog.showErrorBox("POS could not start", `${String(err)}\n\nLogs: ${logDir()}`);
    app.quit();
    return;
  }

  if (smoke) {
    console.log(`SMOKE OK: server healthy at ${backend.baseUrl}`);
    await backend.stop();
    app.exit(0);
    return;
  }

  ipcMain.handle("pos:config", () => ({ apiToken: backend!.token, version: app.getVersion() }));

  // Devices. Only test devices exist so far (real ESC/POS printer + drawer kick come later).
  ipcMain.handle("pos:drawer:open", (_e, info: { changeText?: string; totalText?: string }) =>
    openTestDrawer(info ?? {}, win));
  ipcMain.handle("pos:printer:print", (_e, req: { html: string; name: string; paperWidthMm?: number }) =>
    printToTestPrinter(String(req.html), String(req.name), req.paperWidthMm));

  win = new BrowserWindow({
    width: 1280,
    height: 800,
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  // Only our own origin may load in the window; everything else opens in the system browser or is blocked.
  const allowedOrigin = new URL(devUiUrl ?? backend.baseUrl).origin;
  win.webContents.on("will-navigate", (e, url) => {
    if (new URL(url).origin !== allowedOrigin) e.preventDefault();
  });
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith("https://")) void shell.openExternal(url);
    return { action: "deny" };
  });

  win.once("ready-to-show", () => {
    s?.close();
    win!.show();
  });
  await win.loadURL(devUiUrl ?? `${backend.baseUrl}/`);
}

let quitting = false;
app.on("before-quit", (e) => {
  if (quitting || !backend) return;
  e.preventDefault();
  quitting = true;
  void backend.stop().finally(() => app.quit());
});

app.on("window-all-closed", () => app.quit());
