"use client";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { moduleForPath } from "@/lib/module-availability";

export function ModuleGate({ children }: { children: React.ReactNode }) {
  const feature = moduleForPath(usePathname());
  if (feature?.available) return children;
  return (
    <main className="flex min-h-full items-center justify-center bg-background px-4">
      <section className="w-full max-w-md rounded-xl border border-border bg-card p-6 text-center">
        <h1 className="text-xl font-semibold">
          {feature?.name ?? "Module"} is not available yet
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">
          This module is waiting for its Java migration checkpoint. No data has
          been loaded.
        </p>
        <Link href="/login" className="mt-6 inline-block text-primary">
          Back to sign in
        </Link>
      </section>
    </main>
  );
}
