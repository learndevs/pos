import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { saleApi } from "./saleApi";
import { SALE_KEY, useSaleData } from "./useSaleData";
import { useSaleScreen } from "./saleStore";

/**
 * Keeps an empty OPEN transaction ready whenever the day is open: on sign-in, after the day starts,
 * and after a sale is cancelled. (After a completed sale, finishSale starts the next one itself.)
 */
export function useAutoTransaction() {
  const qc = useQueryClient();
  const { session, sale, saleLoaded } = useSaleData();
  const busy = useSaleScreen((s) => s.mode.kind === "BUSY");
  const starting = useRef(false);

  useEffect(() => {
    if (!session || !saleLoaded || busy || starting.current) return;
    if (sale && sale.status === "OPEN") return;
    starting.current = true;
    saleApi
      .start()
      .then((s) => qc.setQueryData(SALE_KEY, s))
      .catch(() => undefined)
      .finally(() => (starting.current = false));
  }, [session, sale, saleLoaded, busy, qc]);
}
