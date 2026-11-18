import { app } from "electron";
import path from "node:path";

/** Where bundled runtimes live: <install>/resources in production, the repo in dev. */
export function resourcesDir(): string {
  return app.isPackaged ? process.resourcesPath : path.resolve(__dirname, "..", "resources");
}

export function serverJar(): string {
  return app.isPackaged
    ? path.join(resourcesDir(), "server", "server.jar")
    : path.resolve(__dirname, "..", "..", "server", "app", "build", "libs", "server.jar");
}

/** Bundled jlink runtime in production; `java` on PATH in dev. */
export function javaExecutable(): string {
  if (!app.isPackaged) return process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, "bin", "java") : "java";
  return path.join(resourcesDir(), "jre", "bin", process.platform === "win32" ? "java.exe" : "java");
}

/** Dev: run from server/ so server/config/application.properties (DB creds) is picked up. */
export function serverWorkDir(): string | undefined {
  return app.isPackaged ? undefined : path.resolve(__dirname, "..", "..", "server");
}

export function logDir(): string {
  return path.join(app.getPath("userData"), "logs");
}
