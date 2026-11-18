import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Dev: Vite serves the UI on :5173 and proxies the API to the Spring Boot dev server.
// Prod: the built UI (dist/) is packaged into the server jar and served same-origin, so no CORS.
const server = process.env.POS_SERVER_URL ?? "http://127.0.0.1:8080";

export default defineConfig({
  plugins: [react()],
  base: "./",
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": server,
      "/actuator": server,
    },
  },
  build: { outDir: "dist", emptyOutDir: true },
  test: { environment: "node" },
});
