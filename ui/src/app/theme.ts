import { create } from "zustand";

/**
 * Themes change only the visual layer (CSS tokens + optional component overrides), never the layout.
 * To add one: add it here, add `:root[data-theme="<id>"]` tokens (and overrides) in src/themes/<id>.css,
 * and import that file in main.tsx.
 */
export const THEMES = [
  { id: "light", label: "Default", hint: "Clean white, lines and blue. Fastest on older PCs." },
  { id: "dark", label: "Dark", hint: "Default layout on dark surfaces." },
  { id: "glass", label: "Liquid Glass", hint: "Frosted translucent panels and glossy buttons. Needs a GPU." },
] as const;

export type Theme = (typeof THEMES)[number]["id"];
const KEY = "pos.theme";

function initial(): Theme {
  try {
    const saved = localStorage.getItem(KEY);
    return THEMES.some((t) => t.id === saved) ? (saved as Theme) : "light";
  } catch {
    return "light";
  }
}

function apply(theme: Theme) {
  document.documentElement.dataset.theme = theme;
}

export const useTheme = create<{ theme: Theme; setTheme(theme: Theme): void }>((set) => {
  const theme = initial();
  apply(theme);
  return {
    theme,
    setTheme: (next) => {
      apply(next);
      try {
        localStorage.setItem(KEY, next); // per-terminal preference
      } catch {
        // storage unavailable: theme still applies for this session
      }
      set({ theme: next });
    },
  };
});
