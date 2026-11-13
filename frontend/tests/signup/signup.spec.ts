import { test, expect } from "@playwright/test";
const email = process.env.SIGNUP_TEST_EMAIL!;
test("real signup commits owner identity and duplicate creates no second account", async ({
  page,
  context,
}) => {
  async function fill(value: string) {
    await page.locator("#fullName").fill("Browser Owner");
    await page.locator("#email").fill(value);
    await page.locator("#password").fill("Browser-only-password!24");
    await page.locator("#confirmPassword").fill("Browser-only-password!24");
  }
  await page.goto("/signup", { waitUntil: "domcontentloaded" });
  await fill(email.toUpperCase());
  await page.locator("button[type=submit]").click();
  await expect(page.getByRole("status")).toContainText(
    "Your account has been created. You can now sign in.",
  );
  expect(new URL(page.url()).pathname).toBe("/signup");
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === "WC_CSRF")).toMatchObject({
    httpOnly: true,
    sameSite: "Lax",
  });
  expect(
    cookies.some((cookie) =>
      ["WC_SESSION", "JSESSIONID"].includes(cookie.name),
    ),
  ).toBe(false);
  await page.screenshot({
    path: "../.cache/cp04-signup-complete.png",
    fullPage: true,
  });
  expect((await context.request.get("/api/account")).status()).toBe(401);
  await page.goto("/signup");
  await fill(email);
  await page.locator("button[type=submit]").click();
  await expect(
    page.getByText("An account with this email already exists."),
  ).toBeVisible();
  // Existing CP04 account now exercises the real Java sign-in/session UI.
  await page.goto('/dashboard');await expect(page).toHaveURL(/\/login$/);
  await page.locator('#email').fill(email);await page.locator('#password').fill('Wrong-password!24');
  await page.locator('button[type=submit]').click();
  await expect(page.getByText('Invalid email or password.',{exact:true})).toBeVisible();
  await page.locator('#password').fill('Browser-only-password!24');await page.locator('button[type=submit]').click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.locator('header')).toContainText('Browser Owner');
  await expect(page.getByRole('heading',{name:'Dashboard is not available yet'})).toBeVisible();
  expect((await context.request.get('/api/auth-context')).status()).toBe(200);
  expect((await context.cookies()).find(c=>c.name==='WC_SESSION')).toMatchObject({httpOnly:true,sameSite:'Lax'});
  expect(await page.evaluate(()=>document.cookie)).not.toContain('WC_SESSION');
  await page.reload();await expect(page.locator('header')).toContainText('Browser Owner');
  const tab=await context.newPage();await tab.goto('/dashboard');await expect(tab.locator('header')).toContainText('Browser Owner');
  await page.screenshot({path:'../.cache/cp05-protected-shell.png',fullPage:true});
  await page.locator('header').getByRole('button',{name:'Open account menu'}).click();
  await page.getByRole('menuitem',{name:/Sign out/i}).click();await expect(page).toHaveURL(/\/login$/);
  expect((await context.request.get('/api/auth/get-session')).status()).toBe(401);
  await tab.reload();await expect(tab).toHaveURL(/\/login$/);
  await page.goBack();await page.reload();await expect(page).toHaveURL(/\/login$/);
  await tab.close();

});
test("same-origin proxy still enforces Java CSRF and rejects missing/foreign origin", async ({
  request,
}) => {
  const seed = await request.get("/api/auth/csrf");
  expect(seed.status()).toBe(200);
  const csrf = await seed.json();
  const data = {
    name: "Must not persist",
    email: "rejected-" + email,
    password: "Browser-only-password!24",
  };
  expect(
    (
      await request.post("/api/auth/sign-up/email", {
        data,
        headers: { Origin: "http://127.0.0.1:3012" },
      })
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.post("/api/auth/sign-up/email", {
        data,
        headers: {
          [csrf.headerName]: csrf.token,
          Origin: "https://evil.invalid",
        },
      })
    ).status(),
  ).toBe(403);
});
test("password confirmation and pending invitations never send signup requests", async ({
  page,
}) => {
  const posts: string[] = [];
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      request.url().includes("/api/auth/sign-up/email")
    )
      posts.push(request.url());
  });
  await page.goto("/signup");
  await page.locator("#fullName").fill("No request");
  await page.locator("#email").fill("no-request@signup.invalid");
  await page.locator("#password").fill("Browser-only-password!24");
  await page.locator("#confirmPassword").fill("Different-password!");
  await page.locator("button[type=submit]").click();
  await expect(
    page.getByText("Passwords do not match", { exact: true }),
  ).toBeVisible();
  await page.goto("/signup?invite=pending-token");
  await expect(page.getByRole("status")).toContainText(
    "Invitations are not available yet.",
  );
  await expect(page.locator("button[type=submit]")).toBeDisabled();
  expect(posts).toEqual([]);
});
