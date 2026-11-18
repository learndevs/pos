import { app, BrowserWindow } from "electron";
import fs from "node:fs";
import path from "node:path";

/**
 * Test receipt printer: renders the receipt HTML to an 80mm PDF (like paper coming out of a
 * thermal printer), saves it under <userData>/receipts, and shows it in a "Test Printer" window.
 */
let viewer: BrowserWindow | undefined;

export function receiptsDir(): string {
  return path.join(app.getPath("userData"), "receipts");
}

export async function printToTestPrinter(html: string, name: string, paperWidthMm = 80): Promise<string> {
  const pdf = await renderPdf(html, paperWidthMm);
  fs.mkdirSync(receiptsDir(), { recursive: true });
  const file = path.join(receiptsDir(), `${name.replace(/[^\w.-]+/g, "_")}.pdf`);
  fs.writeFileSync(file, pdf);
  await show(file, name);
  return file;
}

async function renderPdf(html: string, paperWidthMm: number): Promise<Buffer> {
  // Offscreen, sandboxed, no preload: the template only needs HTML/CSS.
  const win = new BrowserWindow({
    show: false,
    width: Math.round((paperWidthMm / 25.4) * 96),
    height: 800,
    webPreferences: { sandbox: true, contextIsolation: true, nodeIntegration: false, offscreen: true },
  });
  try {
    win.webContents.on("will-navigate", (e) => e.preventDefault());
    await win.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(html));
    // Content height (not the window height): the "paper" is cut right after the receipt.
    const heightPx: number = await win.webContents.executeJavaScript(
      "(() => { const b = document.body, cs = getComputedStyle(b); return Math.ceil(b.getBoundingClientRect().height + parseFloat(cs.marginTop) + parseFloat(cs.marginBottom)); })()",
    );
    const inch = 25.4;
    return await win.webContents.printToPDF({
      printBackground: true,
      margins: { top: 0, bottom: 0, left: 0, right: 0 },
      pageSize: { width: paperWidthMm / inch, height: Math.max(heightPx / 96 + 0.1, 1) }, // inches; one long "paper roll" page
    });
  } finally {
    win.destroy();
  }
}

async function show(file: string, name: string) {
  if (!viewer || viewer.isDestroyed()) {
    viewer = new BrowserWindow({
      width: 460,
      height: 760,
      title: "Test Printer",
      autoHideMenuBar: true,
      webPreferences: { plugins: true, sandbox: true, contextIsolation: true, nodeIntegration: false }, // plugins: built-in PDF viewer
    });
    viewer.on("closed", () => (viewer = undefined));
  }
  await viewer.loadFile(file);
  viewer.setTitle(`Test Printer · ${name}`);
  viewer.showInactive();
}
