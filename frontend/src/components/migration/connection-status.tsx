"use client";
import { useState, useSyncExternalStore } from "react";
import { apiResult } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
const subscribe = () => () => {};
const clientReady = () => true;
const serverReady = () => false;
export function ConnectionStatus() {
  const hydrated = useSyncExternalStore(subscribe, clientReady, serverReady);
  const [message, setMessage] = useState(
    "Sign-in will be enabled in the authentication checkpoint.",
  );
  const [pending, setPending] = useState(false);
  async function check() {
    setPending(true);
    const { data, error } = await apiResult<{ status: string }>("/api/health");
    setMessage(
      error?.message ??
        (data?.status === "UP"
          ? "Java backend connected."
          : "Java backend is not ready."),
    );
    setPending(false);
  }
  return (
    <div className="mb-4 rounded-lg border border-border bg-muted p-3 text-sm">
      <p role="status" aria-live="polite">
        {message}
      </p>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="mt-2"
        disabled={!hydrated || pending}
        onClick={check}
      >
        {pending ? "Checking Java..." : "Test Java connection"}
      </Button>
    </div>
  );
}
