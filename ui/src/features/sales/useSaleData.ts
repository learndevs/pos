import { useQuery, useQueryClient } from "@tanstack/react-query";
import { formatMoney } from "../../lib/money";
import { saleApi, type Sale } from "./saleApi";

export const SALE_KEY = ["sale"] as const;

export function useSaleData() {
  const qc = useQueryClient();
  const menus = useQuery({ queryKey: ["menus"], queryFn: saleApi.menus, staleTime: Infinity });
  const business = useQuery({ queryKey: ["business"], queryFn: saleApi.business, staleTime: Infinity });
  const session = useQuery({ queryKey: ["cash-session"], queryFn: saleApi.cashSession });
  const sale = useQuery({ queryKey: SALE_KEY, queryFn: async () => (await saleApi.current()) ?? null });

  const money = (minor: number) =>
    formatMoney(minor, business.data?.currency ?? "USD", business.data?.minorUnitDigits ?? 2);

  return {
    menus: menus.data,
    business: business.data,
    session: session.data,
    sessionLoaded: session.isFetched,
    sale: sale.data ?? null,
    setSale: (s: Sale | null) => qc.setQueryData(SALE_KEY, s),
    refreshSession: () => qc.invalidateQueries({ queryKey: ["cash-session"] }),
    money,
  };
}
