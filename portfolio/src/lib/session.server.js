import { deleteCookie, getCookie, setCookie } from "@tanstack/react-start/server";
import { COOKIE_NAME, SESSION_MAX_AGE, newSessionToken, publicUser, sha256 } from "@/lib/auth.server";
import { find, findOne, insert, remove, removeWhere, update } from "@/lib/db.server";
import { describeDevice, hashIp, userAgent } from "@/lib/request-meta.server";
import { runMaintenance } from "@/lib/maintenance.server";

const cookieOptions = () => ({
  httpOnly: true,
  sameSite: "lax",
  secure: process.env.NODE_ENV === "production",
  path: "/",
});

function currentSession() {
  const token = getCookie(COOKIE_NAME);
  if (!token || token.length > 200) return null;
  const session = findOne("sessions", { where: { token_hash: sha256(token) } });
  if (!session) return null;
  if (Date.parse(session.expires) <= Date.now()) {
    remove("sessions", session.id);
    return null;
  }
  return session;
}

// The signed-in user for the current request, or null.
export function getSessionUser() {
  runMaintenance();
  const session = currentSession();
  if (!session) return null;
  const user = findOne("users", { where: { id: session.user_id } });
  return user ? publicUser(user) : null;
}

export const currentSessionId = () => currentSession()?.id ?? null;

// Starts a new session; returns whether this device/network is new for the user.
export function startSession(user) {
  const token = newSessionToken();
  const ipHash = hashIp();
  const device = describeDevice();
  const seenBefore = find("sessions", { where: { user_id: user.id } }).some((s) => s.ip_hash === ipHash || s.device === device);
  insert("sessions", {
    token_hash: sha256(token),
    user_id: user.id,
    expires: new Date(Date.now() + SESSION_MAX_AGE * 1000).toISOString(),
    ip_hash: ipHash,
    device,
    ua: userAgent().slice(0, 120),
  });
  update("users", user.id, { last_login: new Date().toISOString() });
  setCookie(COOKIE_NAME, token, { ...cookieOptions(), maxAge: SESSION_MAX_AGE });
  return { newDevice: !seenBefore && !!user.last_login };
}

export function endSession() {
  const session = currentSession();
  if (session) remove("sessions", session.id);
  deleteCookie(COOKIE_NAME, cookieOptions());
}

// Signs the user out everywhere (optionally keeping the current session).
export function revokeSessions(userId, { keepCurrent = false } = {}) {
  const keep = keepCurrent ? currentSessionId() : null;
  return removeWhere("sessions", (s) => s.user_id === userId && s.id !== keep);
}

export function listSessions(userId) {
  const current = currentSessionId();
  return find("sessions", { where: { user_id: userId }, sort: "-created_date" }).map((s) => ({
    id: s.id,
    device: s.device,
    created_date: s.created_date,
    current: s.id === current,
  }));
}

export { cookieOptions };
