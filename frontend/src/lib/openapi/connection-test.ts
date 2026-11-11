export async function testApiConnection(fetcher: typeof fetch = fetch) {
  const started = Date.now();
  const response = await fetcher('/api/auth-context', {
    method: 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    redirect: 'error',
    headers: { Accept: 'application/json' },
    signal: AbortSignal.timeout(15_000),
  });
  const raw = await response.text();
  let body: unknown = raw;
  try {
    body = JSON.parse(raw);
  } catch {
    /* Display non-JSON errors as plain text. */
  }
  return {
    ok: response.ok,
    status: response.status,
    durationMs: Date.now() - started,
    body,
  };
}
