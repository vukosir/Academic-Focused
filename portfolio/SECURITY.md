# Security

How this portfolio site protects visitors' messages and the owner's account, what was found in the
November 2026 audit of the codebase it was built from (the Indulge Cakes Bakery site), how it was
fixed and tested, and what still has to be set up on the host. The bakery's order bag and photo
uploads were removed for the portfolio, so the upload findings no longer apply.

Run the automated checks any time (against a local production build, never the live site):

```bash
npm run build
DATA_DIR=/tmp/portfolio-check AUTH_SECRET=<32+ random chars> PORT=3107 APP_URL=http://localhost:3107 node server.js
DATA_DIR=/tmp/portfolio-check npm run security:check -- http://localhost:3107
```

Result on 17 November 2026 (portfolio): **76 passed, 0 failed.**

## 1. Audit findings (before this work)

| # | Severity | Finding | Status |
| --- | --- | --- | --- |
| C1 | Critical | **Owner account takeover.** Any account registered with an address in `ADMIN_EMAILS` became admin automatically. There is no email verification, so whoever registered the owner's address first got the orders inbox: every customer's name, phone number, email and photos. | Fixed |
| H1 | High | **No brute-force or credential-stuffing protection.** Login, sign-up, password reset and order requests had no rate limits or lockout. | Fixed |
| H2 | High | **No two-step verification**, including on the account that can read all customer data. | Fixed (TOTP + recovery codes; required for the inbox) |
| H3 | High | **No security headers.** No Content Security Policy, HSTS, clickjacking protection, `nosniff`, Referrer-Policy or Permissions-Policy. | Fixed |
| H4 | High | **Sessions could not be revoked.** Stateless signed cookies stayed valid after logout, password reset or a role change until they expired. | Fixed (server-side sessions) |
| M1 | Medium | Weak password rules (short minimum, no common-password check). | Fixed (10+ characters, common-password and email checks) |
| M2 | Medium | Unexpected server errors could reach the visitor with internal details. | Fixed |
| M3 | Medium | Customers' photos kept their EXIF metadata, which can include the GPS location of their home. | Fixed (metadata stripped on upload) |
| M4 | Medium | Password-reset links (live tokens) were written to the server log in production. | Fixed (only in development, or when `RESET_LINKS_TO_LOG=1`) |
| M5 | Medium | The production server had no request-size cap, no slow-request timeouts and accepted every HTTP method. | Fixed |
| M6 | Medium | No security audit trail (sign-ins, lockouts, admin actions). | Fixed |
| L1 | Low | Password hashes used default scrypt settings. | Fixed (stronger settings; old hashes upgrade at next sign-in) |
| L2 | Low | Data files and photos were written with default file permissions. | Fixed (owner-only: 0600 files, 0700 folders) |
| L3 | Low | Personal data kept forever: expired sessions and reset tokens, photos of finished orders. | Fixed (automatic clean-up) |
| L4 | Low | Unused packages widened the supply chain. | Fixed (removed) |
| L5 | Low | Build-tool dependency advisories (`picomatch` ReDoS). | Accepted, see section 4 |

Checked and found sound: database access (a JSON store with no query language, so no SQL/NoSQL
injection), React output escaping (no `dangerouslySetInnerHTML`, `innerHTML`, `eval` or `new Function`
anywhere), the `returnTo` redirect check, and same-origin
enforcement on server functions (TanStack's CSRF middleware).

## 2. What is in place now

### Accounts and sign-in
- **Passwords:** at least 10 characters, refused if common or containing the email; hashed with
  scrypt (N=2^16, r=8, p=2, random salt). Never stored or logged in plain text.
- **Lockout:** 5 wrong passwords for an account locks it for 15 minutes (even the right password is
  refused). 30 sign-in attempts per network per 15 minutes. The same message for "no such account"
  and "wrong password", with a dummy hash check so timing doesn't reveal which.
- **Two-step verification (TOTP):** any account can turn it on under Account > Security, with any
  authenticator app. Codes can't be reused, guesses are rate-limited, and 8 single-use recovery
  codes are shown once (stored hashed). The authenticator secret is encrypted (AES-256-GCM) in the
  database. Turning it off needs the password and a code.
- **Sessions:** random 256-bit token in an `HttpOnly`, `Secure`, `SameSite=Lax`, `__Host-` cookie;
  only its SHA-256 hash is stored. 14-day expiry. A new token at every sign-in (no fixation).
  Logout, password reset and role changes end sessions on the server. "Where you're signed in"
  lists devices and can sign out the others.
- **Password reset:** single-use token, stored hashed, expires; same answer whether or not the
  email exists; all sessions end after a reset; rate-limited per network and per account.
- **Unusual sign-ins:** a sign-in from a new device or network is recorded as `login_new_device`.

### The owner (admin)
- Admin is never granted by signing up. It is granted on the server only:
  `npm run admin -- grant owner@example.com` (also `revoke` and `list`). Changing it signs that
  account out everywhere.
- The messages inbox and any change to a message also require two-step verification to be on.

### Application
- **CSRF:** server functions refuse cross-site requests and requests with no origin (403).
- **XSS:** React escapes all output; strict CSP with a fresh nonce per page, so injected scripts
  can't run; no inline handlers, `eval` or plugins.
- **Clickjacking:** `X-Frame-Options: DENY` and `frame-ancestors 'none'`.
- **Input validation:** every server function validates input with zod (types, lengths, formats,
  allowed values). Validation errors return only the friendly message.
- **Access control:** only the admin (with two-step verification on) can list, change or delete messages.
- **No uploads:** the contact form takes text only.
- **Error handling:** unexpected errors are logged on the server and replaced with a generic message.
  Deliberate refusals keep their status (400, 401, 403, 404, 413, 429).
- **Spam and abuse:** contact messages are limited to 5 per network per hour and 200 per day overall,
  plus a hidden honeypot field for bots. All server functions together are capped at 300 per
  network per minute.
- **Server (`server.js`):** only GET, HEAD and POST; 64 KB body cap, counted while streaming; header
  and request timeouts against slow-request attacks; static files can't escape `dist/client`;
  the client IP for rate limits comes from the socket (visitors can't spoof it); HTTPS redirect
  behind a proxy.

### Headers (production)
`Content-Security-Policy` (nonce-based scripts; `object-src 'none'`; `frame-ancestors 'none'`;
`base-uri` and `form-action 'self'`; `upgrade-insecure-requests`),
`Strict-Transport-Security: max-age=63072000; includeSubDomains`, `X-Frame-Options: DENY`,
`X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`,
`Permissions-Policy` (camera, microphone, location, payment, USB off),
`Cross-Origin-Opener-Policy` and `Cross-Origin-Resource-Policy: same-origin`, and
`Cache-Control: no-store` on pages.

### Data protection and privacy
- `DATA_DIR` holds `db.json` and `logs/`, all owner-only. Kept out of git.
- Secrets come from environment variables only. `AUTH_SECRET` (32+ characters) is required in
  production; separate keys for sessions, MFA tickets and encryption are derived from it.
- Automatic clean-up (hourly): expired sessions and reset tokens (`src/lib/maintenance.server.js`).

### Monitoring
- `DATA_DIR/logs/security.log` (JSON lines, rotated at 5 MB): registrations, sign-ins, failures,
  lockouts, new-device sign-ins, two-step changes, password resets, rate limits, admin denials,
  message changes. Emails are masked, IP addresses are keyed hashes, and no passwords, codes or
  tokens are written.
- Lockouts, admin denials, rate limits, new-device sign-ins, two-step being turned off and
  password resets are also printed to the server console as `[security]` warnings, so the host's
  log alerts can pick them up.

## 3. Deployment checklist (host settings, not in this code)

These are set where the site is hosted. The app can't do them itself.

- [ ] **HTTPS with TLS 1.2+ (prefer 1.3)** at the host or CDN, with automatic certificate renewal.
      Set `APP_URL=https://…` and, behind a proxy or CDN, `TRUST_PROXY=1`.
- [ ] **CDN with WAF and DDoS protection** (for example Cloudflare free plan): proxy the domain,
      turn on the managed WAF rules, bot fight mode, and "Always use HTTPS".
- [ ] **DNS:** registrar lock, two-step verification on the registrar and host accounts, CAA record
      for your certificate authority, DNSSEC if the registrar supports it.
- [ ] **HSTS preload** (optional, once HTTPS is solid everywhere): add `preload` and submit the domain.
- [ ] **Secrets:** set `AUTH_SECRET` in the host's secret settings, never in git:
      `node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"`.
      Changing it signs everyone out and resets two-step setups.
- [ ] **Data:** put `DATA_DIR` on a persistent, private volume and back it up daily (encrypted).
- [ ] **Owner account:** register on the site, then on the server:
      `npm run admin -- grant vukosir@icloud.com`, then turn on two-step verification
      under Account > Security and keep the recovery codes safe.
- [ ] **Alerts:** forward `[security]` lines from the server log to email or chat.
- [ ] Run one instance only (see limitations), with Node.js 20+ kept up to date.

## 4. Known limitations (honest)

- **Network layers** (TLS, DNS, WAF, DDoS, bot management) depend on the host and CDN above.
- **No passkeys (WebAuthn) yet.** Doing it properly needs a WebAuthn library; two-step codes are
  in place instead.
- **No email service:** there is no email verification at sign-up, and reset links aren't emailed.
  The owner can't reset a forgotten password by email until a provider (Resend, Postmark, SES) is
  plugged into `requestPasswordReset`. Admin is granted by CLI because emails aren't verified.
- **Rate limits are in memory** for one server process. They reset on restart and aren't shared
  between instances. Use one instance, or move them to Redis if you scale out.
- **Sign-up reveals whether an email already has an account** (it has to say so). It is limited to
  5 sign-ups per network per hour.
- **Passwords use scrypt, not Argon2id.** Both are memory-hard and recommended by OWASP; scrypt is
  built into Node, so it avoids a native dependency.
- **CSP allows inline styles** (`'unsafe-inline'` for `style-src`), needed by React style attributes.
  Scripts stay strictly nonce-only.
- **Dependency advisories:** `npm audit` reports `picomatch` ReDoS (GHSA-fc5q-39xc-vjpf) through
  Vite, Tailwind and the TanStack build plugin. They are build-time tools only. `picomatch` is not
  in the built server, there is no fixed version yet, and `npm audit fix --force` would downgrade
  core packages. Re-check with `npm run audit:deps`.
- **JSON-file database:** fine for a personal site, not for high write volume or several servers.

## 5. Recommendations

1. Put the site behind Cloudflare (or similar) with the WAF and bot protection on, from day one.
2. Add an email provider, then email verification, emailed reset links and new-device alerts.
3. Add passkeys (for example with `@simplewebauthn/server`) once email exists for recovery.
4. Move to SQLite or Postgres and a shared rate-limit store if the site grows past one server.
5. Run `npm run security:check` and `npm run audit:deps` before each release.
6. Remove `'unsafe-inline'` from `style-src` by moving inline style attributes to classes.

## Reporting a problem

Email vukosir@icloud.com with details. Please don't test against the live site.
