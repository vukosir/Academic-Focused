import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireUser } from "@/lib/auth-middleware";

// Server-only modules are loaded lazily so they never reach the browser bundle.
const server = () => import("@tanstack/react-start/server");
const authLib = () => import("@/lib/auth.server");
const dbLib = () => import("@/lib/db.server");
const sessions = () => import("@/lib/session.server");
const limiter = () => import("@/lib/rate-limit.server");
const auditLib = () => import("@/lib/audit.server");
const meta = () => import("@/lib/request-meta.server");

const fail = async (status, message) => {
  (await server()).setResponseStatus(status);
  return Object.assign(new Error(message), { status });
};

const MIN = 60 * 1000;
const email = z.string().trim().toLowerCase().email().max(200);
const passwordInput = z.string().min(1).max(200);
const code = z.string().trim().max(20);

// Runs a rate limit and turns it into a 429 with a friendly message, logged once.
async function rateLimit(key, max, windowMs, message) {
  const { hit, RateLimitError } = await limiter();
  const result = hit(key, max, windowMs);
  if (result.ok) return;
  if (result.retryAfter && hit(`${key}:logged`, 1, windowMs).ok) (await auditLib()).audit("rate_limited", { key: key.split(":")[0] });
  (await server()).setResponseStatus(429);
  throw new RateLimitError(result.retryAfter, message);
}

const ipKey = async (name) => `${name}:${(await meta()).clientIp()}`;

export const getMe = createServerFn({ method: "GET" }).handler(async () => (await sessions()).getSessionUser());

// === Create an account =======================================================
// Every new account is a customer. Admins are made only with `npm run admin`
// on the server, never by signing up (see scripts/admin.mjs).
export const register = createServerFn({ method: "POST" })
  .validator(z.object({ email, password: passwordInput, full_name: z.string().trim().max(100).optional() }))
  .handler(async ({ data }) => {
    await rateLimit(await ipKey("register"), 5, 60 * MIN, "Too many accounts created from this network. Please try again later.");
    const [{ hashPassword, passwordProblem, publicUser }, { find, insert }, { startSession }, { audit }] = await Promise.all([authLib(), dbLib(), sessions(), auditLib()]);
    const problem = passwordProblem(data.password, data.email);
    if (problem) throw await fail(400, problem);
    if (find("users", { where: { email: data.email } }).length) throw await fail(409, "An account with that email already exists. Sign in, or reset your password.");
    const user = insert("users", {
      email: data.email,
      full_name: data.full_name || "",
      password_hash: hashPassword(data.password),
      role: "user",
    });
    startSession(user);
    audit("register", { user_id: user.id, email: user.email });
    return publicUser(user);
  });

// === Sign in =================================================================
// Limits: 30 attempts per network per 15 min, and 5 failed passwords per account
// per 15 min (then the account pauses, which stops password guessing and stuffing).
export const login = createServerFn({ method: "POST" })
  .validator(z.object({ email, password: passwordInput }))
  .handler(async ({ data }) => {
    await rateLimit(await ipKey("login"), 30, 15 * MIN);
    const [{ verifyPassword, verifyDummy, needsRehash, hashPassword, createMfaTicket, MFA_COOKIE, publicUser }, { findOne, update }, { startSession, cookieOptions }, { peek, hit, clear }, { audit }] =
      await Promise.all([authLib(), dbLib(), sessions(), limiter(), auditLib()]);

    const failKey = `login-fail:${data.email}`;
    const locked = peek(failKey, 5);
    if (!locked.ok) {
      audit("login_locked", { email: data.email });
      (await server()).setResponseStatus(429);
      throw await fail(429, `Too many failed attempts for this account. Please try again in ${Math.ceil(locked.retryAfter / 60)} minutes, or reset your password.`);
    }

    const user = findOne("users", { where: { email: data.email } });
    const ok = user ? verifyPassword(data.password, user.password_hash) : verifyDummy(data.password);
    if (!user || !ok) {
      const count = hit(failKey, 5, 15 * MIN);
      audit(count.ok ? "login_failed" : "login_locked", { email: data.email, known_account: !!user });
      throw await fail(401, "Invalid email or password");
    }
    clear(failKey);
    if (needsRehash(user.password_hash)) update("users", user.id, { password_hash: hashPassword(data.password) });

    if (user.mfa_enabled) {
      (await server()).setCookie(MFA_COOKIE, createMfaTicket(user.id), { ...cookieOptions(), maxAge: 5 * 60 });
      audit("login_password_ok_mfa_pending", { user_id: user.id });
      return { mfaRequired: true };
    }
    const { newDevice } = startSession(user);
    audit(newDevice ? "login_new_device" : "login_success", { user_id: user.id, email: user.email, device: (await meta()).describeDevice() });
    return { user: publicUser(user) };
  });

// Second step: a 6-digit code from the authenticator app, or a recovery code.
export const verifyMfaLogin = createServerFn({ method: "POST" })
  .validator(z.object({ code }))
  .handler(async ({ data }) => {
    const [{ readMfaTicket, MFA_COOKIE, verifyTotp, decryptSecret, hashRecoveryCode, publicUser }, { findOne, update }, { startSession, cookieOptions }, { audit }] = await Promise.all([
      authLib(),
      dbLib(),
      sessions(),
      auditLib(),
    ]);
    const srv = await server();
    const uid = readMfaTicket(srv.getCookie(MFA_COOKIE));
    if (!uid) throw await fail(401, "Your sign-in timed out. Please enter your email and password again.");
    await rateLimit(`mfa:${uid}`, 5, 10 * MIN, "Too many codes tried. Please wait 10 minutes and sign in again.");
    const user = findOne("users", { where: { id: uid } });
    if (!user?.mfa_enabled) throw await fail(401, "Please sign in again.");

    const entered = data.code.replace(/\s/g, "");
    const step = verifyTotp(decryptSecret(user.mfa_secret_enc), entered, user.mfa_last_step ?? -1);
    let usedRecovery = false;
    if (step !== null) {
      update("users", user.id, { mfa_last_step: step });
    } else {
      const hash = hashRecoveryCode(entered);
      if (!(user.mfa_recovery || []).includes(hash)) {
        audit("mfa_failed", { user_id: user.id });
        throw await fail(401, "That code didn't work. Check the time on your phone and try again.");
      }
      update("users", user.id, { mfa_recovery: user.mfa_recovery.filter((h) => h !== hash) });
      usedRecovery = true;
    }
    srv.deleteCookie(MFA_COOKIE, cookieOptions());
    const { newDevice } = startSession(user);
    audit(usedRecovery ? "login_recovery_code_used" : newDevice ? "login_new_device" : "login_success", { user_id: user.id, email: user.email, mfa: true });
    return { user: publicUser(user), recoveryCodesLeft: usedRecovery ? (user.mfa_recovery.length - 1) : undefined };
  });

export const logout = createServerFn({ method: "POST" }).handler(async () => {
  const [{ endSession, getSessionUser }, { audit }] = await Promise.all([sessions(), auditLib()]);
  const user = getSessionUser();
  endSession();
  if (user) audit("logout", { user_id: user.id });
  return { ok: true };
});

// === Password reset ==========================================================
// No mail service is wired in yet. In development the reset link is printed to the
// server log; in production it is printed only if RESET_LINKS_TO_LOG=1. Replace the
// console.log with your email provider to deliver it. The token is stored hashed.
export const requestPasswordReset = createServerFn({ method: "POST" })
  .validator(z.object({ email }))
  .handler(async ({ data }) => {
    await rateLimit(await ipKey("reset-request"), 5, 60 * MIN);
    const [{ newResetToken, sha256 }, { findOne, insert, removeWhere }, { hit }, { audit }] = await Promise.all([authLib(), dbLib(), limiter(), auditLib()]);
    const user = findOne("users", { where: { email: data.email } });
    // Same answer either way, and quietly capped per account, so this can't be used to find accounts.
    if (user && hit(`reset:${user.id}`, 3, 60 * MIN).ok) {
      const token = newResetToken();
      removeWhere("resets", (r) => r.user_id === user.id);
      insert("resets", { user_id: user.id, token_hash: sha256(token), expires: Date.now() + 60 * MIN });
      audit("password_reset_requested", { user_id: user.id });
      if (process.env.NODE_ENV !== "production" || process.env.RESET_LINKS_TO_LOG === "1") {
        const base = process.env.APP_URL || "http://localhost:3000";
        console.log(`Password reset link for ${user.email}: ${base}/reset-password?token=${token}`);
      }
    }
    return { ok: true };
  });

export const resetPassword = createServerFn({ method: "POST" })
  .validator(z.object({ token: z.string().min(1).max(200), password: passwordInput }))
  .handler(async ({ data }) => {
    await rateLimit(await ipKey("reset"), 10, 60 * MIN);
    const [{ hashPassword, passwordProblem, sha256 }, { findOne, update, remove }, { revokeSessions }, { audit }] = await Promise.all([authLib(), dbLib(), sessions(), auditLib()]);
    const reset = findOne("resets", { where: { token_hash: sha256(data.token) } });
    if (!reset || reset.expires < Date.now()) throw await fail(400, "This reset link is invalid or has expired");
    const user = findOne("users", { where: { id: reset.user_id } });
    if (!user) throw await fail(400, "This reset link is invalid or has expired");
    const problem = passwordProblem(data.password, user.email);
    if (problem) throw await fail(400, problem);
    update("users", user.id, { password_hash: hashPassword(data.password) });
    remove("resets", reset.id);
    revokeSessions(user.id); // a reset signs every device out
    audit("password_reset", { user_id: user.id, email: user.email });
    return { ok: true };
  });

// === Two-step verification settings (signed in) ==============================
const fullUser = async (id) => (await dbLib()).findOne("users", { where: { id } });

export const mfaBeginSetup = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .handler(async ({ context }) => {
    const [{ newTotpSecret, otpauthUri, encryptSecret }, { update }] = await Promise.all([authLib(), dbLib()]);
    const user = await fullUser(context.user.id);
    if (user.mfa_enabled) throw await fail(400, "Two-step verification is already on.");
    const secret = newTotpSecret();
    update("users", user.id, { mfa_pending_enc: encryptSecret(secret), mfa_pending_expires: Date.now() + 15 * MIN });
    return { secret: secret.match(/.{1,4}/g).join(" "), uri: otpauthUri(secret, user.email) };
  });

export const mfaConfirmSetup = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ code }))
  .handler(async ({ data, context }) => {
    await rateLimit(`mfa-setup:${context.user.id}`, 10, 15 * MIN);
    const [{ verifyTotp, decryptSecret, newRecoveryCodes, hashRecoveryCode }, { update }, { revokeSessions }, { audit }] = await Promise.all([authLib(), dbLib(), sessions(), auditLib()]);
    const user = await fullUser(context.user.id);
    if (!user.mfa_pending_enc || user.mfa_pending_expires < Date.now()) throw await fail(400, "Setup timed out. Please start again.");
    const step = verifyTotp(decryptSecret(user.mfa_pending_enc), data.code.replace(/\s/g, ""));
    if (step === null) throw await fail(400, "That code didn't match. Check the time on your phone and try the newest code.");
    const codes = newRecoveryCodes();
    update("users", user.id, {
      mfa_enabled: true,
      mfa_secret_enc: user.mfa_pending_enc,
      mfa_last_step: step,
      mfa_recovery: codes.map(hashRecoveryCode),
      mfa_pending_enc: null,
      mfa_pending_expires: null,
    });
    revokeSessions(user.id, { keepCurrent: true });
    audit("mfa_enabled", { user_id: user.id });
    return { recoveryCodes: codes };
  });

export const mfaDisable = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ password: passwordInput, code }))
  .handler(async ({ data, context }) => {
    await rateLimit(`mfa-disable:${context.user.id}`, 5, 15 * MIN);
    const [{ verifyPassword, verifyTotp, decryptSecret, hashRecoveryCode }, { update }, { revokeSessions }, { audit }] = await Promise.all([authLib(), dbLib(), sessions(), auditLib()]);
    const user = await fullUser(context.user.id);
    if (!user.mfa_enabled) return { ok: true };
    const entered = data.code.replace(/\s/g, "");
    const codeOk = verifyTotp(decryptSecret(user.mfa_secret_enc), entered, user.mfa_last_step ?? -1) !== null || (user.mfa_recovery || []).includes(hashRecoveryCode(entered));
    if (!verifyPassword(data.password, user.password_hash) || !codeOk) {
      audit("mfa_disable_failed", { user_id: user.id });
      throw await fail(401, "Your password or code was not right.");
    }
    update("users", user.id, { mfa_enabled: false, mfa_secret_enc: null, mfa_last_step: null, mfa_recovery: [] });
    revokeSessions(user.id, { keepCurrent: true });
    audit("mfa_disabled", { user_id: user.id, email: user.email });
    return { ok: true };
  });

export const mfaStatus = createServerFn({ method: "GET" })
  .middleware([requireUser])
  .handler(async ({ context }) => {
    const user = await fullUser(context.user.id);
    return { enabled: !!user.mfa_enabled, recoveryCodesLeft: (user.mfa_recovery || []).length };
  });

// === Where you're signed in ==================================================
export const listMySessions = createServerFn({ method: "GET" })
  .middleware([requireUser])
  .handler(async ({ context }) => (await sessions()).listSessions(context.user.id));

export const signOutOtherDevices = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .handler(async ({ context }) => {
    const [{ revokeSessions }, { audit }] = await Promise.all([sessions(), auditLib()]);
    const count = revokeSessions(context.user.id, { keepCurrent: true });
    audit("sessions_revoked", { user_id: context.user.id, count });
    return { count };
  });
