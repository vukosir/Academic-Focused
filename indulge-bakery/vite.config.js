import { fileURLToPath } from "node:url";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { existsSync } from "node:fs";

// Server code reads its settings (AUTH_SECRET, APP_URL, DATA_DIR, ...) from process.env;
// load .env into it for the dev server. Variables already set in the shell win.
if (existsSync(".env")) process.loadEnvFile(".env");

export default defineConfig({
  // PORT lets tools (and you) run a second copy; it defaults to 3000.
  server: { port: Number(process.env.PORT) || 3000 },
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  plugins: [tanstackStart({ srcDirectory: "src" }), react()],
});
