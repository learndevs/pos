import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { create } from "zustand";
import { usePosDispatch } from "./actions";
import { saleApi } from "./saleApi";
import { useSaleData } from "./useSaleData";
import { SearchIcon } from "./icons";

export const useProductSearch = create<{ isOpen: boolean; open(): void; close(): void }>((set) => ({
  isOpen: false,
  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
}));

/** Search by name, SKU or barcode; tap a result to add it (keypad quantity applies, as with buttons). */
export function ProductSearch() {
  const { isOpen, close } = useProductSearch();
  const { money } = useSaleData();
  const { dispatch } = usePosDispatch();
  const [q, setQ] = useState("");
  const [term, setTerm] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setTerm(q.trim()), 200); // debounce typing
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    if (!isOpen) setQ("");
  }, [isOpen]);

  const results = useQuery({
    queryKey: ["product-search", term],
    queryFn: () => saleApi.searchProducts(term),
    enabled: isOpen && term.length > 0,
  });

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && close()}>
      <div className="modal search" role="dialog" aria-label="Product search">
        <div className="search-field">
          <SearchIcon />
          <input
            autoFocus
            placeholder="Search products by name, SKU or barcode"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Escape") close();
              if (e.key === "Enter" && results.data?.[0]) {
                void dispatch({ type: "product", productId: results.data[0].id });
                close();
              }
            }}
          />
          <button type="button" className="nav-btn" onClick={close}>Close</button>
        </div>
        <div className="search-results">
          {term === "" && <p className="muted pad">Type to search. Enter adds the first result.</p>}
          {results.isFetching && term !== "" && !results.data && <p className="muted pad">Searching...</p>}
          {results.data?.length === 0 && <p className="muted pad">No products match "{term}".</p>}
          {results.data?.map((p) => (
            <button
              key={p.id}
              type="button"
              className="search-row"
              onClick={() => {
                void dispatch({ type: "product", productId: p.id });
                close();
              }}
            >
              <span className="search-name">
                {p.name}
                <small className="muted">{p.category}{p.sku ? ` · ${p.sku}` : ""}</small>
              </span>
              <span className="num">{money(p.priceMinor)}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
