import { defineAction } from "./registry";
import { msg } from "./types";

/** Building the sale: products, order type, cancel, opening the day. */

defineAction({
  id: "PRODUCT",
  async run({ button, productId, input, ensureSale, api }) {
    const sale = await ensureSale();
    const after = await api.addItem(sale.id, productId ?? button!.productId!, input ?? 1);
    return [{ type: "sale", sale: after }];
  },
});

defineAction({
  id: "TAKEAWAY_TOGGLE",
  async run({ ensureSale, api }) {
    const sale = await ensureSale();
    const next = sale.orderType === "TAKEAWAY" ? "DINE_IN" : "TAKEAWAY";
    return [{ type: "sale", sale: await api.orderType(sale.id, next) }, msg("info", next === "DINE_IN" ? "Dine in" : "Takeaway")];
  },
});

defineAction({
  id: "CANCEL_TXN",
  needs: { sale: true },
  confirm: () => "Cancel the whole sale?",
  async run({ sale, api }) {
    await api.cancel(sale!.id, "Cancelled at till");
    // a new empty transaction starts automatically (useAutoTransaction)
    return [{ type: "sale", sale: null }, { type: "menu", menu: "SALE" }, msg("info", `Transaction #${sale!.txnNo} cancelled`)];
  },
});

/** ENTER while the day is not open: the keypad value is the opening float. */
defineAction({
  id: "OPEN_DAY",
  needs: { day: false },
  async run({ input, api, money }) {
    await api.openDay(input ?? 0);
    return [{ type: "refreshDay" }, msg("success", `Day started with float ${money(input ?? 0)}`)];
  },
});
