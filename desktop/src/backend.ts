import { ChildProcess, spawn } from "node:child_process";
import { randomBytes } from "node:crypto";
import fs from "node:fs";
import net from "node:net";
import path from "node:path";
import { dataDir, javaExecutable, jar, logDir, serverWorkDir, type Mode } from "./paths";

export interface Backend {
  baseUrl: string;
  token: string;
  stop(): Promise<void>;
}

/** Admin server settings (<userData>/admin-server.json): port and whether other PCs on the shop network may connect. */
interface AdminServerConfig { port: number; lan: boolean }
function adminConfig(): AdminServerConfig {
  const file = path.join(dataDir(), "admin-server.json");
  const defaults: AdminServerConfig = { port: 8081, lan: true };
  try {
    return { ...defaults, ...JSON.parse(fs.readFileSync(file, "utf8")) };
  } catch {
    fs.mkdirSync(dataDir(), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(defaults, null, 2));
    return defaults;
  }
}

/**
 * Database connection (<userData>/database.json). PostgreSQL is installed separately (Windows service,
 * another PC, or Docker) - the installer only ships the app, Java and the JDBC driver inside the jar.
 * First run writes a template and stops with a message saying where to fill it in.
 */
interface DatabaseConfig { url: string; user: string; password: string }
function databaseConfig(m: Mode): DatabaseConfig | undefined {
  const file = path.join(dataDir(), "database.json");
  if (fs.existsSync(file)) {
    const c = JSON.parse(fs.readFileSync(file, "utf8")) as Partial<DatabaseConfig>;
    if (c.url && c.user && c.password) return c as DatabaseConfig;
    throw new Error(`Database settings are incomplete: fill in url, user and password in\n${file}`);
  }
  if (serverWorkDir()) return undefined;            // dev: server/config/application-local.properties
  fs.mkdirSync(dataDir(), { recursive: true });
  const template: DatabaseConfig = {
    url: `jdbc:postgresql://127.0.0.1:5432/${m === "admin" ? "pos_admin" : "pos"}`,
    user: "pos_app",
    password: "",
  };
  fs.writeFileSync(file, JSON.stringify(template, null, 2), { mode: 0o600 });
  throw new Error(`No database configured yet.\nEdit ${file}\n(url, user, password of your PostgreSQL server), then start again.`);
}

/** Fail fast with a clear message when PostgreSQL is not running / not reachable. */
function checkReachable(url: string): Promise<void> {
  const m = /^jdbc:postgresql:\/\/([^:/?]+)(?::(\d+))?/.exec(url);
  if (!m) return Promise.reject(new Error(`Not a PostgreSQL JDBC url: ${url}`));
  const [host, port] = [m[1], Number(m[2] ?? 5432)];
  return new Promise((resolve, reject) => {
    const sock = net.connect({ host, port, timeout: 5000 }, () => { sock.end(); resolve(); });
    const fail = () => { sock.destroy(); reject(new Error(`PostgreSQL is not reachable at ${host}:${port}. Is the database service running?`)); };
    sock.on("error", fail);
    sock.on("timeout", fail);
  });
}

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
    if (child && child.exitCode !== null) throw new Error(`server exited early with code ${child.exitCode} (see ${logDir()})`);
    try {
      if ((await fetch(`${baseUrl}/actuator/health`)).ok) return;
    } catch {
      // not up yet
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`server not healthy after ${timeoutMs / 1000}s`);
}

/**
 * Dev attach (POS_SERVER_URL): use an already running server.
 * Otherwise: read the database connection, start the Java server for this mode, and wait for health.
 */
export async function startBackend(m: Mode): Promise<Backend> {
  const token = randomBytes(32).toString("hex");
  const external = process.env.POS_SERVER_URL;
  if (external) {
    await waitForHealth(external, 60_000);
    return { baseUrl: external, token, stop: async () => {} };
  }

  fs.mkdirSync(logDir(), { recursive: true });
  const db = databaseConfig(m);
  if (db) await checkReachable(db.url);

  const admin = m === "admin" ? adminConfig() : undefined;
  const port = admin ? admin.port : await freePort();
  const env: NodeJS.ProcessEnv = { ...process.env };
  if (db) Object.assign(env, m === "admin"
    ? { ADMIN_DB_URL: db.url, ADMIN_DB_USER: db.user, ADMIN_DB_PASSWORD: db.password }
    : { DB_URL: db.url, DB_USER: db.user, DB_PASSWORD: db.password });
  if (m === "admin") Object.assign(env, { ADMIN_PORT: String(port), ADMIN_ADDRESS: admin!.lan ? "0.0.0.0" : "127.0.0.1" });
  else Object.assign(env, { SERVER_PORT: String(port), POS_LAUNCH_TOKEN: token });
  // dev without database.json: server/config/application-local.properties (DB credentials)
  if (serverWorkDir() && m === "till") env.SPRING_PROFILES_ACTIVE ??= "local";
  if (serverWorkDir() && m === "admin") env.SPRING_PROFILES_ACTIVE ??= "admin-local";

  const log = fs.openSync(path.join(logDir(), `${m === "admin" ? "admin" : "server"}.log`), "a");
  const child = spawn(javaExecutable(), ["-Xmx256m", "-XX:+UseSerialGC", "-jar", jar(m)], {
    env, cwd: serverWorkDir(), stdio: ["ignore", log, log], windowsHide: true,
  });
  const baseUrl = `http://127.0.0.1:${port}`;
  try {
    await waitForHealth(baseUrl, 120_000, child);
  } catch (e) {
    child.kill();
    throw e;
  }

  return {
    baseUrl,
    token,
    stop: async () => {
      if (child.exitCode === null) {
        await new Promise<void>((resolve) => {
          child.once("exit", () => resolve());
          child.kill();                                    // SIGTERM -> Spring graceful shutdown
          setTimeout(() => child.kill("SIGKILL"), 10_000).unref();
        });
      }
    },
  };
}
