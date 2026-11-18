import { useState } from "react";
import { api, useSession } from "./api";
import { Auth } from "./pages/Auth";
import { Categories, Products, TaxRates } from "./pages/Catalog";
import { Packages } from "./pages/Packages";
import { Terminals } from "./pages/Terminals";
import { Transactions } from "./pages/Transactions";

const PAGES = {
  products: { label: "Products", group: "Catalog", el: <Products /> },
  categories: { label: "Categories", group: "Catalog", el: <Categories /> },
  taxRates: { label: "Tax rates", group: "Catalog", el: <TaxRates /> },
  packages: { label: "Packages", group: "Sync", el: <Packages /> },
  terminals: { label: "Terminals", group: "Sync", el: <Terminals /> },
  transactions: { label: "Transactions", group: "Reports", el: <Transactions /> },
} as const;
type PageKey = keyof typeof PAGES;

export function App() {
  const { user, clear } = useSession();
  const [page, setPage] = useState<PageKey>("products");
  if (!user) return <Auth />;
  const groups = [...new Set(Object.values(PAGES).map((p) => p.group))];
  return (
    <div className="shell">
      <aside className="nav">
        <div className="brand">POS <span>Admin</span></div>
        {groups.map((g) => (
          <div key={g} className="nav-group">
            <div className="nav-title">{g}</div>
            {(Object.keys(PAGES) as PageKey[]).filter((k) => PAGES[k].group === g).map((k) => (
              <button key={k} className={page === k ? "active" : ""} onClick={() => setPage(k)}>{PAGES[k].label}</button>
            ))}
          </div>
        ))}
        <div className="nav-foot">
          <div className="who">{user.displayName}</div>
          <button className="link" onClick={async () => { await api.post("/admin/api/logout").catch(() => undefined); clear(); }}>Sign out</button>
        </div>
      </aside>
      <main className="main">{PAGES[page].el}</main>
    </div>
  );
}
