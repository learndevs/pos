/** Inline line icons (no CDN). currentColor, 16px default. */
const base = (size = 16) => ({ width: size, height: size, viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round" as const, strokeLinejoin: "round" as const, "aria-hidden": true });

export const EyeIcon = () => <svg {...base()}><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" /><circle cx="12" cy="12" r="3" /></svg>;
export const EditIcon = () => <svg {...base()}><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" /></svg>;
export const TrashIcon = () => <svg {...base()}><path d="M3 6h18" /><path d="M8 6V4h8v2" /><path d="M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" /></svg>;
export const SearchIcon = () => <svg {...base(18)}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>;
export const PlusIcon = () => <svg {...base()}><path d="M12 5v14M5 12h14" /></svg>;
export const CheckIcon = () => <svg {...base()}><path d="M20 6 9 17l-5-5" /></svg>;
export const XIcon = () => <svg {...base()}><path d="M18 6 6 18M6 6l12 12" /></svg>;
export const HomeIcon = () => <svg {...base(18)}><path d="M3 11 12 4l9 7" /><path d="M5 10v10h14V10" /></svg>;
export const BoxIcon = () => <svg {...base(18)}><path d="M21 8 12 3 3 8v8l9 5 9-5z" /><path d="M3 8l9 5 9-5M12 13v8" /></svg>;
export const TagIcon = () => <svg {...base(18)}><path d="M20 12 12 20 4 12V4h8z" /><circle cx="8.5" cy="8.5" r="1.5" /></svg>;
export const PercentIcon = () => <svg {...base(18)}><path d="M19 5 5 19" /><circle cx="6.5" cy="6.5" r="2.5" /><circle cx="17.5" cy="17.5" r="2.5" /></svg>;
export const GridIcon = () => <svg {...base(18)}><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>;
export const SendIcon = () => <svg {...base(18)}><path d="M22 2 11 13" /><path d="M22 2 15 22l-4-9-9-4z" /></svg>;
export const MonitorIcon = () => <svg {...base(18)}><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></svg>;
export const ReceiptIcon = () => <svg {...base(18)}><path d="M6 2h12v20l-3-2-3 2-3-2-3 2z" /><path d="M9 7h6M9 11h6M9 15h4" /></svg>;
