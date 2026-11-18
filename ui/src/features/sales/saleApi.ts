import { api, apiText, post } from "../../api/client";

export type MenuCode = "SALE" | "TENDER" | "ITEM";
export type ActionType = "PRODUCT" | "OPEN_FOLDER" | "TENDER" | "FUNCTION" | "SWITCH_MENU";

export interface MenuButton {
  id: string;
  folderId: string;
  page: number;
  gridRow: number;
  gridCol: number;
  label: string;
  color: string | null;
  actionType: ActionType;
  productId: string | null;
  productPriceMinor: number | null;
  targetFolderId: string | null;
  tenderMethod: "CASH" | "CARD" | "OTHER" | null;
  amountMinor: number | null;
  functionCode: string | null;
  functionInput: "NONE" | "NUMBER" | "AMOUNT" | "CONFIRM" | null;
  needsSelectedLine: boolean | null;
  functionAvailable: boolean | null;
  requiredPermission: string | null;
  targetMenuCode: MenuCode | null;
}

export interface MenuConfig {
  menus: { id: string; code: MenuCode; name: string; rootFolderId: string }[];
  folders: { id: string; menuId: string; parentFolderId: string | null; name: string }[];
  buttons: MenuButton[];
}

export interface SaleLine {
  id: string;
  lineNo: number;
  kind: "SALE" | "TENDER";
  amountMinor: number;
  name: string | null;
  quantity: number | null;
  unitPriceMinor: number | null;
  itemClass: "MERCHANDISE" | "NON_MERCHANDISE" | null;
  tenderMethod: "CASH" | "CARD" | "OTHER" | null;
  tenderedMinor: number | null;
  changeMinor: number | null;
}

export interface Sale {
  id: string;
  txnNo: number;
  status: "OPEN" | "COMPLETED" | "CANCELLED";
  orderType: "DINE_IN" | "TAKEAWAY";
  receiptNo: string | null;
  businessDate: string;
  lines: SaleLine[];
  subtotalMinor: number;
  taxMinor: number;
  totalMinor: number;
  paidMinor: number;
  dueMinor: number;
  changeMinor: number;
  cashTendered: boolean;
}

export interface ProductHit {
  id: string;
  name: string;
  priceMinor: number;
  category: string;
  sku: string | null;
  barcode: string | null;
}

export interface CashSession {
  id: string;
  businessDate: string;
  openingFloat: number;
}

export interface Business {
  name: string;
  currency: string;
  minorUnitDigits: number;
}

const put = <T>(path: string, body: unknown) => api<T>(path, { method: "PUT", body: JSON.stringify(body) });

export const saleApi = {
  menus: () => api<MenuConfig>("/api/menus"),
  business: () => api<Business>("/api/settings/business"),
  cashSession: () => api<CashSession | undefined>("/api/cash/session"),
  openDay: (openingFloatMinor: number) => post<CashSession>("/api/cash/session/open", { openingFloatMinor }),
  current: () => api<Sale | undefined>("/api/sales/current"),
  start: () => post<Sale>("/api/sales", {}),
  addItem: (id: string, productId: string, quantity: number) => post<Sale>(`/api/sales/${id}/items`, { productId, quantity }),
  setQuantity: (id: string, itemId: string, quantity: number) => put<Sale>(`/api/sales/${id}/items/${itemId}/quantity`, { quantity }),
  removeLine: (id: string, itemId: string) => api<Sale>(`/api/sales/${id}/items/${itemId}`, { method: "DELETE" }),
  orderType: (id: string, orderType: Sale["orderType"]) => put<Sale>(`/api/sales/${id}/order-type`, { orderType }),
  tender: (id: string, method: string, amountMinor: number | null) =>
    post<Sale>(`/api/sales/${id}/tenders`, amountMinor == null ? { method } : { method, amountMinor }),
  cancel: (id: string, reason: string) => post<Sale>(`/api/sales/${id}/cancel`, { reason }),
  receiptHtml: (id: string) => apiText(`/api/receipts/${id}`),
  searchProducts: (q: string) => api<ProductHit[]>(`/api/products/search?q=${encodeURIComponent(q)}`),
};
