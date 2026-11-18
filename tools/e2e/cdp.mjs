// Minimal Chrome DevTools Protocol helper for driving the Electron app (remote debugging :9333).
import fs from "node:fs";
import path from "node:path";

const PORT = process.env.E2E_CDP_PORT ?? "9333";
export const OUT = path.resolve(path.dirname(new URL(import.meta.url).pathname), "out");
fs.mkdirSync(OUT, { recursive: true });
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const targets = async () => (await fetch(`http://127.0.0.1:${PORT}/json`)).json();

async function waitForTarget(pred, ms = 30000) {
  const end = Date.now() + ms;
  while (Date.now() < end) {
    try { const t = (await targets()).find(pred); if (t) return t; } catch { /* not up yet */ }
    await sleep(300);
  }
  throw new Error("CDP target not found");
}

async function attach(target) {
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener("open", r, { once: true }));
  let id = 0;
  const pending = new Map();
  ws.addEventListener("message", (m) => {
    const d = JSON.parse(m.data);
    if (d.id && pending.has(d.id)) { pending.get(d.id)(d); pending.delete(d.id); }
  });
  const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });
  const evaluate = async (expression) => {
    const r = await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
    if (r.result?.exceptionDetails) throw new Error(r.result.exceptionDetails.text);
    return r.result?.result?.value;
  };
  const shot = async (name) => {
    const r = await send("Page.captureScreenshot", { format: "png" });
    const file = path.join(OUT, `${name}.png`);
    fs.writeFileSync(file, Buffer.from(r.result.data, "base64"));
    return file;
  };
  return { evaluate, shot, close: () => ws.close() };
}

/** The main POS window plus helpers that act like a cashier. */
export async function app(urlPrefix = "http://localhost:5174") {
  const page = await attach(await waitForTarget((t) => t.type === "page" && t.url.startsWith(urlPrefix)));
  const api = {
    ...page,
    /** Click the first enabled button whose first text line equals label. */
    click: (label) => page.evaluate(`(() => { const b = [...document.querySelectorAll('button')].find(b => b.innerText.trim().split('\\n')[0].trim() === ${JSON.stringify(label)}); if (!b) return 'missing'; if (b.disabled) return 'disabled'; b.click(); return 'ok'; })()`),
    /** Wait until a selector exists (e.g. async search results). Returns true/false. */
    waitFor: async (sel, ms = 5000) => {
      const end = Date.now() + ms;
      while (Date.now() < end) {
        if (await page.evaluate(`!!document.querySelector(${JSON.stringify(sel)})`)) return true;
        await sleep(100);
      }
      return false;
    },
    clickSelector: (sel) => page.evaluate(`(() => { const e = document.querySelector(${JSON.stringify(sel)}); if (!e) return 'missing'; e.click(); return 'ok'; })()`),
    /** Keyboard key as the till sees it (digits, Enter, Escape, Backspace). */
    key: (k) => page.evaluate(`window.dispatchEvent(new KeyboardEvent('keydown', { key: ${JSON.stringify(k)} }))`),
    keys: async (s) => { for (const k of s) await api.key(k); },
    /** Type into an input (React-compatible). */
    type: (sel, text) => page.evaluate(`(() => { const i = document.querySelector(${JSON.stringify(sel)}); const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; set.call(i, ${JSON.stringify(text)}); i.dispatchEvent(new Event('input', { bubbles: true })); return 'ok'; })()`),
    /** What the cashier sees right now. */
    state: () => page.evaluate(`({ txn: document.querySelector('.txn')?.innerText, total: document.querySelector('.total-amount')?.innerText, prompt: document.querySelector('.prompt')?.innerText, message: document.querySelector('.message')?.innerText.trim(), menu: document.querySelector('.crumbs')?.innerText, lines: [...document.querySelectorAll('.line')].map(l => l.innerText.split('\\n').filter(Boolean).join(' | ')) })`),
    /** Start from an empty basket: cancels a leftover open sale (needs ORDER_VOID, e.g. the test owner). */
    freshSale: async () => {
      const lines = await page.evaluate("document.querySelectorAll('.line').length");
      if (!lines) return "empty";
      await api.click("Done"); await sleep(200);            // leave the ITEM menu if a line was selected
      await api.click("Cancel sale"); await sleep(200);
      await api.key("Enter"); await sleep(1500);            // confirm; a new empty transaction starts
      return page.evaluate("document.querySelectorAll('.line').length === 0 ? 'cancelled leftover' : 'still not empty'");
    },
    /** Sign in from the user tiles with a PIN (no-op if already on the sale screen). */
    signIn: async (pin = "1234") => {
      if (await page.evaluate("!!document.querySelector('.sale-screen')")) return "already";
      await api.clickSelector(".user-tile"); await sleep(300);
      await api.keys(pin); await api.key("Enter"); await sleep(1500);
      return page.evaluate("!!document.querySelector('.sale-screen') ? 'ok' : 'failed'");
    },
  };
  return api;
}

/** Another Electron window by title prefix (e.g. "Cash Drawer", "Test Printer"). */
export async function window(titlePrefix, ms = 20000) {
  return attach(await waitForTarget((t) => t.title.startsWith(titlePrefix) || (titlePrefix === "Test Printer" && t.url.endsWith(".pdf")), ms));
}
