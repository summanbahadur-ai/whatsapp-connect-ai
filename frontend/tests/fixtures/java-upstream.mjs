import { createServer } from "node:http";
import { createHash } from "node:crypto";
// Protocol fixture only. This is never part of the application or a real provider.
let down = false;
const media = Buffer.from([0, 1, 2, 13, 10, 128, 255]);
createServer(async (req, res) => {
  const url = new URL(req.url, "http://127.0.0.1");
  if (url.pathname === "/__control/down") {
    down = true;
    res.end("ok");
    return;
  }
  if (url.pathname === "/__control/up") {
    down = false;
    res.end("ok");
    return;
  }
  if (url.pathname === "/actuator/health") {
    if (down) {
      req.socket.destroy();
      return;
    }
    res.setHeader(
      "Content-Type",
      "application/vnd.spring-boot.actuator.v3+json",
    );
    res.end('{"status":"UP"}');
    return;
  }
  if (url.pathname === "/media/cp03.bin") {
    res.writeHead(206, {
      "Content-Type": "application/octet-stream",
      "Content-Range": "bytes 0-6/7",
      "Cache-Control": "no-store",
    });
    res.end(media);
    return;
  }
  if (url.pathname === "/api/cp03/cookies") {
    res.setHeader("Set-Cookie", [
      "WC_SESSION=fixture-session; HttpOnly; SameSite=Lax; Path=/",
      "WC_TEST=second; HttpOnly; SameSite=Lax; Path=/",
    ]);
  }
  if (url.pathname === "/api/cp03/unauthorized") res.statusCode = 401;
  if (url.pathname === "/api/cp03/forbidden") res.statusCode = 403;
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const body = Buffer.concat(chunks);
  res.setHeader("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
  res.end(
    JSON.stringify({
      method: req.method,
      url: req.url,
      hash: createHash("sha256").update(body).digest("hex"),
      cookie: req.headers.cookie ?? "",
      csrf: req.headers["x-csrf-token"] ?? null,
      contentType: req.headers["content-type"] ?? null,
      authorization: req.headers.authorization ?? null,
    }),
  );
}).listen(18081, "127.0.0.1", () =>
  console.log("CP03 protocol fixture listening"),
);
