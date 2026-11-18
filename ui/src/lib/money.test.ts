import { describe, expect, it } from "vitest";
import { formatMoney } from "./money";

describe("formatMoney", () => {
  it("formats minor units", () => {
    expect(formatMoney(1350, "USD", 2, "en-US")).toBe("$13.50");
    expect(formatMoney(-500, "USD", 2, "en-US")).toBe("-$5.00");
  });

  it("supports zero-decimal currencies", () => {
    expect(formatMoney(1500, "JPY", 0, "en-US")).toBe("¥1,500");
  });

  it("rejects fractional input", () => {
    expect(() => formatMoney(10.5)).toThrow();
  });
});
