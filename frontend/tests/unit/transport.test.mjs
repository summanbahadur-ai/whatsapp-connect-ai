import test from "node:test";
import assert from "node:assert/strict";
import { backendOrigin } from "../../config/backend.mjs";
import { apiResult } from "../../src/lib/api/client.ts";

test("Java origin defaults only to the new Java port and rejects unsafe values", () => {
  assert.equal(backendOrigin(undefined), "http://127.0.0.1:8081");
  for (const value of [
    "http://localhost:3000",
    "http://localhost:3001",
    "https://user:secret@example.com",
    "http://localhost:8081/api",
    "ftp://example.com",
    "http://localhost:8081?x=1",
  ])
    assert.throws(() => backendOrigin(value));
  assert.equal(
    backendOrigin("https://java.example.com/"),
    "https://java.example.com",
  );
});
test("browser client rejects external or ambiguous paths without fetching", async () => {
  const previous = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    throw Error("must not fetch");
  };
  try {
    for (const path of [
      "https://example.com/api/contacts",
      "//example.com/api/contacts",
      "/api/../media/private",
      "/api/%2e%2e/media",
      "/api/contacts#secret",
      "/api/contacts\\evil",
    ]) {
      const result = await apiResult(path);
      assert.equal(result.data, null);
      assert.ok(result.error);
      assert.equal(calls, 0);
    }
  } finally {
    globalThis.fetch = previous;
  }
});
test("client keeps cookies same-origin, forwards CSRF, and exposes HTTP errors", async () => {
  const previous = globalThis.fetch;
  let observed;
  globalThis.fetch = async (path, options) => {
    observed = { path, options };
    return Response.json({ error: "forbidden" }, { status: 403 });
  };
  try {
    const result = await apiResult("/api/contacts", {
      method: "POST",
      headers: { "X-CSRF-TOKEN": "fixture" },
      body: "{}",
    });
    assert.equal(result.error.status, 403);
    assert.equal(result.error.message, "forbidden");
    assert.equal(observed.options.credentials, "same-origin");
    assert.equal(observed.options.redirect, "error");
    assert.equal(
      new Headers(observed.options.headers).get("X-CSRF-TOKEN"),
      "fixture",
    );
  } finally {
    globalThis.fetch = previous;
  }
});
test("backend HTML errors become a safe typed failure", async () => {
  const previous = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response("<html>internal diagnostic</html>", { status: 502 });
  try {
    const result = await apiResult("/api/contacts");
    assert.equal(result.error.status, 502);
    assert.equal(
      result.error.message,
      "Java backend is unavailable. Try again shortly.",
    );
  } finally {
    globalThis.fetch = previous;
  }
});

test("Spring Actuator vendor JSON is parsed as JSON", async () => {
  const previous = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response('{"status":"UP"}', {
      headers: {
        "Content-Type": "application/vnd.spring-boot.actuator.v3+json",
      },
    });
  try {
    const result = await apiResult("/api/health");
    assert.equal(result.error, null);
    assert.equal(result.data.status, "UP");
  } finally {
    globalThis.fetch = previous;
  }
});
