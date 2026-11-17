import { createMiddleware } from "@tanstack/react-start";

// The session lives in an httpOnly cookie. Server-only modules are loaded lazily so
// they never reach the browser bundle.
const sessionUser = async () => (await import("@/lib/session.server")).getSessionUser();

// Add to a server function that needs a signed-in caller; it rejects anonymous
// callers and sets `context.user`:
//   createServerFn().middleware([requireUser]).handler(({ context }) => context.user.id)
export const requireUser = createMiddleware({ type: "function" }).server(async ({ next }) => {
  const user = await sessionUser();
  if (!user) throw Object.assign(new Error("Unauthorized"), { status: 401, code: "UNAUTHORIZED" });
  return next({ context: { user } });
});

// The same for a server route (src/routes/api/*): answers anonymous callers with 401.
export const requireUserRoute = createMiddleware({ type: "request" }).server(async ({ next }) => {
  const user = await sessionUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  return next({ context: { user } });
});
