// Production server: serves the built client assets and hands everything else
// to the TanStack Start server bundle. Run `npm run build`, then `npm start`.
//
// Security at this layer (see SECURITY.md): only GET/HEAD/POST, a 12 MB request
// body cap, slow-request timeouts, safe static file paths, baseline security
// headers on static files (the app adds CSP and the rest), the client IP passed on
// in a header visitors can't spoof, and an HTTPS redirect when behind a proxy.
// TLS itself is terminated by the host/CDN in front of this process.
import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize, resolve, sep } from "node:path";
import { Readable, Transform } from "node:stream";
import { pathToFileURL } from "node:url";

process.env.NODE_ENV ??= "production";

const PORT = Number(process.env.PORT) || 3000;
const CLIENT_DIR = resolve("dist/client");
const MAX_BODY_BYTES = 12 * 1024 * 1024;
const TRUST_PROXY = process.env.TRUST_PROXY === "1";
if (!process.env.APP_URL) console.warn("[security] APP_URL is not set; set it to the site's https:// address in production.");
const { default: app } = await import(pathToFileURL(resolve("dist/server/server.js")).href);

const TYPES = {
  ".js": "text/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".json": "application/json",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".webp": "image/webp",
  ".ico": "image/x-icon",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".txt": "text/plain",
};

const BASE_HEADERS = {
  "x-content-type-options": "nosniff",
  "referrer-policy": "strict-origin-when-cross-origin",
  "x-frame-options": "DENY",
  "cross-origin-resource-policy": "same-origin",
  "strict-transport-security": "max-age=63072000; includeSubDomains",
};

function serveStatic(url, res) {
  let path;
  try {
    path = decodeURIComponent(url.pathname);
  } catch {
    return false;
  }
  if (path.includes("\0")) return false;
  const file = join(CLIENT_DIR, normalize(path));
  if (!file.startsWith(CLIENT_DIR + sep) || !existsSync(file) || !statSync(file).isFile()) return false;
  const immutable = url.pathname.startsWith("/assets/");
  res.writeHead(200, {
    ...BASE_HEADERS,
    "content-type": TYPES[extname(file).toLowerCase()] || "application/octet-stream",
    "cache-control": immutable ? "public, max-age=31536000, immutable" : "public, max-age=3600",
  });
  createReadStream(file).pipe(res);
  return true;
}

// Stops reading a request body once it passes the limit.
function limitBody(req) {
  let seen = 0;
  return req.pipe(
    new Transform({
      transform(chunk, _enc, done) {
        seen += chunk.length;
        if (seen > MAX_BODY_BYTES) return done(Object.assign(new Error("Request too large"), { status: 413 }));
        done(null, chunk);
      },
    }),
  );
}

const plain = (res, status, text, extra = {}) => res.writeHead(status, { ...BASE_HEADERS, "content-type": "text/plain", ...extra }).end(text);

const server = createServer(async (req, res) => {
  try {
    if (!["GET", "HEAD", "POST"].includes(req.method)) return plain(res, 405, "Method Not Allowed", { allow: "GET, HEAD, POST" });
    if (Number(req.headers["content-length"] || 0) > MAX_BODY_BYTES) return plain(res, 413, "Request too large");

    // Behind a CDN/proxy that terminates TLS: send plain-HTTP visitors to HTTPS.
    if (TRUST_PROXY && req.headers["x-forwarded-proto"] === "http" && process.env.APP_URL?.startsWith("https://")) {
      return res.writeHead(301, { location: new URL(req.url, process.env.APP_URL).href }).end();
    }

    const origin = process.env.APP_URL || `http://${req.headers.host}`;
    const url = new URL(req.url, origin);
    if ((req.method === "GET" || req.method === "HEAD") && serveStatic(url, res)) return;

    // The visitor's address for rate limiting; any value they sent is replaced.
    const headers = { ...req.headers };
    headers["x-indulge-client-ip"] = req.socket.remoteAddress || "unknown";

    const hasBody = req.method === "POST";
    const response = await app.fetch(
      new Request(url, { method: req.method, headers, body: hasBody ? Readable.toWeb(limitBody(req)) : undefined, duplex: "half" }),
    );
    const out = {};
    for (const [key, value] of response.headers) if (key !== "set-cookie") out[key] = value;
    const cookies = response.headers.getSetCookie?.() ?? [];
    if (cookies.length) out["set-cookie"] = cookies;
    res.writeHead(response.status, out);
    if (response.body) Readable.fromWeb(response.body).pipe(res);
    else res.end();
  } catch (error) {
    if (error?.status === 413) return plain(res, 413, "Request too large");
    console.error(error);
    if (!res.headersSent) plain(res, 500, "Internal Server Error");
    else res.end();
  }
});

// Slow-request (slowloris) protection.
server.headersTimeout = 15_000;
server.requestTimeout = 60_000;
server.keepAliveTimeout = 5_000;

server.listen(PORT, () => console.log(`Indulge Cakes Bakery listening on http://localhost:${PORT}`));
