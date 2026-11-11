export interface DocsRequest {
  url: string;
  headers: Record<string, string>;
  credentials?: RequestCredentials;
  redirect?: RequestRedirect;
  [key: string]: unknown;
}

/** Restrict the explorer to this instance. No key persistence or privileged proxy. */
export function prepareDocsRequest<T extends DocsRequest>(
  request: T,
  origin: string
): T & DocsRequest {
  const destination = new URL(request.url, origin);
  if (
    destination.origin !== new URL(origin).origin ||
    !destination.pathname.startsWith('/api/') ||
    destination.username ||
    destination.password
  )
    throw new Error('API testing is restricted to this application.');
  if (
    [
      '/api/automations/cron',
      '/api/flows/cron',
      '/api/whatsapp/webhook',
    ].includes(destination.pathname.replace(/\/$/, ''))
  )
    throw new Error(
      'This integration endpoint is reference-only in the browser explorer.'
    );
  const isPublicApi = destination.pathname.startsWith('/api/v1/');
  const headers = Object.fromEntries(
    Object.entries(request.headers ?? {}).filter(([name]) => {
      const lower = name.toLowerCase();
      return (
        !['cookie', 'x-cron-secret', 'x-hub-signature-256'].includes(lower) &&
        (lower !== 'authorization' || isPublicApi)
      );
    })
  );
  return {
    ...request,
    url: destination.href,
    headers,
    credentials: 'same-origin',
    redirect: 'error',
  };
}
