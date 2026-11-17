import { createCsrfMiddleware, createMiddleware, createStart } from "@tanstack/react-start";

// Server functions are same-origin RPC endpoints: reject cross-site requests (CSRF).
const csrfMiddleware = createCsrfMiddleware({
  filter: (ctx) => ctx.handlerType === "serverFn",
});

const MAX_BODY_BYTES = 64 * 1024; // contact messages and sign-in forms are small
const isProd = process.env.NODE_ENV === "production";

// Content Security Policy (production). Scripts must carry this request's nonce, so an
// injected <script> can't run; no inline event handlers, no eval, no plugins, no framing.
// Styles allow 'unsafe-inline' because React style attributes (image focal points,
// language swatches) need it; style injection can't run code.
function contentSecurityPolicy(nonce) {
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}'`,
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    "img-src 'self' data: blob:",
    "connect-src 'self'",
    "frame-src 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    "upgrade-insecure-requests",
  ].join("; ");
}

export function securityHeaders(headers, { nonce, html } = {}) {
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()");
  headers.set("X-Frame-Options", "DENY");
  headers.set("Cross-Origin-Opener-Policy", "same-origin");
  headers.set("Cross-Origin-Resource-Policy", "same-origin");
  if (isProd) {
    headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
    if (html && nonce) headers.set("Content-Security-Policy", contentSecurityPolicy(nonce));
  }
}

const randomNonce = () => btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(16))));

const securityMiddleware = createMiddleware({ type: "request" }).server(async ({ request, next, handlerType }) => {
  // Request size limit (prod server.js also counts streamed bytes).
  if (Number(request.headers.get("content-length") || 0) > MAX_BODY_BYTES) {
    const headers = new Headers({ "content-type": "text/plain" });
    securityHeaders(headers);
    return new Response("Request too large", { status: 413, headers });
  }

  // A broad per-network cap on API calls, on top of the tighter per-action limits.
  if (handlerType === "serverFn") {
    const [{ hit }, { clientIp }] = await Promise.all([import("@/lib/rate-limit.server"), import("@/lib/request-meta.server")]);
    const result = hit(`api:${clientIp()}`, 300, 60 * 1000);
    if (!result.ok) {
      const headers = new Headers({ "content-type": "text/plain", "retry-after": String(result.retryAfter) });
      securityHeaders(headers);
      return new Response("Too many requests", { status: 429, headers });
    }
  }

  const nonce = randomNonce();
  const result = await next({ context: { nonce } });
  const original = result.response;
  const headers = new Headers(original.headers);
  securityHeaders(headers, { nonce, html: (headers.get("content-type") || "").includes("text/html") });
  if ((headers.get("content-type") || "").includes("text/html")) headers.set("Cache-Control", headers.get("Cache-Control") || "no-store");
  result.response = new Response(original.body, { status: original.status, statusText: original.statusText, headers });
  return result;
});

// Unexpected server errors never reach the visitor with internals (paths, stack
// traces, database details): they're logged on the server and replaced with a
// generic message. Deliberate errors (validation, 401/403/404/429) pass through.
function validationIssues(error) {
  if (Array.isArray(error?.issues)) return error.issues;
  try {
    const parsed = JSON.parse(error?.message);
    if (Array.isArray(parsed) && parsed.length && parsed.every((i) => typeof i?.message === "string" && Array.isArray(i.path))) return parsed;
  } catch {
    /* not a validation error */
  }
  return null;
}

const maskErrors = createMiddleware({ type: "function" }).server(async ({ next }) => {
  try {
    return await next();
  } catch (error) {
    if (error instanceof Response || error?.isNotFound || error?.isRedirect) throw error;
    const { setResponseStatus } = await import("@tanstack/react-start/server");
    // Deliberate errors keep their HTTP status (401/403/404/429); validation errors are 400.
    if (error?.status >= 400 && error.status < 500) {
      setResponseStatus(error.status);
      throw error;
    }
    // Input validation failures: TanStack throws an Error whose message is the JSON list
    // of issues. Keep just the friendly messages and field paths for the form.
    const issues = validationIssues(error);
    if (issues) {
      setResponseStatus(400);
      throw new Error(JSON.stringify(issues.map(({ message, path }) => ({ message, path }))));
    }
    console.error("[server error]", error);
    setResponseStatus(500);
    throw Object.assign(new Error("Something went wrong. Please try again."), { status: 500 });
  }
});

export const startInstance = createStart(() => ({
  requestMiddleware: [securityMiddleware, csrfMiddleware],
  functionMiddleware: [maskErrors],
}));
