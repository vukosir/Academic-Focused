import { createCipheriv, createDecipheriv, createHash, createHmac, randomBytes, scryptSync, timingSafeEqual } from "node:crypto";

// ---------------------------------------------------------------------------
// Secrets
// ---------------------------------------------------------------------------
let warned = false;
const secret = () => {
  if (process.env.AUTH_SECRET) {
    if (process.env.NODE_ENV === "production" && process.env.AUTH_SECRET.length < 32) throw new Error("AUTH_SECRET must be at least 32 characters");
    return process.env.AUTH_SECRET;
  }
  if (process.env.NODE_ENV === "production") throw new Error("AUTH_SECRET must be set in production");
  if (!warned) {
    warned = true;
    console.warn("AUTH_SECRET is not set; using an insecure development secret.");
  }
  return "dev-only-insecure-secret";
};

// Separate keys per purpose, derived from AUTH_SECRET.
const subkey = (purpose) => createHmac("sha256", secret()).update(`portfolio:${purpose}`).digest();
const sign = (payload, purpose) => createHmac("sha256", subkey(purpose)).update(payload).digest("base64url");
export const sha256 = (value) => createHash("sha256").update(value).digest("base64url");

const safeEqual = (a, b) => {
  const x = Buffer.from(String(a));
  const y = Buffer.from(String(b));
  return x.length === y.length && timingSafeEqual(x, y);
};

// ---------------------------------------------------------------------------
// Passwords: scrypt (memory-hard; OWASP-recommended alternative to Argon2id that
// ships with Node, so no native dependency). Parameters live in the hash, so
// they can be raised later; older hashes are upgraded on the next sign-in.
// ---------------------------------------------------------------------------
const SCRYPT = { N: 2 ** 16, r: 8, p: 2 }; // 64 MiB, ~OWASP "N=2^16, r=8, p=2"
const KEYLEN = 64;
const MAXMEM = 256 * 1024 * 1024;

export function hashPassword(password) {
  const salt = randomBytes(16).toString("base64url");
  const hash = scryptSync(password.normalize("NFKC"), salt, KEYLEN, { ...SCRYPT, maxmem: MAXMEM }).toString("base64url");
  return `scrypt$${SCRYPT.N}$${SCRYPT.r}$${SCRYPT.p}$${salt}$${hash}`;
}

export function verifyPassword(password, stored) {
  if (!stored) return false;
  try {
    if (stored.startsWith("scrypt$")) {
      const [, N, r, p, salt, hash] = stored.split("$");
      const expected = Buffer.from(hash, "base64url");
      const actual = scryptSync(password.normalize("NFKC"), salt, expected.length, { N: +N, r: +r, p: +p, maxmem: MAXMEM });
      return timingSafeEqual(actual, expected);
    }
    // Legacy "salt:hash" (Node default scrypt N=16384) from before the upgrade.
    const [salt, hash] = stored.split(":");
    if (!salt || !hash) return false;
    const expected = Buffer.from(hash, "hex");
    return timingSafeEqual(scryptSync(password, salt, expected.length), expected);
  } catch {
    return false;
  }
}

export const needsRehash = (stored) => !String(stored).startsWith(`scrypt$${SCRYPT.N}$${SCRYPT.r}$${SCRYPT.p}$`);

// Burns the same time as a real check, so "no such user" isn't faster than "wrong password".
const DUMMY = hashPassword(randomBytes(12).toString("hex"));
export const verifyDummy = (password) => verifyPassword(password, DUMMY);

// Password rules: length over complexity (NIST SP 800-63B), no very common
// passwords, nothing built from the email address.
const COMMON = new Set(
  "password password1 password123 passw0rd 123456789 1234567890 12345678 qwerty123 qwertyuiop 1q2w3e4r5t iloveyou123 letmein123 welcome123 admin12345 abc1234567 football123 sunshine123 princess123 monkey12345 dragon12345 baseball123 superman123 trustno1234 11111111111 00000000000 vukosirikhotso portfolio123 developer123 software123 southafrica johannesburg pretoria123"
    .split(" "),
);
export function passwordProblem(password, email = "") {
  if (typeof password !== "string" || password.length < 10) return "Use at least 10 characters for your password.";
  if (password.length > 200) return "That password is too long.";
  const lower = password.toLowerCase();
  if (COMMON.has(lower) || /^(.)\1+$/.test(password)) return "That password is too common. Try a short phrase you'll remember.";
  const local = String(email).split("@")[0].toLowerCase();
  if (local.length >= 4 && lower.includes(local)) return "Don't use your email address in your password.";
  return null;
}

// ---------------------------------------------------------------------------
// Sessions: a random token in an httpOnly cookie; the server stores only its
// SHA-256 hash, so sessions can be listed and revoked (sign out, password reset).
// ---------------------------------------------------------------------------
export const SESSION_DAYS = 14;
export const SESSION_MAX_AGE = SESSION_DAYS * 24 * 60 * 60;
// __Host- cookies must be Secure, Path=/ and have no Domain: they can't be set by
// subdomains or over plain HTTP. Development runs on http://localhost, so it uses a plain name.
export const COOKIE_NAME = process.env.NODE_ENV === "production" ? "__Host-portfolio_session" : "portfolio_session";
export const MFA_COOKIE = process.env.NODE_ENV === "production" ? "__Host-portfolio_mfa" : "portfolio_mfa";

export const newSessionToken = () => randomBytes(32).toString("base64url");
export const newResetToken = () => randomBytes(32).toString("base64url");

// A short-lived signed ticket that proves the password step passed (two-step sign-in).
export function createMfaTicket(userId) {
  const payload = Buffer.from(JSON.stringify({ uid: userId, exp: Date.now() + 5 * 60 * 1000, n: randomBytes(8).toString("hex") })).toString("base64url");
  return `${payload}.${sign(payload, "mfa-ticket")}`;
}
export function readMfaTicket(token) {
  const [payload, signature] = String(token || "").split(".");
  if (!payload || !signature || !safeEqual(signature, sign(payload, "mfa-ticket"))) return null;
  try {
    const { uid, exp } = JSON.parse(Buffer.from(payload, "base64url").toString());
    return exp > Date.now() ? uid : null;
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// Two-step verification: TOTP (RFC 6238), works with any authenticator app.
// Secrets are encrypted at rest with AES-256-GCM under a key derived from AUTH_SECRET.
// ---------------------------------------------------------------------------
const B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
export function base32Encode(buffer) {
  let bits = 0;
  let value = 0;
  let out = "";
  for (const byte of buffer) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      out += B32[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) out += B32[(value << (5 - bits)) & 31];
  return out;
}
function base32Decode(text) {
  let bits = 0;
  let value = 0;
  const out = [];
  for (const ch of text.replace(/[\s=]/g, "").toUpperCase()) {
    const index = B32.indexOf(ch);
    if (index === -1) throw new Error("bad base32");
    value = (value << 5) | index;
    bits += 5;
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return Buffer.from(out);
}

export const newTotpSecret = () => base32Encode(randomBytes(20));

function hotp(secretBase32, counter) {
  const buf = Buffer.alloc(8);
  buf.writeBigUInt64BE(BigInt(counter));
  const digest = createHmac("sha1", base32Decode(secretBase32)).update(buf).digest();
  const offset = digest[digest.length - 1] & 15;
  const code = (digest.readUInt32BE(offset) & 0x7fffffff) % 1_000_000;
  return String(code).padStart(6, "0");
}

// Returns the matching time step (to block reuse), or null. Allows ±1 step for clock drift.
export function verifyTotp(secretBase32, code, lastUsedStep = -1) {
  if (!/^\d{6}$/.test(String(code))) return null;
  const now = Math.floor(Date.now() / 30_000);
  for (const step of [now - 1, now, now + 1]) {
    if (step <= lastUsedStep) continue;
    if (safeEqual(hotp(secretBase32, step), code)) return step;
  }
  return null;
}

export function otpauthUri(secretBase32, email) {
  const label = encodeURIComponent(`Vukosi Rikhotso:${email}`);
  return `otpauth://totp/${label}?secret=${secretBase32}&issuer=${encodeURIComponent("Vukosi Rikhotso")}&algorithm=SHA1&digits=6&period=30`;
}

export function encryptSecret(plain) {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", subkey("mfa-encryption"), iv);
  const data = Buffer.concat([cipher.update(plain, "utf8"), cipher.final()]);
  return `v1.${iv.toString("base64url")}.${cipher.getAuthTag().toString("base64url")}.${data.toString("base64url")}`;
}
export function decryptSecret(box) {
  const [, iv, tag, data] = String(box).split(".");
  const decipher = createDecipheriv("aes-256-gcm", subkey("mfa-encryption"), Buffer.from(iv, "base64url"));
  decipher.setAuthTag(Buffer.from(tag, "base64url"));
  return Buffer.concat([decipher.update(Buffer.from(data, "base64url")), decipher.final()]).toString("utf8");
}

// One-time recovery codes ("abcd-efgh-ij"), stored hashed.
export function newRecoveryCodes(count = 8) {
  return Array.from({ length: count }, () => {
    const raw = base32Encode(randomBytes(7)).slice(0, 10).toLowerCase();
    return `${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8)}`;
  });
}
export const hashRecoveryCode = (code) => sha256(`recovery:${String(code).toLowerCase().replace(/[^a-z0-9]/g, "")}`);

// ---------------------------------------------------------------------------
// What the browser may see about a user. Never password hashes, secrets or codes.
// ---------------------------------------------------------------------------
export const publicUser = (user) => ({
  id: user.id,
  email: user.email,
  full_name: user.full_name || "",
  role: user.role,
  mfa_enabled: !!user.mfa_enabled,
});
