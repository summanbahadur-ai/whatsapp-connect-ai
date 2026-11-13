import test from "node:test";
import assert from "node:assert/strict";
import { authClient } from "../../src/lib/auth/client.ts";
const input = {
  name: "Browser fixture",
  email: "fixture@example.invalid",
  password: "Not-a-real-password!",
};
const user = {
  id: "da579729-0239-4545-bc8c-4ebad8c51ad0",
  name: input.name,
  email: input.email,
  emailVerified: false,
  image: null,
  createdAt: "2026-11-11T00:00:00Z",
  updatedAt: "2026-11-11T00:00:00Z",
};
test("signup obtains CSRF before sending the password through same-origin transport", async () => {
  const previous = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (path, init) => {
    calls.push({ path, init });
    return path === "/api/auth/csrf"
      ? Response.json({
          token: "fixture-masked-token",
          headerName: "X-XSRF-TOKEN",
        })
      : Response.json({ user }, { status: 201 });
  };
  try {
    const result = await authClient.signUp.email(input);
    assert.equal(result.error, null);
    assert.equal(result.data.user.id, user.id);
    assert.equal(
      result.data.user.createdAt.toISOString(),
      user.createdAt.replace("Z", ".000Z"),
    );
    assert.deepEqual(
      calls.map((call) => call.path),
      ["/api/auth/csrf", "/api/auth/sign-up/email"],
    );
    assert.equal(calls[1].init.credentials, "same-origin");
    assert.equal(
      new Headers(calls[1].init.headers).get("X-XSRF-TOKEN"),
      "fixture-masked-token",
    );
    assert.deepEqual(JSON.parse(calls[1].init.body), input);
  } finally {
    globalThis.fetch = previous;
  }
});
test("CSRF failure never sends signup credentials", async () => {
  const previous = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (path) => {
    calls.push(path);
    return Response.json({ error: "unavailable" }, { status: 503 });
  };
  try {
    const result = await authClient.signUp.email(input);
    assert.equal(result.data, null);
    assert.ok(result.error);
    assert.deepEqual(calls, ["/api/auth/csrf"]);
  } finally {
    globalThis.fetch = previous;
  }
});
test("invalid CSRF envelope never sends signup credentials", async () => {
  const previous = globalThis.fetch;
  let count = 0;
  globalThis.fetch = async () => {
    count++;
    return Response.json({ token: "bad", headerName: "Authorization" });
  };
  try {
    const result = await authClient.signUp.email(input);
    assert.equal(result.data, null);
    assert.ok(result.error);
    assert.equal(count, 1);
  } finally {
    globalThis.fetch = previous;
  }
});
test("duplicate email response reaches existing UI as a safe error", async () => {
  const previous = globalThis.fetch;
  globalThis.fetch = async (path) =>
    path === "/api/auth/csrf"
      ? Response.json({ token: "fixture", headerName: "X-XSRF-TOKEN" })
      : Response.json(
          {
            error: "An account with this email already exists.",
            code: "EMAIL_ALREADY_EXISTS",
          },
          { status: 409 },
        );
  try {
    const result = await authClient.signUp.email(input);
    assert.equal(result.error.status, 409);
    assert.equal(
      result.error.message,
      "An account with this email already exists.",
    );
    assert.equal(result.error.code, "EMAIL_ALREADY_EXISTS");
  } finally {
    globalThis.fetch = previous;
  }
});
test("anonymous Java session is a safe typed 401", async () => {
  const previous = globalThis.fetch;
  globalThis.fetch = async () => Response.json({error:"unauthorized"},{status:401});
  try {
    const result=await authClient.getSession();
    assert.equal(result.data,null);
    assert.equal(result.error.status,401);
  } finally {globalThis.fetch=previous;}
});