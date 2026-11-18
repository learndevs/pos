import { ChildProcess, spawn } from "node:child_process";
import { randomBytes } from "node:crypto";
import fs from "node:fs";
import net from "node:net";
import path from "node:path";
import { javaExecutable, logDir, serverJar, serverWorkDir } from "./paths";

export interface Backend {
  baseUrl: string;
  token: string;
  stop(): Promise<void>;
}

/** Pick a free loopback port. */
function freePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const srv = net.createServer();
    srv.unref();
    srv.on("error", reject);
    srv.listen(0, "127.0.0.1", () => {
      const { port } = srv.address() as net.AddressInfo;
      srv.close(() => resolve(port));
    });
  });
}

async function waitForHealth(baseUrl: string, timeoutMs: number, child?: ChildProcess): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (child && child.exitCode !== null) throw new Error(`server exited early with code ${child.exitCode}`);
    try {
      const res = await fetch(`${baseUrl}/actuator/health`);
      if (res.ok) return;
    } catch {
      // not up yet
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`server not healthy after ${timeoutMs / 1000}s`);
}

/**
 * Dev (POS_SERVER_URL set): attach to an already running server.
 * Otherwise: spawn the server jar on a random port with a per-launch token, and wait for health.
 * TODO(phase 0): start bundled PostgreSQL first and pass DB_URL / DB_PASSWORD.
 */
export async function startBackend(): Promise<Backend> {
  const token = randomBytes(32).toString("hex");

  const external = process.env.POS_SERVER_URL;
  if (external) {
    await waitForHealth(external, 60_000);
    return { baseUrl: external, token, stop: async () => {} };
  }

  const port = await freePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  fs.mkdirSync(logDir(), { recursive: true });
  const log = fs.openSync(path.join(logDir(), "server.log"), "a");

  const child = spawn(
    javaExecutable(),
    ["-Xmx256m", "-XX:+UseSerialGC", "-jar", serverJar()],
    {
      env: {
        ...process.env,
        SERVER_PORT: String(port),
        POS_LAUNCH_TOKEN: token,
        // dev: use server/config/application-local.properties for DB credentials
        ...(serverWorkDir() ? { SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE ?? "local" } : {}),
      },
      cwd: serverWorkDir(),
      stdio: ["ignore", log, log],
      windowsHide: true,
    },
  );

  await waitForHealth(baseUrl, 90_000, child);

  return {
    baseUrl,
    token,
    stop: () =>
      new Promise<void>((resolve) => {
        if (child.exitCode !== null) return resolve();
        child.once("exit", () => resolve());
        child.kill();                                   // SIGTERM -> Spring graceful shutdown
        setTimeout(() => child.kill("SIGKILL"), 10_000).unref();
      }),
  };
}
