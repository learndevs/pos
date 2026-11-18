/** Money is always integer minor units (see pos-domain skill). Format only at the edge.
 *  en-US formatting matches the printed receipt (server uses the same), e.g. $13.50. */
export function formatMoney(minorUnits: number, currency = "USD", minorDigits = 2, locale = "en-US"): string {
  if (!Number.isSafeInteger(minorUnits)) throw new Error(`money must be an integer of minor units, got ${minorUnits}`);
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    minimumFractionDigits: minorDigits,
    maximumFractionDigits: minorDigits,
  }).format(minorUnits / 10 ** minorDigits);
}
