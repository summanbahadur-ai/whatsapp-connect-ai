import { defineConfig } from "@playwright/test";
const java = process.env.SIGNUP_TEST_JAVA_ORIGIN;
if (
  !java ||
  !/^http:\/\/127\.0\.0\.1:\d+$/.test(java) ||
  java.endsWith(":8081")
)
  throw new Error(
    "Run the signup browser gate through the Java disposable-database harness.",
  );
export default defineConfig({
  testDir: "./tests/signup",
  workers: 1,
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 20_000 },
  outputDir: "../.cache/cp04-signup-browser-results",
  reporter: [["list"]],
  use: {
    baseURL: "http://127.0.0.1:3012",
    channel: "chrome",
    headless: true,
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      "node node_modules/next/dist/bin/next dev --hostname 127.0.0.1 --port 3012",
    url: "http://127.0.0.1:3012/signup",
    reuseExistingServer: false,
    timeout: 120_000,
    env: { JAVA_BACKEND_ORIGIN: java, NEXT_TELEMETRY_DISABLED: "1" },
  },
});
