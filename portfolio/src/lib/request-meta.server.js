import { createHmac } from "node:crypto";
import { getRequestHeader } from "@tanstack/react-start/server";

// The visitor's IP address, for rate limiting and the audit log.
// In production server.js sets x-portfolio-client-ip from the socket (and strips any
// value a visitor sent). Behind a CDN or reverse proxy set TRUST_PROXY=1 to use the
// first X-Forwarded-For address instead; never trust that header otherwise.
export function clientIp() {
  if (process.env.TRUST_PROXY === "1") {
    const forwarded = getRequestHeader("x-forwarded-for");
    if (forwarded) return forwarded.split(",")[0].trim();
  }
  return getRequestHeader("x-portfolio-client-ip") || "local";
}

export function userAgent() {
  return (getRequestHeader("user-agent") || "").slice(0, 200);
}

// IPs are stored only as a keyed hash: enough to spot repeats, not to identify anyone.
export function hashIp(ip = clientIp()) {
  const key = process.env.AUTH_SECRET || "dev-only-insecure-secret";
  return createHmac("sha256", key).update(`ip:${ip}`).digest("base64url").slice(0, 16);
}

// A short, human description of a browser for "where you're signed in".
export function describeDevice(ua = userAgent()) {
  const browser = /Edg\//.test(ua) ? "Edge" : /Chrome\//.test(ua) ? "Chrome" : /Firefox\//.test(ua) ? "Firefox" : /Safari\//.test(ua) ? "Safari" : "Browser";
  const os = /Windows/.test(ua) ? "Windows" : /Android/.test(ua) ? "Android" : /iPhone|iPad/.test(ua) ? "iOS" : /Mac OS X/.test(ua) ? "macOS" : /Linux/.test(ua) ? "Linux" : "";
  return os ? `${browser} on ${os}` : browser;
}
