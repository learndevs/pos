import { Basket } from "./Basket";
import { DialogPanel } from "./DialogPanel";
import { MenuBar } from "./MenuBar";
import { ProductSearch } from "./ProductSearch";
import { SaleHeader } from "./SaleHeader";
import { TotalButton } from "./TotalButton";
import { useAutoTransaction } from "./useAutoTransaction";
import { DrawerSimulator } from "../../devices/DrawerSimulator";

/**
 * ┌ header: business · day · Txn # │ actions (scroll) │ theme · user · clock ┐
 * │ basket (60%)                    │ readout + keypad (50%)                   │
 * │ TOTAL                           │ menu 4 x 3 (50%)                         │
 * └─────────────────────────────────┴──────────────────────────────────────────┘
 */
export function SaleScreen() {
  useAutoTransaction();
  return (
    <div className="sale-screen">
      <SaleHeader />
      <div className="left-col">
        <Basket />
        <TotalButton />
      </div>
      <div className="right-col">
        <DialogPanel />
        <MenuBar />
      </div>
      <ProductSearch />
      <DrawerSimulator />
    </div>
  );
}
