'use client';

import Script from 'next/script';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Download, Loader2, Play, RefreshCw } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { prepareDocsRequest, type DocsRequest } from '@/lib/openapi/request';
import { testApiConnection } from '@/lib/openapi/connection-test';
import { docsBrowserPlugin } from '@/lib/openapi/swagger-plugin';

interface SwaggerSystem {
  specActions?: { updateSpec: (spec: string) => void };
  authActions?: { logout: (names: string[]) => void };
}
type SwaggerFactory = (options: Record<string, unknown>) => SwaggerSystem;
declare global {
  interface Window {
    SwaggerUIBundle?: SwaggerFactory;
  }
}

export function ApiDocsClient() {
  const container = useRef<HTMLDivElement>(null);
  const swagger = useRef<SwaggerSystem | null>(null);
  const [scriptError, setScriptError] = useState(false);
  const [testing, setTesting] = useState(false);
  const [result, setResult] = useState<Awaited<
    ReturnType<typeof testApiConnection>
  > | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const initialize = useCallback(() => {
    if (!container.current || !window.SwaggerUIBundle || swagger.current)
      return;
    try {
      swagger.current = window.SwaggerUIBundle({
        domNode: container.current,
        url: '/api/openapi',
        layout: 'BaseLayout',
        deepLinking: true,
        filter: true,
        docExpansion: 'none',
        displayRequestDuration: true,
        defaultModelsExpandDepth: -1,
        persistAuthorization: false,
        validatorUrl: null,
        queryConfigEnabled: false,
        withCredentials: true,
        tryItOutEnabled: false,
        supportedSubmitMethods: ['get', 'post', 'put', 'patch', 'delete'],
        plugins: [docsBrowserPlugin],
        requestInterceptor: (request: DocsRequest) =>
          prepareDocsRequest(request, window.location.origin),
        syntaxHighlight: { activated: true, theme: 'agate' },
      });
    } catch {
      setScriptError(true);
    }
  }, []);
  useEffect(
    () => () => {
      // Forget pasted keys when leaving the docs page; no localStorage persistence.
      swagger.current?.authActions?.logout(['ApiKey']);
      swagger.current = null;
    },
    []
  );
  async function test() {
    setTesting(true);
    setTestError(null);
    setResult(null);
    try {
      setResult(await testApiConnection());
    } catch (error) {
      setTestError(
        error instanceof Error ? error.message : 'The API request failed.'
      );
    } finally {
      setTesting(false);
    }
  }
  return (
    <div className="space-y-6 p-4 md:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            API documentation
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Explore the current backend and test requests against this
            application.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <a
            className={buttonVariants({ variant: 'outline' })}
            href="/api/openapi"
            download="omni-connect.openapi.json"
          >
            <Download className="mr-2 size-4" />
            Download OpenAPI
          </a>
          <Button onClick={test} disabled={testing}>
            {testing ? (
              <Loader2 className="mr-2 size-4 animate-spin" />
            ) : (
              <Play className="mr-2 size-4" />
            )}
            {testing ? 'Testing…' : 'Test API'}
          </Button>
        </div>
      </div>
      <div className="grid gap-3 text-sm md:grid-cols-3">
        <div className="bg-card rounded-lg border p-4">
          <p className="font-medium">Dashboard APIs</p>
          <p className="text-muted-foreground mt-1">
            Use your current sign-in automatically. Your account and role still
            control access; no token entry is needed.
          </p>
        </div>
        <div className="bg-card rounded-lg border p-4">
          <p className="font-medium">Public /api/v1 APIs</p>
          <p className="text-muted-foreground mt-1">
            Use Authorize → ApiKey with a scoped key from Settings. It stays in
            memory and is used only for public API requests.
          </p>
        </div>
        <div className="bg-card rounded-lg border p-4">
          <p className="font-medium">Try it out → Execute</p>
          <p className="text-muted-foreground mt-1">
            Requests are real: writes change data, sends contact WhatsApp
            recipients, and AI requests can incur usage. Webhook and cron routes
            are reference-only.
          </p>
        </div>
      </div>
      <section aria-label="API connection test" aria-live="polite">
        <p className="text-muted-foreground text-xs">
          Test API performs only GET /api/auth-context. It does not send
          messages or call external providers.
        </p>
        {testError && (
          <p role="alert" className="text-destructive mt-2 text-sm">
            {testError}
          </p>
        )}
        {result && (
          <div className="bg-card mt-3 rounded-lg border p-4">
            <p
              className={
                result.ok
                  ? 'text-sm font-medium text-emerald-600 dark:text-emerald-400'
                  : 'text-destructive text-sm font-medium'
              }
            >
              {result.ok ? 'API connection successful' : 'API request failed'} ·
              HTTP {result.status} · {result.durationMs} ms
            </p>
            {result.status === 401 && (
              <p className="mt-1 text-sm">
                Your session expired.{' '}
                <a className="underline" href="/login">
                  Sign in again
                </a>
                .
              </p>
            )}
            <pre className="bg-muted mt-3 max-h-64 overflow-auto rounded p-3 text-xs break-all whitespace-pre-wrap">
              {JSON.stringify(result.body, null, 2)}
            </pre>
          </div>
        )}
      </section>
      <div className="overflow-hidden rounded-xl border bg-white text-slate-900">
        {scriptError ? (
          <div role="alert" className="p-6">
            <p>
              Swagger UI could not load. Check that the local documentation
              assets were prepared.
            </p>
            <Button
              className="mt-3"
              variant="outline"
              onClick={() => window.location.reload()}
            >
              <RefreshCw className="mr-2 size-4" />
              Reload documentation
            </Button>
          </div>
        ) : (
          <div
            ref={container}
            id="swagger-ui"
            className="min-h-48 px-2 py-4 md:px-4"
          />
        )}
      </div>
      <Script
        src="/api-docs-assets/swagger-ui-bundle.js"
        strategy="afterInteractive"
        onReady={initialize}
        onError={() => setScriptError(true)}
      />
    </div>
  );
}
