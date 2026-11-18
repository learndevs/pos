import { defineAction } from "./registry";
import { msg } from "./types";

/** TENDER menu: payments. Amount = button preset, else keypad value, else exact amount due. */
defineAction({
  id: "TENDER",
  needs: { sale: true, items: true },
  async run({ button, input, sale, api, money }) {
    const after = await api.tender(sale!.id, button!.tenderMethod!, button!.amountMinor ?? input);
    if (after.status === "COMPLETED") {
      return [
        { type: "sale", sale: after },
        { type: "select" },
        { type: "menu", menu: "SALE" },
        msg("success", after.changeMinor > 0 ? `Change ${money(after.changeMinor)}` : `Paid. Receipt ${after.receiptNo}`),
      ];
    }
    return [{ type: "sale", sale: after }, msg("info", `Still due ${money(after.dueMinor)}`)];
  },
});
