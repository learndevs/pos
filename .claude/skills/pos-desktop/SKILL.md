---
name: pos-desktop
description: Electron shell and React UI conventions: spawning the bundled JRE, health wait, IPC, security hardening, installer. Use when working in desktop/ or ui/.
---

# Desktop and UI conventions

**Electron main (desktop/)**
- `src/backend.ts` spawns the bundled `jlink` JRE with `server.jar` on a free port, with a random `POS_LAUNCH_TOKEN`; the renderer gets it via `window.pos.getConfig()` and sends it as `X-POS-Launch-Token`. Wait for `/actuator/health` (timeout 60 s, then show an error screen with log path).
- Show a splash immediately; JVM start is 5-15 s on old PCs.
- Restart the JVM on unexpected exit (limit retries); kill it on `before-quit`.
- Single-instance lock; second launch focuses the existing window.
- Security: `contextIsolation: true`, `nodeIntegration: false`, `sandbox: true`, strict CSP, block navigation and `window.open`, expose only a minimal typed preload API (`print`, `openDrawer`, `pickFolder`, `appInfo`).
- The server serves the built UI same-origin (no CORS); in dev, Vite on :5173 proxies `/api` to :8080.
- Run: `npm run dev` from the repo root (server + Vite + Electron). `npm run smoke -w desktop` starts the jar, checks health and quits. Always launch Electron via `scripts/run-electron.cjs`: VS Code exports `ELECTRON_RUN_AS_NODE=1`, which breaks Electron.
- Install deps with npm 11 (`npx npm@11 install`); npm 10.9 crashes resolving vitest's optional peers.
- Packaging: electron-builder NSIS, per-machine install, code-signed, bundle JRE and jar as `extraResources`. Data lives in `%APPDATA%`, never the install dir.

**UI (ui/)**
- React + TypeScript strict. TanStack Query for server state, Zustand for UI state (`app/authStore` holds the session in memory only).
- All HTTP goes through `api/client.ts` (adds both tokens; a 401 with a session signs out). Feature folders: `features/<name>/` with an `*Api.ts` and screens.
- Sign-in: first run shows `CreateOwner`; afterwards `SignIn` (user tiles + `PinPad`, which also takes keyboard digits/Enter).
- Sale screen: basket 60% left (TOTAL at the bottom); right column 50/50 readout+keypad / menu (4 x 3 grid). Behaviour: see the `pos-sale-actions` skill.
- Themes (`ui/src/app/theme.ts`, picker in the header, saved per terminal in localStorage): `light` = Default, `dark`, `glass` = Liquid Glass. A theme changes only tokens and styling, never layout or markup.
  - Add a theme: add it to `THEMES`, write `:root[data-theme="<id>"]` tokens (+ overrides) in `ui/src/themes/<id>.css`, import it in `main.tsx`.
  - Liquid Glass uses `backdrop-filter` (GPU). Keep Default for slow or old PCs; glass.css has fallbacks for no blur / reduced transparency.
- Visual style (Default): white theme default (dark optional), flat surfaces separated by 1px lines (`var(--border)`), ruled grids (1px gap over the border colour), blue (`--primary*`) only for actions. Button style follows its type (product, folder, tender, function), not per-button colours. Use the CSS tokens in `styles.css`; no new hard-coded colours.
- Chromium in this Electron returns a Promise from `scrollIntoView`; never return it from a `useEffect` arrow.
- Touch first: min 48 px targets, no hover-only actions, works offline by nature (local API).
- Display money with a single `formatMoney(minorUnits)` helper; never do arithmetic on formatted strings.
- Keep the order screen fast: optimistic add-item, no full-list rerenders.
- Test with Vitest; E2E with Playwright against the packaged Electron app.
