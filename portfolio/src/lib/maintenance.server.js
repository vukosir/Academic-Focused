import { removeWhere } from "@/lib/db.server";

// Retention, run at most once an hour from normal requests:
// expired sessions and password-reset tokens are deleted.
const HOUR = 60 * 60 * 1000;
let lastRun = 0;

export function runMaintenance(force = false) {
  const now = Date.now();
  if (!force && now - lastRun < HOUR) return;
  lastRun = now;
  try {
    removeWhere("sessions", (s) => Date.parse(s.expires) <= now);
    removeWhere("resets", (r) => r.expires <= now);
  } catch (error) {
    console.error("[maintenance]", error.message);
  }
}
