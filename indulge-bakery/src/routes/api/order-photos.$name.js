import { createFileRoute } from "@tanstack/react-router";
import { requireUserRoute } from "@/lib/auth-middleware";

// Serves a customer's order photo to the bakery owner, or to the customer whose order it is.
// An <img> requests it by URL, so it is a server route rather than a server function.
export const Route = createFileRoute("/api/order-photos/$name")({
  server: {
    middleware: [requireUserRoute],
    handlers: {
      GET: async ({ params, context }) => {
        const [{ findOne }, { isPhotoName, readPhoto }] = await Promise.all([import("@/lib/db.server"), import("@/lib/uploads.server")]);
        const notFound = () => new Response("Not found", { status: 404 });
        if (!isPhotoName(params.name)) return notFound();
        const order = findOne("orders", { test: (o) => o.photos?.includes(params.name) });
        // Same rule as the inbox: an admin also needs two-step verification switched on.
        const isAdmin = context.user.role === "admin" && context.user.mfa_enabled;
        if (!order || (!isAdmin && order.created_by_id !== context.user.id)) return notFound();
        const photo = readPhoto(params.name);
        if (!photo) return notFound();
        return new Response(photo, {
          headers: {
            "content-type": "image/jpeg",
            // Private photos: never keep a copy in the browser cache (shared computers).
            "cache-control": "private, no-store",
            "x-content-type-options": "nosniff",
          },
        });
      },
    },
  },
});
