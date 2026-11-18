# UI checks (e2e)

Drive the **real Electron app** against a throwaway test stack and capture screenshots, so a
change can be verified by running it (our main check; unit tests are kept light).

```bash
tools/e2e/test-stack.sh up       # pos_test DB reset + demo data, server :8090, Vite :5174, Electron (CDP :9333)
node tools/e2e/sale-cycle.mjs    # example flow -> tools/e2e/out/*.png
node tools/e2e/line-discount.mjs # line discount flow
tools/e2e/test-stack.sh down
```
- Never touches the dev DB `pos`. Test login: user `check`, PIN `1234` (day already open).
- Electron windows appear on screen (needed for the drawer and printer windows). Do not click in them while a script runs.
- `cdp.mjs` is the helper library: `app()`, `signIn()`, `freshSale()`, `click(label)`, `clickSelector(sel)`, `waitFor(sel)`, `key(k)`, `keys(s)`, `type(selector, text)`, `state()`, `shot(name)`, `window(titlePrefix)`.
- Start flows with `freshSale()` so a rerun does not add to a leftover open transaction.
- Anything async (search results, API responses) needs `waitFor(selector)` before clicking; fixed sleeps cause flaky runs.
- Do not pipe `test-stack.sh up` into another command (background processes keep the pipe open); redirect to a file instead.
- Quick screenshot of any page without Electron devices: `node tools/e2e/screenshot.cjs <url> <out.png>` (run with Electron, see file header).
