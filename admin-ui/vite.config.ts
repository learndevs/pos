import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev: admin UI on :5175, API proxied to admin-app (:8081). Prod: built into admin.jar, same origin.
const admin = process.env.ADMIN_SERVER_URL ?? "http://127.0.0.1:8081";

export default defineConfig({
  plugins: [react()],
  base: "./",
  server: { port: 5175, strictPort: true, proxy: { "/admin/api": admin, "/sync": admin } },
  build: { outDir: "dist", emptyOutDir: true },
});
