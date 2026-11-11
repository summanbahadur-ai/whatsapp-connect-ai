import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/browser",
  workers: 1,
  fullyParallel: false,
  timeout: 120_000,
  expect: { timeout: 30_000 },
  outputDir: "../.cache/cp03-browser-results",
  reporter: [
    ["list"],
    ["html", { outputFolder: "../.cache/cp03-browser-report", open: "never" }],
  ],
  use: {
    baseURL: "http://127.0.0.1:3011",
    channel: "chrome",
    headless: true,
    viewport: { width: 1280, height: 900 },
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: "node tests/fixtures/java-upstream.mjs",
      url: "http://127.0.0.1:18081/actuator/health",
      reuseExistingServer: false,
    },
    {
      command:
        "node node_modules/next/dist/bin/next dev --hostname 127.0.0.1 --port 3011",
      url: "http://127.0.0.1:3011/login",
      reuseExistingServer: false,
      timeout: 180_000,
      env: {
        JAVA_BACKEND_ORIGIN: "http://127.0.0.1:18081",
        NEXT_TELEMETRY_DISABLED: "1",
      },
    },
  ],
});
