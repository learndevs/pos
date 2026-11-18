import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { api, useSession } from "./api";
import { Auth } from "./pages/Auth";
import { Categories, Products, TaxRates } from "./pages/Catalog";
import { Dashboard } from "./pages/Dashboard";
import { Menus } from "./pages/Menus";
import { Packages } from "./pages/Packages";
import { Terminals } from "./pages/Terminals";
import { Transactions } from "./pages/Transactions";
import { BoxIcon, GridIcon, HomeIcon, MonitorIcon, PercentIcon, ReceiptIcon, SearchIcon, SendIcon, TagIcon } from "./icons";

type PageKey = "dashboard" | "products" | "categories" | "taxRates" | "menus" | "packages" | "terminals" | "transactions";
interface Feature { key: PageKey; title: string; category: string; description: string; keywords: string; icon: ReactNode }

/** Every configuration feature: title, category (side-nav group) and search keywords. */
const FEATURES: Feature[] = [
  { key: "dashboard", title: "Dashboard", category: "Overview", description: "Broadcast changes to tills", keywords: "home broadcast sync status", icon: <HomeIcon /> },
  { key: "products", title: "Products", category: "Catalog", description: "Items, prices, tax, stock", keywords: "item price sku barcode merchandise charge", icon: <BoxIcon /> },
  { key: "categories", title: "Categories", category: "Catalog", description: "Group products", keywords: "group section", icon: <TagIcon /> },
  { key: "taxRates", title: "Tax rates", category: "Catalog", description: "VAT and other taxes", keywords: "vat gst tax inclusive", icon: <PercentIcon /> },
  { key: "menus", title: "Menus", category: "Till layout", description: "Sale, tender and item buttons", keywords: "buttons grid folder layout sale tender item keypad", icon: <GridIcon /> },
  { key: "packages", title: "Packages", category: "Sync", description: "Build and broadcast packages", keywords: "zip broadcast sync date range snapshot", icon: <SendIcon /> },
  { key: "terminals", title: "Terminals", category: "Sync", description: "Tills and their tokens", keywords: "till device token register", icon: <MonitorIcon /> },
  { key: "transactions", title: "Transactions", category: "Reports", description: "Archive of till sales", keywords: "sales receipts archive report cancelled", icon: <ReceiptIcon /> },
];
const CATEGORIES = [...new Set(FEATURES.map((f) => f.category))];

export function App() {
  const { user, clear } = useSession();
  const [page, setPage] = useState<PageKey>("dashboard");
  if (!user) return <Auth />;
  const feature = FEATURES.find((f) => f.key === page)!;
  const go = (k: string) => setPage(k as PageKey);
  return (
    <div className="shell">
      <aside className="nav">
        <div className="brand">POS <span>Admin</span></div>
        {CATEGORIES.map((cat) => (
          <div key={cat} className="nav-group">
            <div className="nav-title">{cat}</div>
            {FEATURES.filter((f) => f.category === cat).map((f) => (
              <button key={f.key} className={page === f.key ? "active" : ""} onClick={() => setPage(f.key)}>{f.icon}<span>{f.title}</span></button>
            ))}
          </div>
        ))}
      </aside>
      <div className="frame">
        <header className="topbar">
          <div className="crumb"><span className="muted">{feature.category}</span><span className="sep">/</span><strong>{feature.title}</strong></div>
          <FeatureSearch onOpen={setPage} />
          <div className="user">
            <span className="avatar">{user.displayName.slice(0, 1).toUpperCase()}</span>
            <span className="who">{user.displayName}</span>
            <button className="link" onClick={async () => { await api.post("/admin/api/logout").catch(() => undefined); clear(); }}>Sign out</button>
          </div>
        </header>
        <main className="main" key={page}>
          {page === "dashboard" && <Dashboard go={go} />}
          {page === "products" && <Products />}
          {page === "categories" && <Categories />}
          {page === "taxRates" && <TaxRates />}
          {page === "menus" && <Menus />}
          {page === "packages" && <Packages />}
          {page === "terminals" && <Terminals />}
          {page === "transactions" && <Transactions />}
        </main>
      </div>
    </div>
  );
}

/** Header search: type to find a configuration feature; results drop down inline. Ctrl/Cmd+K focuses it. */
function FeatureSearch({ onOpen }: { onOpen(k: PageKey): void }) {
  const [q, setQ] = useState("");
  const [focused, setFocused] = useState(false);
  const [active, setActive] = useState(0);
  const input = useRef<HTMLInputElement>(null);
  const results = useMemo(() => {
    const t = q.trim().toLowerCase();
    if (!t) return FEATURES;
    // title matches first, then category / description / keywords
    const score = (f: Feature) => f.title.toLowerCase().startsWith(t) ? 0 : f.title.toLowerCase().includes(t) ? 1
      : `${f.category} ${f.description} ${f.keywords}`.toLowerCase().includes(t) ? 2 : 9;
    return FEATURES.filter((f) => score(f) < 9).sort((a, b) => score(a) - score(b));
  }, [q]);
  useEffect(() => setActive(0), [q]);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") { e.preventDefault(); input.current?.focus(); } };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  const open = (f?: Feature) => { if (!f) return; onOpen(f.key); setQ(""); input.current?.blur(); };
  return (
    <div className="feature-search">
      <SearchIcon />
      <input ref={input} placeholder="Search settings (Ctrl K)" value={q} aria-label="Search configuration features"
        onFocus={() => setFocused(true)} onBlur={() => setTimeout(() => setFocused(false), 120)} onChange={(e) => setQ(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") { e.preventDefault(); setActive(Math.min(active + 1, results.length - 1)); }
          if (e.key === "ArrowUp") { e.preventDefault(); setActive(Math.max(active - 1, 0)); }
          if (e.key === "Enter") open(results[active]);
          if (e.key === "Escape") { setQ(""); input.current?.blur(); }
        }} />
      {focused && (
        <div className="search-results" role="listbox">
          {results.length === 0 && <div className="muted small pad">No feature matches "{q}".</div>}
          {results.map((f, i) => (
            <button key={f.key} role="option" aria-selected={i === active} className={i === active ? "active" : ""}
              onMouseEnter={() => setActive(i)} onMouseDown={(e) => e.preventDefault()} onClick={() => open(f)}>
              <span className="res-icon">{f.icon}</span>
              <span className="res-text"><strong>{f.title}</strong><span className="muted small">{f.category} · {f.description}</span></span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
