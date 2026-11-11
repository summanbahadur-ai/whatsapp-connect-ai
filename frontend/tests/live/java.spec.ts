import { test, expect } from "@playwright/test";
test("actual Java health is reachable from the frontend on desktop and mobile", async ({
  page,
}) => {
  page.on("pageerror", (error) => console.log("Browser error:", error.message));
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await expect(page.locator("input[type=email]")).toBeVisible();
  await page.getByRole("button", { name: "Test Java connection" }).click();
  await expect(page.getByRole("status")).toHaveText("Java backend connected.");
  await page.screenshot({
    path: "../.cache/cp03-live-java-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator("input[type=email]")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth,
    ),
  ).toBe(false);
  await page.screenshot({
    path: "../.cache/cp03-live-java-mobile.png",
    fullPage: true,
  });
});
