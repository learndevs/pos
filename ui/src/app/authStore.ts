import { create } from "zustand";

export interface SessionUser {
  id: string;
  username: string;
  displayName: string;
  role: string;
  permissions: string[];
}

interface AuthState {
  token?: string;
  user?: SessionUser;
  signIn(token: string, user: SessionUser): void;
  signOut(): void;
}

/** Session lives in memory only: closing the app or restarting the server signs the user out. */
export const useAuth = create<AuthState>((set) => ({
  signIn: (token, user) => set({ token, user }),
  signOut: () => set({ token: undefined, user: undefined }),
}));
