/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite reads variables prefixed with VITE_ from .env files and exposes them on
// import.meta.env. See src/config.ts for the ones this app understands.
export default defineConfig({
  // Relative asset paths, so the built site works at any URL: a domain root,
  // or a sub-folder such as https://<user>.github.io/<repository>/.
  base: './',
  plugins: [react()],
  server: { port: 5173, open: false },
  preview: { port: 4173 },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
    unstubGlobals: true,
    coverage: {
      provider: 'v8',
      include: ['src/api/**', 'src/lib/**', 'src/state/**'],
    },
  },
});
