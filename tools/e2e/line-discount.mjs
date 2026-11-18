// Line discount flow (docs/features/line-discount.md). Requires `test-stack.sh up`.
import { app, sleep, window } from "./cdp.mjs";

const pos = await app();
await sleep(2000);
console.log("sign in:", await pos.signIn());
console.log("fresh sale:", await pos.freshSale());

await pos.keys("2"); await pos.click("Burger"); await sleep(500);
await pos.clickSelector(".line"); await sleep(300);                 // select line 1 -> ITEM menu
console.log("menu:", (await pos.state()).menu);
console.log("Line discount:", await pos.click("Line discount")); await sleep(200);
console.log("prompt:", (await pos.state()).prompt);
await pos.keys("10"); await pos.key("Enter"); await sleep(600);
const s = await pos.state();
console.log("after 10%:", { total: s.total, message: s.message, line: s.lines[0] });
await pos.shot("discount-1-basket");

await pos.click("Done"); await sleep(200);
await pos.clickSelector(".total-button"); await sleep(300);
console.log("Cash exact:", await pos.click("Cash"));
const drawer = await window("Cash Drawer");
await sleep(400);
await drawer.evaluate("document.getElementById('close').click()");
await sleep(3000);
try { const printer = await window("Test Printer", 5000); await printer.shot("discount-2-receipt"); } catch { console.log("printer window not captured"); }
console.log("after:", (await pos.state()).message);
process.exit(0);
