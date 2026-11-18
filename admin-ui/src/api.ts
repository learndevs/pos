import { create } from "zustand";

export interface AdminUser { id: string; username: string; displayName: string }

export const useSession = create<{ token?: string; user?: AdminUser; set(token: string, user: AdminUser): void; clear(): void }>((set) => ({
  set: (token, user) => set({ token, user }),
  clear: () => set({ token: undefined, user: undefined }),
}));

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const token = useSession.getState().token;
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let res: Response;
  try {
    res = await fetch(path, { ...init, headers });
  } catch {
    throw new ApiError(0, "Cannot reach the admin server.");
  }
  if (!res.ok) {
    const p = (await res.json().catch(() => undefined)) as { detail?: string; errors?: unknown } | undefined;
    if (res.status === 401 && token) useSession.getState().clear();
    throw new ApiError(res.status, p?.detail ?? `Request failed (${res.status})`);
  }
  return res;
}

export const api = {
  get: async <T,>(path: string) => (await request(path)).json() as Promise<T>,
  post: async <T,>(path: string, body?: unknown) => {
    const r = await request(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) });
    return (r.status === 204 ? undefined : await r.json()) as T;
  },
  put: async <T,>(path: string, body: unknown) => (await request(path, { method: "PUT", body: JSON.stringify(body) })).json() as Promise<T>,
  /** Authenticated file download (e.g. package zip). */
  download: async (path: string, filename: string) => {
    const blob = await (await request(path)).blob();
    const url = URL.createObjectURL(blob);
    const a = Object.assign(document.createElement("a"), { href: url, download: filename });
    a.click();
    URL.revokeObjectURL(url);
  },
};

// ---- types (mirror server/modules/sync-contract) ----
export type Entity = "TAX_RATE" | "CATEGORY" | "PRODUCT";
export interface TaxRate { id: string; name: string; rateBp: number; inclusive: boolean; active: boolean }
export interface Category { id: string; name: string; sortOrder: number; active: boolean }
export interface Product {
  id: string; categoryId: string; taxRateId: string | null; name: string; itemClass: "MERCHANDISE" | "NON_MERCHANDISE";
  nonMerchType: string | null; sku: string | null; barcode: string | null; priceMinor: number; kitchenPrint: boolean;
  trackStock: boolean; sortOrder: number; active: boolean;
}
export interface Terminal { id: string; name: string; active: boolean; lastSeenAt: string | null; lastAppliedPackage: number | null }
export interface PackageTarget { terminalId: string; terminalName: string; status: "PENDING" | "DOWNLOADED" | "APPLIED" | "FAILED"; error: string | null; updatedAt: string }
export interface Package {
  packageNo: number; createdAt: string; scope: "FULL" | "DATE_RANGE"; dateFrom: string | null; dateTo: string | null;
  entities: Entity[]; counts: Record<string, number>; sizeBytes: number; sha256: string; note: string | null;
  targets: PackageTarget[]; warnings: string[];
}
export interface TxnSummary { id: string; terminalId: string; txnNo: number; receiptNo: string | null; status: string; businessDate: string; totalMinor: number; finishedAt: string | null; receivedAt: string }
export interface TxnLine { lineNo: number; kind: "SALE" | "TENDER"; name: string | null; quantity: number | null; unitPriceMinor: number | null; discountMinor: number | null; discountPercent: number | null; taxMinor: number | null; amountMinor: number; tenderMethod: string | null; tenderedMinor: number | null; changeMinor: number | null }
export interface Txn { id: string; txnNo: number; receiptNo: string | null; type: string; status: string; orderType: string; businessDate: string; createdAt: string; completedAt: string | null; cancelledAt: string | null; reason: string | null; cashierName: string | null; subtotalMinor: number; taxMinor: number; totalMinor: number; paidMinor: number; changeMinor: number; lines: TxnLine[] }

export const money = (minor: number | null | undefined) =>
  minor == null ? "" : new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(minor / 100);
export const when = (iso: string | null | undefined) => (iso ? new Date(iso).toLocaleString() : "-");
export const ENTITY_LABEL: Record<Entity, string> = { TAX_RATE: "Tax rates", CATEGORY: "Categories", PRODUCT: "Products" };
