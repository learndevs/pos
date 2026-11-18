// Example flow: full sale with cash, drawer, receipt, next transaction. Requires `test-stack.sh up`.
import { app, sleep, window } from "./cdp.mjs";

const pos = await app();
await sleep(2000);
console.log("sign in:", await pos.signIn());
console.log("fresh sale:", await pos.freshSale());
console.log("start:", await pos.state());

await pos.click("Burger"); await sleep(400);
await pos.keys("2"); await pos.click("Cola"); await sleep(400);
await pos.click("Product search"); await sleep(300);
await pos.type(".search-field input", "brow");
console.log("search results:", await pos.waitFor(".search-row"));
console.log("pick:", await pos.clickSelector(".search-row")); await sleep(500);
console.log("basket:", await pos.state());
await pos.shot("1-basket");

await pos.clickSelector(".total-button"); await sleep(300);
await pos.click("Cash 50");
const drawer = await window("Cash Drawer");
await sleep(500);
await drawer.shot("2-drawer");
console.log("waiting:", (await pos.state()).prompt);
await drawer.evaluate("document.getElementById('close').click()");

await sleep(3000);
try { const printer = await window("Test Printer", 5000); await printer.shot("3-printer"); } catch { console.log("printer window not captured"); }
console.log("after:", await pos.state());
await pos.shot("4-next-transaction");
process.exit(0);
