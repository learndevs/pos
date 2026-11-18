import { Basket } from "./Basket";
import { DialogPanel } from "./DialogPanel";
import { MenuBar } from "./MenuBar";
import { SaleHeader } from "./SaleHeader";
import { TotalButton } from "./TotalButton";

/**
 * ┌ header ─────────────────────────────────────┐
 * │ basket (60%)              │ dialog + keypad │
 * │                           ├─────────────────┤
 * │ TOTAL                     │ menu (3 x 4)    │
 * └───────────────────────────┴─────────────────┘
 */
export function SaleScreen() {
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
    </div>
  );
}
