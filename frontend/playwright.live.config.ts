import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/live",
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 20_000 },
  outputDir: "../.cache/cp03-live-browser-results",
  use: {
    baseURL: process.env.FRONTEND_SMOKE_URL || "http://127.0.0.1:3001",
    channel: "chrome",
    headless: true,
    viewport: { width: 1280, height: 900 },
    screenshot: "only-on-failure",
  },
});
