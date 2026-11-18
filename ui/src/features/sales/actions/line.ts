import { defineAction } from "./registry";
import { msg } from "./types";

/** ITEM menu: actions on the selected basket line. */

defineAction({
  id: "QTY",
  needs: { line: "SALE" },
  input: { kind: "number", prompt: ({ line }) => `New quantity for ${line?.name}` },
  async run({ sale, line, input, api }) {
    return [{ type: "sale", sale: await api.setQuantity(sale!.id, line!.id, input!) }, msg("success", `${line!.name}: quantity ${input}`)];
  },
});

defineAction({
  id: "QTY_PLUS",
  needs: { line: "SALE" },
  async run({ sale, line, api }) {
    return [{ type: "sale", sale: await api.setQuantity(sale!.id, line!.id, Number(line!.quantity) + 1) }];
  },
});

defineAction({
  id: "QTY_MINUS",
  needs: { line: "SALE" },
  async run({ sale, line, api }) {
    const q = Number(line!.quantity);
    if (q <= 1) return [msg("error", "Quantity is 1. Use Void line to remove it.")];
    return [{ type: "sale", sale: await api.setQuantity(sale!.id, line!.id, q - 1) }];
  },
});

defineAction({
  id: "VOID_LINE",
  needs: { line: "ANY" },
  confirm: ({ line }) => `Void line ${line?.lineNo} (${line?.name ?? line?.tenderMethod})?`,
  async run({ sale, line, api }) {
    const after = await api.removeLine(sale!.id, line!.id);
    return [{ type: "sale", sale: after }, { type: "select" }, msg("info", `Line ${line!.lineNo} voided`)];
  },
});
