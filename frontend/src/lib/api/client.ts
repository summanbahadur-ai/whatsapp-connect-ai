"use client";

export interface ApiFailure {
  message: string;
  status: number;
  code?: string;
}
export async function apiResult<T>(
  path: string,
  init: RequestInit = {},
): Promise<{ data: T | null; error: ApiFailure | null }> {
  let status = 0;
  try {
    if (
      !path.startsWith("/api/") ||
      /[\\#]/.test(path) ||
      /%(?:2e|2f|5c)/i.test(path.split("?")[0]) ||
      path
        .split("?")[0]
        .split("/")
        .some((part) => part === "." || part === "..")
    ) {
      throw new Error("Only same-origin API paths are allowed.");
    }
    const headers = new Headers(init.headers);
    if (
      init.body &&
      !(init.body instanceof FormData) &&
      !headers.has("Content-Type")
    )
      headers.set("Content-Type", "application/json");
    const response = await fetch(path, {
      ...init,
      headers,
      cache: "no-store",
      credentials: "same-origin",
      redirect: "error",
    });
    status = response.status;
    const contentType = response.headers
      .get("content-type")
      ?.split(";")[0]
      .trim()
      .toLowerCase();
    const json =
      contentType === "application/json" || contentType?.endsWith("+json");
    if (!json)
      return {
        data: null,
        error: {
          status,
          message:
            status >= 500
              ? "Java backend is unavailable. Try again shortly."
              : "Unexpected response from Java backend.",
        },
      };
    const body = await response.json();
    if (!response.ok)
      return {
        data: null,
        error: {
          status,
          message:
            typeof body.error === "string" ? body.error : "Request failed.",
          ...(status === 409 ? { code: "23505" } : {}),
        },
      };
    return { data: body as T, error: null };
  } catch {
    return {
      data: null,
      error: {
        status,
        message:
          status >= 500 || status === 0
            ? "Java backend is unavailable. Try again shortly."
            : "Unable to read the server response.",
      },
    };
  }
}
