// electron-builder beforePack hook: refuse to package an installer without its bundled Java runtime (PostgreSQL is installed separately).
const fs = require("node:fs");
const path = require("node:path");

const ARCH = { 0: "ia32", 1: "x64", 2: "armv7l", 3: "arm64", 4: "universal" };

exports.default = async function (context) {
  const os = context.electronPlatformName === "win32" ? "win" : context.electronPlatformName === "darwin" ? "mac" : "linux";
  const target = `${os}-${ARCH[context.arch] ?? context.arch}`;
  const res = path.resolve(__dirname, "..", "..", "desktop", "resources");
  const missing = [["jre", "bin"]]
    .map(([dir, sub]) => path.join(res, dir, target, sub))
    .filter((p) => !fs.existsSync(p));
  if (missing.length) {
    throw new Error(`Runtimes for ${target} are missing:\n  ${missing.join("\n  ")}\nRun: bash packaging/runtime/prepare.sh ${target}`);
  }
  console.log(`  • runtimes ok for ${target}`);
};
