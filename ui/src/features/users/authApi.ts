import { api, post } from "../../api/client";
import type { SessionUser } from "../../app/authStore";

export interface UserTile {
  id: string;
  username: string;
  displayName: string;
}

export const authApi = {
  setupStatus: () => api<{ needsOwner: boolean }>("/api/setup/status"),
  createOwner: (body: { username: string; displayName: string; pin: string }) => post<SessionUser>("/api/setup/owner", body),
  users: () => api<UserTile[]>("/api/auth/users"),
  login: (username: string, pin: string) => post<{ token: string; user: SessionUser }>("/api/auth/login", { username, pin }),
  logout: () => api<void>("/api/auth/logout", { method: "POST" }),
};
