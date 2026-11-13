import { test, expect } from "@playwright/test";
import { createHash } from "node:crypto";

test("existing login renders and reaches the backend through the same origin", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/login");
  await expect(page.locator("input[type=email]")).toBeVisible();
  await expect(page.locator("input[type=password]")).toBeVisible();
  await page.getByRole("button", { name: "Test Java connection" }).click();
  await expect(page.getByRole("status")).toHaveText("Java backend connected.");
  expect(new URL(page.url()).pathname).toBe("/login");
  expect(errors).toEqual([]);
  await page.screenshot({ path: "../.cache/cp03-login.png", fullPage: true });
});
test("unfinished pages stay gated and do not call their business APIs", async ({
  page,
}) => {
  const calls: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/"))
      calls.push(request.url());
  });
  for (const path of [
    "/forgot-password",
    "/join/example",
  ]) {
    await page.goto(path);
    await expect(
      page.getByRole("heading", { name: /is not available yet/ }),
    ).toBeVisible();
  }
  expect(calls).toEqual([]);
});
test("rewrite preserves raw binary request, CSRF, authorization, query and method", async ({
  page,
}) => {
  await page.goto("/login");
  const bytes = [0, 13, 10, 123, 34, 195, 169, 34, 58, 32, 49, 125, 255];
  const expected = createHash("sha256")
    .update(Buffer.from(bytes))
    .digest("hex");
  const result = await page.evaluate(async (data) => {
    const response = await fetch("/api/cp03/echo?literal=%2F&plus=a+b", {
      method: "PATCH",
      headers: {
        "Content-Type": "application/octet-stream",
        "X-CSRF-TOKEN": "fixture-csrf",
        Authorization: "Bearer fixture-only",
      },
      body: new Uint8Array(data),
    });
    return { status: response.status, body: await response.json() };
  }, bytes);
  expect(result.status).toBe(200);
  expect(result.body).toMatchObject({
    hash: expected,
    method: "PATCH",
    csrf: "fixture-csrf",
    contentType: "application/octet-stream",
    authorization: "Bearer fixture-only",
  });
  const query = new URL(result.body.url, "http://fixture").searchParams;
  expect(query.get("literal")).toBe("/");
  expect(query.get("plus")).toBe("a b");
});
test("multiple HttpOnly cookies survive the rewrite and return on the next request", async ({
  page,
  context,
}) => {
  await page.goto("/login");
  await page.evaluate(() => fetch("/api/cp03/cookies"));
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === "WC_SESSION")).toMatchObject({
    value: "fixture-session",
    httpOnly: true,
    sameSite: "Lax",
  });
  expect(cookies.find((cookie) => cookie.name === "WC_TEST")).toMatchObject({
    value: "second",
    httpOnly: true,
  });
  const echoed = await page.evaluate(
    async () => await (await fetch("/api/cp03/echo")).json(),
  );
  expect(echoed.cookie).toContain("WC_SESSION=fixture-session");
  expect(await page.evaluate(() => document.cookie)).not.toContain(
    "WC_SESSION",
  );
});
test("media bytes, range metadata and 401/403 statuses survive transport", async ({
  request,
}) => {
  const media = await request.get("/media/cp03.bin");
  expect(media.status()).toBe(206);
  expect(media.headers()["content-range"]).toBe("bytes 0-6/7");
  expect([...(await media.body())]).toEqual([0, 1, 2, 13, 10, 128, 255]);
  expect((await request.get("/api/cp03/unauthorized")).status()).toBe(401);
  expect((await request.get("/api/cp03/forbidden")).status()).toBe(403);
});
test("backend connection failure is visible without leaking diagnostics", async ({
  page,
  request,
}) => {
  await page.goto("/login");
  await request.get("http://127.0.0.1:18081/__control/down");
  try {
    await page.getByRole("button", { name: "Test Java connection" }).click();
    await expect(page.getByRole("status")).toHaveText(
      "Java backend is unavailable. Try again shortly.",
    );
    await expect(
      page.getByRole("button", { name: "Test Java connection" }),
    ).toBeEnabled();
  } finally {
    await request.get("http://127.0.0.1:18081/__control/up");
  }
});
