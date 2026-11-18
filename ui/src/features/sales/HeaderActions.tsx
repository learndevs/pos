import { useSaleScreen } from "./saleStore";
import { useProductSearch } from "./ProductSearch";
import { MonitorIcon, SearchIcon, SettingsIcon, UsersIcon } from "./icons";
import { useDevicePanel } from "./DevicePanel";

/**
 * Header action panel. Scrolls horizontally when there is not enough room.
 * Product search works; CRM and Admin are placeholders for now.
 */
export function HeaderActions() {
  const say = useSaleScreen((s) => s.say);
  const openSearch = useProductSearch((s) => s.open);
  const openDevice = useDevicePanel((s) => s.open);
  const soon = (what: string) => say({ tone: "info", text: `${what} is coming soon.` });

  return (
    <nav className="header-actions" aria-label="Actions">
      <button type="button" className="action-btn" onClick={openSearch}>
        <SearchIcon />
        <span>Product search</span>
      </button>
      <button type="button" className="action-btn" onClick={() => soon("CRM")}>
        <UsersIcon />
        <span>CRM</span>
      </button>
      <button type="button" className="action-btn" onClick={openDevice}>
        <MonitorIcon />
        <span>Device</span>
      </button>
      <button type="button" className="action-btn" onClick={() => soon("Admin")}>
        <SettingsIcon />
        <span>Admin</span>
      </button>
    </nav>
  );
}
