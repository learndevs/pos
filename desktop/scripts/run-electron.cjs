// Launch Electron with a clean environment. Editors like VS Code export ELECTRON_RUN_AS_NODE=1,
// which makes Electron behave like plain Node (require("electron") is undefined).
const { spawn } = require("node:child_process");
const electron = require("electron"); // path to the binary when required from Node

const env = { ...process.env };
delete env.ELECTRON_RUN_AS_NODE;
if (process.argv.includes("--dev")) env.POS_DEV = "1";
if (process.argv.includes("--admin")) env.POS_MODE = "admin";

const args = ["."].concat(process.argv.slice(2).filter((a) => a !== "--dev" && a !== "--admin"));
const child = spawn(electron, args, { stdio: "inherit", env, cwd: __dirname + "/.." });
child.on("exit", (code) => process.exit(code ?? 0));
