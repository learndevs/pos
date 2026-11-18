import { create } from "zustand";

/**
 * Hardware access for the UI. In Electron the preload bridge drives the real (or test) devices.
 * In a plain browser (dev without Electron) an in-page simulator stands in, so the flow still works.
 */
interface SimState {
  drawer?: { changeText?: string; totalText?: string; close: () => void };
}
export const useDeviceSim = create<SimState>(() => ({}));

export const devices = {
  openDrawer(info: { changeText?: string; totalText?: string }): Promise<void> {
    if (window.pos?.openDrawer) return window.pos.openDrawer(info);
    return new Promise((resolve) =>
      useDeviceSim.setState({ drawer: { ...info, close: () => (useDeviceSim.setState({ drawer: undefined }), resolve()) } }),
    );
  },

  async printReceipt(html: string, name: string): Promise<string> {
    if (window.pos?.printReceipt) return window.pos.printReceipt(html, name);
    const url = URL.createObjectURL(new Blob([html], { type: "text/html" }));
    window.open(url, "_blank", "width=420,height=720");
    return url;
  },
};
