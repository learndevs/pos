import { useAuth } from "../app/authStore";

/**
 * Thin fetch wrapper for the local Spring Boot API.
 * The UI is served same-origin with the API (Vite proxy in dev, Spring Boot in prod), so paths are relative.
 * Two headers: the per-launch token (proves we are the Electron window) and the user's session token.
 */
let launchTokenPromise: Promise<string | undefined> | undefined;

function launchToken(): Promise<string | undefined> {
  launchTokenPromise ??= window.pos ? window.pos.getConfig().then((c) => c.apiToken) : Promise.resolve(undefined);
  return launchTokenPromise;
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const lt = await launchToken();
  if (lt) headers.set("X-POS-Launch-Token", lt);
  const session = useAuth.getState().token;
  if (session) headers.set("Authorization", `Bearer ${session}`);

  let res: Response;
  try {
    res = await fetch(path, { ...init, headers });
  } catch {
    throw new ApiError(0, "Cannot reach the POS server.");
  }
  if (!res.ok) {
    const problem = (await res.json().catch(() => undefined)) as { detail?: string } | undefined;
    if (res.status === 401 && session) useAuth.getState().signOut(); // session expired
    throw new ApiError(res.status, problem?.detail ?? `Request failed (${res.status})`);
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export const post = <T>(path: string, body: unknown) => api<T>(path, { method: "POST", body: JSON.stringify(body) });
