export function backendOrigin(value = process.env.JAVA_BACKEND_ORIGIN) {
  const url = new URL(value || "http://127.0.0.1:8081");
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.pathname !== "/" ||
    url.search ||
    url.hash ||
    ["3000", "3001"].includes(url.port)
  ) {
    throw new Error(
      "JAVA_BACKEND_ORIGIN must be the Java server origin, without credentials, paths or queries.",
    );
  }
  return url.origin;
}
