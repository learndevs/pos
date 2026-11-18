// Screenshot any URL in a hidden Electron window (no devices, no preload).
// Run: env -u ELECTRON_RUN_AS_NODE node_modules/electron/dist/Electron.app/Contents/MacOS/Electron tools/e2e/screenshot.cjs <url> <out.png> [width] [height]
const { app, BrowserWindow } = require("electron");
const fs = require("fs");
const [url, out, width = "1366", height = "768"] = process.argv.slice(-4).filter((a) => !a.endsWith("screenshot.cjs"));
app.whenReady().then(async () => {
  const w = new BrowserWindow({ show: false, width: Number(width), height: Number(height) });
  w.webContents.on("console-message", (e) => { if (e.level === "error") console.log("[console error]", e.message); });
  await w.loadURL(url);
  await new Promise((r) => setTimeout(r, 2000));
  fs.writeFileSync(out, (await w.webContents.capturePage()).toPNG());
  console.log("saved", out);
  app.quit();
});
