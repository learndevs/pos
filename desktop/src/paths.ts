import { app } from "electron";
import fs from "node:fs";
import path from "node:path";

/**
 * One Electron codebase, two products:
 *   till   = Retail POS (sale screen; server.jar on a random loopback port)
 *   admin  = Retail POS Admin Server (admin.jar on the shop network, port 8081)
 * Packaged builds bake the mode into package.json (electron-builder extraMetadata.posMode); dev uses POS_MODE.
 */
export type Mode = "till" | "admin";

export function mode(): Mode {
  if (process.env.POS_MODE === "admin" || process.env.POS_MODE === "till") return process.env.POS_MODE;
  try {
    const pkg = JSON.parse(fs.readFileSync(path.join(app.getAppPath(), "package.json"), "utf8"));
    return pkg.posMode === "admin" ? "admin" : "till";
  } catch {
    return "till";
  }
}

/** Bundled Java runtime: <install>/resources in production, the repo in dev. */
export function resourcesDir(): string {
  return app.isPackaged ? process.resourcesPath : path.resolve(__dirname, "..", "resources");
}

const platformDir = () => `${process.platform === "win32" ? "win" : process.platform === "darwin" ? "mac" : "linux"}-${process.arch}`;

export function jar(m: Mode): string {
  const file = m === "admin" ? "admin.jar" : "server.jar";
  return app.isPackaged
    ? path.join(resourcesDir(), "server", file)
    : path.resolve(__dirname, "..", "..", "server", m === "admin" ? "admin-app" : "app", "build", "libs", file);
}

/** Bundled jlink runtime in production; JAVA_HOME / java on PATH in dev. */
export function javaExecutable(): string {
  const bin = process.platform === "win32" ? "java.exe" : "java";
  if (app.isPackaged) return path.join(resourcesDir(), "jre", "bin", bin);
  const dev = path.join(resourcesDir(), "jre", platformDir(), "bin", bin);
  if (process.env.POS_USE_BUNDLED && fs.existsSync(dev)) return dev;
  return process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, "bin", bin) : "java";
}

/** Dev: run from server/ so server/config/application-*.properties is picked up. */
export function serverWorkDir(): string | undefined {
  return app.isPackaged ? undefined : path.resolve(__dirname, "..", "..", "server");
}

export function dataDir(): string {
  return app.getPath("userData");
}

export function logDir(): string {
  return path.join(dataDir(), "logs");
}
