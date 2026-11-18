import { useDeviceSim } from "./devices";

/** In-page stand-in for the Electron "Cash Drawer (test)" window when running in a plain browser. */
export function DrawerSimulator() {
  const drawer = useDeviceSim((s) => s.drawer);
  if (!drawer) return null;
  return (
    <div className="modal-backdrop">
      <div className="modal drawer-sim" role="dialog" aria-label="Cash drawer">
        <span className="state-pill">Drawer open</span>
        {drawer.changeText && (
          <>
            <div className="muted">Change to give</div>
            <div className="big">{drawer.changeText}</div>
          </>
        )}
        {drawer.totalText && <div className="muted">Sale total {drawer.totalText}</div>}
        <button type="button" className="btn-primary wide" onClick={drawer.close} autoFocus>
          Close drawer
        </button>
      </div>
    </div>
  );
}
