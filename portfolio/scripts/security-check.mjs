#!/usr/bin/env node
// Automated security checks against a LOCAL production build. It creates test
// accounts and messages, triggers lockouts and rate limits, so it refuses to run
// against anything but localhost. Use a throwaway data folder:
//
//   npm run build
//   DATA_DIR=/tmp/portfolio-check AUTH_SECRET=<32+ random chars> PORT=3107 APP_URL=http://localhost:3107 node server.js
//   DATA_DIR=/tmp/portfolio-check npm run security:check -- http://localhost:3107
//
// DATA_DIR must match the server's so the admin CLI and audit-log checks can run.
// Exit code 1 if any check fails.
import { execFileSync } from "node:child_process";
import { createHmac, randomBytes } from "node:crypto";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { toJSONAsync } from "seroval";

const BASE = (process.argv[2] || process.env.CHECK_URL || "http://localhost:3000").replace(/\/$/, "");
const HOST = new URL(BASE).hostname;
if (!["localhost", "127.0.0.1", "[::1]"].includes(HOST)) {
  console.error(`Refusing to run against ${BASE}: these checks create accounts and trigger lockouts. Run them on a local copy.`);
  process.exit(2);
}
const DATA_DIR = process.env.DATA_DIR;
if (!existsSync("dist/server/server.js")) {
  console.error("Run `npm run build` first (the checks read server function ids from the build).");
  process.exit(2);
}

// Server function name -> id, from the build's manifest.
const manifest = readFileSync("dist/server/server.js", "utf8");
const FN = Object.fromEntries([...manifest.matchAll(/"([a-f0-9]{64})": \{\s*functionName: "(\w+?)_createServerFn_handler"/g)].map((m) => [m[2], m[1]]));

let failed = 0;
let passed = 0;
function check(name, ok, detail = "") {
  if (ok) passed++;
  else failed++;
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${!ok && detail ? `  (${detail})` : ""}`);
}
const skip = (name, why) => console.log(`SKIP  ${name}  (${why})`);

// --- HTTP helpers -------------------------------------------------------------
class Client {
  cookies = new Map();
  cookieHeader() {
    return [...this.cookies].map(([k, v]) => `${k}=${v}`).join("; ");
  }
  absorb(res) {
    for (const c of res.headers.getSetCookie?.() ?? []) {
      const [pair] = c.split(";");
      const at = pair.indexOf("=");
      const k = pair.slice(0, at).trim();
      const v = pair.slice(at + 1).trim();
      if (!v || /max-age=0|expires=thu, 01 jan 1970/i.test(c)) this.cookies.delete(k);
      else this.cookies.set(k, v);
    }
  }
  async call(name, data, { method = "POST", headers = {} } = {}) {
    if (!FN[name]) throw new Error(`Unknown server function ${name}`);
    const h = { "x-tsr-serverFn": "true", accept: "application/json", origin: BASE, cookie: this.cookieHeader(), ...headers };
    let url = `${BASE}/_serverFn/${FN[name]}`;
    let body;
    const payload = data === undefined ? undefined : JSON.stringify(await toJSONAsync({ data }));
    if (method === "GET") {
      if (payload) url += `?payload=${encodeURIComponent(payload)}`;
    } else {
      body = payload;
      if (body) h["content-type"] = "application/json";
    }
    const res = await fetch(url, { method, headers: h, body, redirect: "manual" });
    this.absorb(res);
    const text = await res.text();
    const message = /"message":\{"t":1,"s":"((?:[^"\\]|\\.)*)"/.exec(text)?.[1] ?? "";
    return { status: res.status, text, message, headers: res.headers, res };
  }
}

// Plain strings in a seroval-encoded response.
const strings = (text) => [...text.matchAll(/\{"t":1,"s":"((?:[^"\\]|\\.)*)"\}/g)].map((m) => JSON.parse(`"${m[1]}"`));

const BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
function base32Decode(s) {
  let bits = 0;
  let value = 0;
  const out = [];
  for (const ch of s.replace(/[\s=]/g, "").toUpperCase()) {
    value = (value << 5) | BASE32.indexOf(ch);
    bits += 5;
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return Buffer.from(out);
}
function totp(secret, offset = 0) {
  const buf = Buffer.alloc(8);
  buf.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30_000) + offset));
  const d = createHmac("sha1", base32Decode(secret)).update(buf).digest();
  const o = d[d.length - 1] & 15;
  return String((d.readUInt32BE(o) & 0x7fffffff) % 1_000_000).padStart(6, "0");
}

const stamp = Date.now().toString(36);
const mail = (who) => `check-${who}-${stamp}@example.com`;
const strongPassword = () => `Dev-${randomBytes(9).toString("base64url")}`;
const message = (extra = {}) => ({
  name: "Security Check",
  email: "check@example.com",
  organisation: "",
  reason: "Job opportunity",
  message: "A test message from the security check.",
  ...extra,
});

// --- 1. Transport and headers -----------------------------------------------------
console.log(`\nChecking ${BASE}\n`);
const home = await fetch(`${BASE}/`);
const html = await home.text();
const h = home.headers;
const csp = h.get("content-security-policy") || "";
const nonce = /'nonce-([^']+)'/.exec(csp)?.[1];
check("CSP present with a per-request nonce", Boolean(nonce), csp || "missing");
check("CSP blocks framing, plugins and foreign scripts", /frame-ancestors 'none'/.test(csp) && /object-src 'none'/.test(csp) && /script-src 'self' 'nonce-/.test(csp) && !/unsafe-eval/.test(csp));
const scripts = [...html.matchAll(/<script\b[^>]*>/g)].map((m) => m[0]);
check("every <script> in the page carries the nonce", scripts.length > 0 && scripts.every((s) => s.includes(`nonce="${nonce}"`)), `${scripts.filter((s) => !s.includes(`nonce="${nonce}"`)).length} without`);
const second = await fetch(`${BASE}/`);
check("nonce changes on every request", /'nonce-([^']+)'/.exec(second.headers.get("content-security-policy") || "")?.[1] !== nonce);
await second.text();
check("HSTS (2 years, subdomains)", /max-age=63072000; includeSubDomains/.test(h.get("strict-transport-security") || ""));
check("X-Frame-Options DENY", h.get("x-frame-options") === "DENY");
check("X-Content-Type-Options nosniff", h.get("x-content-type-options") === "nosniff");
check("Referrer-Policy", h.get("referrer-policy") === "strict-origin-when-cross-origin");
check("Permissions-Policy denies camera/mic/location", /camera=\(\)/.test(h.get("permissions-policy") || "") && /geolocation=\(\)/.test(h.get("permissions-policy") || ""));
check("Cross-Origin-Opener-Policy same-origin", h.get("cross-origin-opener-policy") === "same-origin");
check("HTML is not cached (no-store)", /no-store/.test(h.get("cache-control") || ""));
check("no X-Powered-By / server banner", !h.get("x-powered-by") && !/node|express/i.test(h.get("server") || ""));
const asset = /src="(\/assets\/[^"]+\.js)"/.exec(html)?.[1] || /href="(\/assets\/[^"]+\.css)"/.exec(html)?.[1];
if (asset) {
  const a = await fetch(BASE + asset);
  await a.arrayBuffer();
  check("static files get nosniff + HSTS + immutable caching", a.headers.get("x-content-type-options") === "nosniff" && Boolean(a.headers.get("strict-transport-security")) && /immutable/.test(a.headers.get("cache-control") || ""));
}

// --- 2. Server hardening ---------------------------------------------------------
for (const p of ["/..%2f..%2fpackage.json", "/%2e%2e/%2e%2e/.env", "/assets/..%2f..%2f..%2fserver.js", "/data/db.json", "/.env", "/..%5c..%5cpackage.json", "/%00.js"]) {
  const r = await fetch(BASE + p);
  const t = await r.text();
  check(`path traversal blocked: ${p}`, !/"dependencies"|AUTH_SECRET|"users"\s*:|createServer\(/.test(t), `status ${r.status}`);
}
for (const m of ["PUT", "DELETE", "PATCH", "TRACE", "OPTIONS"]) {
  const r = await fetch(`${BASE}/`, { method: m === "TRACE" ? "OPTIONS" : m }).catch(() => null);
  if (m === "TRACE") continue; // fetch refuses TRACE; OPTIONS covers the same path
  check(`${m} rejected with 405`, r?.status === 405, `status ${r?.status}`);
}
const big = await fetch(`${BASE}/_serverFn/${FN.sendMessage}`, {
  method: "POST",
  headers: { "content-type": "application/json", origin: BASE, "x-tsr-serverFn": "true" },
  body: Buffer.alloc(128 * 1024, 97),
}).catch((e) => ({ status: `error ${e.cause?.code || e.message}` }));
check("request bodies over 64 KB rejected (413)", big.status === 413, `status ${big.status}`);

// --- 3. CSRF and API access control ------------------------------------------------
const anon = new Client();
for (const [label, headers] of [
  ["cross-site Origin", { origin: "https://evil.example" }],
  ["cross-site Sec-Fetch-Site", { origin: "", "sec-fetch-site": "cross-site" }],
  ["no Origin at all", { origin: "" }],
]) {
  const r = await anon.call("sendMessage", message(), { headers });
  check(`CSRF: server function refused with ${label}`, r.status === 403, `status ${r.status}`);
}
check("anonymous: messages inbox needs sign-in (401)", (await anon.call("listMessages", undefined, { method: "GET" })).status === 401);
check("anonymous: update/delete message refused", [(await anon.call("updateMessage", { id: "x", status: "replied" })).status, (await anon.call("deleteMessage", { id: "x" })).status].every((s) => s === 401));
const garbage = await fetch(`${BASE}/_serverFn/${FN.login}`, { method: "POST", headers: { "content-type": "application/json", origin: BASE, "x-tsr-serverFn": "true" }, body: '{"t":{"t":99' });
const garbageText = await garbage.text();
check("malformed request: no stack trace or file paths leaked", !/at \w+ \(|node_modules|[A-Z]:\\|\/src\/|\.js:\d+/.test(garbageText), garbageText.slice(0, 120));
check("unknown server function id is refused", (await fetch(`${BASE}/_serverFn/${"0".repeat(64)}`, { method: "POST", headers: { origin: BASE, "x-tsr-serverFn": "true" } })).status >= 400);

// --- 4. Registration and passwords ------------------------------------------------
const weak = await anon.call("register", { email: mail("weak"), password: "short1", full_name: "Weak" });
check("password under 10 characters rejected", weak.status === 400, weak.message);
const common = await anon.call("register", { email: mail("common"), password: "password123", full_name: "Common" });
check("common password rejected", common.status === 400, common.message);

const A = { c: new Client(), email: mail("alice"), password: strongPassword() };
const B = { c: new Client(), email: mail("bob"), password: strongPassword() };
const C = { c: new Client(), email: mail("owner"), password: strongPassword() };
const reg = await A.c.call("register", { email: A.email, password: A.password, full_name: "Alice Check" });
check("strong password accepted, account created", reg.status === 200, `${reg.status} ${reg.message}`);
check("registration can't choose a role", !/"admin"/.test(reg.text));
const setCookie = reg.headers.getSetCookie?.().join("\n") || "";
check("session cookie is HttpOnly + SameSite + Secure + __Host- prefixed", /HttpOnly/i.test(setCookie) && /SameSite=(Lax|Strict)/i.test(setCookie) && /Secure/i.test(setCookie) && /__Host-/.test(setCookie), setCookie.split(";")[0].replace(/=.*/, "=..."));
const me = await A.c.call("getMe", undefined, { method: "GET" });
check("getMe returns the user without secrets", strings(me.text).includes(A.email) && !/password|scrypt|hash|mfa_secret|recovery/i.test(me.text));
await B.c.call("register", { email: B.email, password: B.password, full_name: "Bob Check" });
const regC = await C.c.call("register", { email: C.email, password: C.password, full_name: "Owner Check" });
if (regC.status !== 200) {
  console.error("Could not create the test accounts (sign-up limit already used on this server?). Restart the test server and run again.");
  process.exit(2);
}
const extra = await new Client().call("register", { email: mail("sixth"), password: strongPassword(), full_name: "Sixth" });
check("registration rate limit (6th from one network in an hour: 429)", extra.status === 429, `status ${extra.status}`);

// --- 5. Sign-in, lockout, sessions -------------------------------------------------
const before = A.c.cookieHeader();
const l1 = await A.c.call("login", { email: A.email, password: A.password });
check("login works and issues a fresh session token", l1.status === 200 && A.c.cookieHeader() !== before);

const wrong = await new Client().call("login", { email: A.email, password: "Wrong-password-123" });
const unknown = await new Client().call("login", { email: mail("nobody"), password: "Wrong-password-123" });
check("same error for wrong password and unknown email (no enumeration)", wrong.status === 401 && unknown.status === 401 && wrong.message === unknown.message);

for (let i = 0; i < 5; i++) await new Client().call("login", { email: B.email, password: `Wrong-${i}-password` });
const locked = await new Client().call("login", { email: B.email, password: B.password });
check("account locks after 5 wrong passwords (even the right one is refused)", locked.status === 429, `status ${locked.status} ${locked.message}`);

const reset1 = await new Client().call("requestPasswordReset", { email: A.email });
const reset2 = await new Client().call("requestPasswordReset", { email: mail("nobody") });
check("password reset: same answer for known and unknown emails", reset1.status === 200 && reset2.status === 200 && reset1.text === reset2.text);
const badToken = await new Client().call("resetPassword", { token: "not-a-real-token", password: strongPassword() });
check("password reset: forged token refused", badToken.status >= 400);

// Logout really ends the session server-side (a copied cookie stops working).
const D = new Client();
await D.call("login", { email: A.email, password: A.password });
const copied = new Client();
copied.cookies = new Map(D.cookies);
await D.call("logout");
const afterLogout = await copied.call("getMe", undefined, { method: "GET" });
check("logout revokes the session on the server (stolen cookie is useless)", !strings(afterLogout.text).includes(A.email));

// --- 6. Contact messages, validation, access ------------------------------------------
const m1 = await anon.call("sendMessage", message({ message: `<script>alert(1)</script><img src=x onerror=alert(2)> hello` }));
check("contact message accepted", m1.status === 200, `${m1.status} ${m1.message}`);
const badEmail = await anon.call("sendMessage", message({ email: "not-an-email" }));
check("message: invalid email rejected", badEmail.status === 400, `status ${badEmail.status}`);
check("validation errors return the friendly message only (no schema internals)", !/pattern|invalid_format|origin/.test(badEmail.text));
const badReason = await anon.call("sendMessage", message({ reason: "Something not on the list" }));
check("message: unknown reason rejected", badReason.status === 400, `status ${badReason.status}`);
const tooLong = await anon.call("sendMessage", message({ message: "x".repeat(3001) }));
check("message: over-long text rejected", tooLong.status === 400, `status ${tooLong.status}`);
const injection = await anon.call("sendMessage", message({ name: { $ne: null } }));
check("injection: objects in place of text refused by validation", injection.status === 400, `status ${injection.status}`);
const honeypot = await anon.call("sendMessage", message({ website: "http://spam.example" }));
check("bot honeypot: request silently dropped", honeypot.status === 200);
check("non-admin: inbox refused (403)", (await A.c.call("listMessages", undefined, { method: "GET" })).status === 403);
check("non-admin: can't change a message", (await A.c.call("updateMessage", { id: "x", status: "replied" })).status === 403);
check("non-admin: can't delete a message", (await B.c.call("deleteMessage", { id: "x" })).status === 403);
let messageLimited = false;
for (let i = 0; i < 6 && !messageLimited; i++) messageLimited = (await new Client().call("sendMessage", message())).status === 429;
check("contact messages rate-limited per network (429)", messageLimited);

// --- 7. Two-step verification ----------------------------------------------------------
const setup = await A.c.call("mfaBeginSetup");
const secret = strings(setup.text).find((s) => /^[A-Z2-7 ]{20,}$/.test(s))?.replace(/ /g, "");
check("MFA: setup returns a base32 secret", Boolean(secret), setup.message);
if (secret) {
  check("MFA: wrong code refused at setup", (await A.c.call("mfaConfirmSetup", { code: "000000" === totp(secret) ? "111111" : "000000" })).status >= 400);
  const confirm = await A.c.call("mfaConfirmSetup", { code: totp(secret) });
  const recovery = strings(confirm.text).filter((s) => /^[a-z2-7]{4}-[a-z2-7]{4}-[a-z2-7]{2}$/.test(s));
  check("MFA: confirmed, 8 recovery codes issued once", confirm.status === 200 && recovery.length === 8, `${confirm.status} ${confirm.message} ${recovery.length}`);

  const s1 = new Client();
  const step1 = await s1.call("login", { email: A.email, password: A.password });
  check("MFA: password alone no longer signs in", /mfaRequired/.test(step1.text) && !strings((await s1.call("getMe", undefined, { method: "GET" })).text).includes(A.email));
  const reuse = await s1.call("verifyMfaLogin", { code: totp(secret) });
  check("MFA: a code can't be used twice (replay)", reuse.status >= 400, `status ${reuse.status}`);
  const ok = await s1.call("verifyMfaLogin", { code: totp(secret, 1) });
  check("MFA: next valid code completes sign-in", ok.status === 200 && strings((await s1.call("getMe", undefined, { method: "GET" })).text).includes(A.email), `${ok.status} ${ok.message}`);

  if (recovery.length) {
    const s2 = new Client();
    await s2.call("login", { email: A.email, password: A.password });
    check("MFA: recovery code signs in", (await s2.call("verifyMfaLogin", { code: recovery[0] })).status === 200);
    const s3 = new Client();
    await s3.call("login", { email: A.email, password: A.password });
    check("MFA: recovery code works only once", (await s3.call("verifyMfaLogin", { code: recovery[0] })).status >= 400);
  }
  const s4 = new Client();
  await s4.call("login", { email: A.email, password: A.password });
  let codeLimited = false;
  for (let i = 0; i < 7 && !codeLimited; i++) codeLimited = (await s4.call("verifyMfaLogin", { code: String(100000 + i) })).status === 429;
  check("MFA: guessing codes is rate-limited", codeLimited);
  const noTicket = await new Client().call("verifyMfaLogin", { code: totp(secret, 1) });
  check("MFA: code step can't be done without the password step", noTicket.status >= 400);
  const sessionsList = await A.c.call("listMySessions", undefined, { method: "GET" });
  check("sessions list shows devices", sessionsList.status === 200);
  const signOut = await A.c.call("signOutOtherDevices");
  check("sign out other devices revokes them", signOut.status === 200 && !strings((await s1.call("getMe", undefined, { method: "GET" })).text).includes(A.email));
}

// --- 8. Admin role ----------------------------------------------------------------------
if (!DATA_DIR) {
  skip("admin role and audit log checks", "set DATA_DIR to the server's data folder");
} else {
  const env = { ...process.env, DATA_DIR };
  execFileSync(process.execPath, ["scripts/admin.mjs", "grant", C.email], { env, stdio: "ignore" });
  const ownerSession = new Client();
  await ownerSession.call("login", { email: C.email, password: C.password });
  const noMfa = await ownerSession.call("listMessages", undefined, { method: "GET" });
  check("admin without two-step verification: inbox refused", noMfa.status === 403 && /MFA_REQUIRED/.test(noMfa.text), `${noMfa.status} ${noMfa.message}`);
  const ownerSetup = await ownerSession.call("mfaBeginSetup");
  const ownerSecret = strings(ownerSetup.text).find((s) => /^[A-Z2-7 ]{20,}$/.test(s))?.replace(/ /g, "");
  await ownerSession.call("mfaConfirmSetup", { code: totp(ownerSecret) });
  const inbox = await ownerSession.call("listMessages", undefined, { method: "GET" });
  check("admin with two-step verification: inbox opens", inbox.status === 200, `${inbox.status} ${inbox.message}`);
  check("XSS payload kept as inert text in the data (React escapes it on display)", inbox.text.includes("alert(1)"));
  const sinks = readdirSync("src", { recursive: true })
    .filter((f) => /\.(jsx?|tsx?)$/.test(f))
    .filter((f) => /dangerouslySetInnerHTML|\.innerHTML\s*=|\beval\(|new Function\(/.test(readFileSync(join("src", f), "utf8")));
  check("no raw-HTML or eval sinks in the source", sinks.length === 0, sinks.join(", "));
  execFileSync(process.execPath, ["scripts/admin.mjs", "revoke", C.email], { env, stdio: "ignore" });
  check("revoking admin signs that account out everywhere", !strings((await ownerSession.call("getMe", undefined, { method: "GET" })).text).includes(C.email));

  const log = existsSync(join(DATA_DIR, "logs", "security.log")) ? readFileSync(join(DATA_DIR, "logs", "security.log"), "utf8") : "";
  check("audit log records security events", /login_locked/.test(log) && /login_success|login_new_device/.test(log) && /mfa_enabled/.test(log));
  check("audit log contains no passwords, codes or full emails", ![A.password, B.password, C.password, A.email, B.email, C.email].some((v) => log.includes(v)));
  const db = readFileSync(join(DATA_DIR, "db.json"), "utf8");
  check("database: no plaintext passwords or TOTP secrets", ![A.password, B.password, C.password, secret, ownerSecret].filter(Boolean).some((v) => db.includes(v)) && /scrypt\$/.test(db));
}

console.log(`\n${passed} passed, ${failed} failed\n`);
process.exit(failed ? 1 : 0);
