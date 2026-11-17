import { find, removeWhere, update } from "@/lib/db.server";
import { deletePhotos } from "@/lib/uploads.server";

// Retention, run at most once an hour from normal requests:
// - expired sessions and password-reset tokens are deleted;
// - inspiration photos of orders completed or cancelled more than 90 days ago are
//   deleted (the order record itself stays for the bakery's records).
const HOUR = 60 * 60 * 1000;
export const PHOTO_RETENTION_DAYS = 90;
let lastRun = 0;

export function runMaintenance(force = false) {
  const now = Date.now();
  if (!force && now - lastRun < HOUR) return;
  lastRun = now;
  try {
    removeWhere("sessions", (s) => Date.parse(s.expires) <= now);
    removeWhere("resets", (r) => r.expires <= now);
    const cutoff = now - PHOTO_RETENTION_DAYS * 24 * HOUR;
    for (const order of find("orders", { test: (o) => ["completed", "cancelled"].includes(o.status) && o.photos?.length })) {
      const closed = Date.parse(order.updated_date || order.created_date);
      if (closed < cutoff) {
        deletePhotos(order.photos);
        update("orders", order.id, { photos: [], photos_purged: new Date().toISOString() });
      }
    }
  } catch (error) {
    console.error("[maintenance]", error.message);
  }
}
