import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireUser } from "@/lib/auth-middleware";

export const MESSAGE_STATUSES = ["new", "replied", "archived"];
export const MESSAGE_REASONS = ["Job opportunity", "Internship or learnership", "Freelance project", "Collaboration", "Something else"];

// The store is server-only; load it lazily so it never reaches the browser bundle.
const db = () => import("@/lib/db.server");
const auditLib = () => import("@/lib/audit.server");
const forbidden = (message = "Forbidden") => Object.assign(new Error(message), { status: 403 });

// The inbox needs the admin role AND two-step verification switched on:
// a stolen password alone can't open people's messages.
async function requireAdmin(user) {
  const { audit } = await auditLib();
  if (user.role !== "admin") {
    audit("admin_denied", { user_id: user.id, reason: "not_admin" });
    throw forbidden();
  }
  if (!user.mfa_enabled) {
    audit("admin_denied", { user_id: user.id, reason: "mfa_off" });
    throw forbidden("MFA_REQUIRED");
  }
}

const text = (max) => z.string().trim().max(max);

const messageInput = z.object({
  name: text(80).min(2, "Please tell me your name"),
  email: z.string().trim().toLowerCase().email("Please enter a valid email").max(200),
  organisation: text(120).optional(),
  reason: z.enum(MESSAGE_REASONS),
  message: text(3000).min(10, "Please write a short message (at least 10 characters)"),
  // Honeypot: hidden from people, filled in by bots.
  website: z.string().max(200).optional(),
});

// === Contact messages ===
// Anyone can send one; they land in the owner's inbox.
export const sendMessage = createServerFn({ method: "POST" })
  .validator(messageInput)
  .handler(async ({ data }) => {
    const [{ hit }, { clientIp }] = await Promise.all([import("@/lib/rate-limit.server"), import("@/lib/request-meta.server")]);
    // 5 messages per network per hour, and at most 200 a day site-wide (inbox flooding).
    if (!hit(`message:${clientIp()}`, 5, 60 * 60 * 1000).ok || !hit("message:all", 200, 24 * 60 * 60 * 1000).ok) {
      (await auditLib()).audit("rate_limited", { key: "message" });
      throw Object.assign(new Error("You've sent several messages already. Please email me instead, or try again in an hour."), { status: 429 });
    }
    const { website, ...message } = data;
    const reference = `VR-${Date.now().toString(36).slice(-5).toUpperCase()}`;
    if (website) return { reference };
    const { insert } = await db();
    insert("messages", {
      ...message,
      organisation: message.organisation || "",
      reference,
      status: "new",
      admin_note: "",
    });
    (await auditLib()).audit("message_received", { reference });
    return { reference };
  });

// === Owner (admin) ===
export const listMessages = createServerFn({ method: "GET" })
  .middleware([requireUser])
  .handler(async ({ context }) => {
    await requireAdmin(context.user);
    return (await db()).find("messages", { sort: "-created_date", limit: 500 });
  });

export const updateMessage = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ id: z.string().max(64), status: z.enum(MESSAGE_STATUSES).optional(), admin_note: text(1000).optional() }))
  .handler(async ({ data, context }) => {
    await requireAdmin(context.user);
    const { id, ...patch } = data;
    const row = (await db()).update("messages", id, patch);
    if (row) (await auditLib()).audit("message_updated", { user_id: context.user.id, reference: row.reference, fields: Object.keys(patch) });
    return row;
  });

export const deleteMessage = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ id: z.string().max(64) }))
  .handler(async ({ data, context }) => {
    await requireAdmin(context.user);
    const { findOne, remove } = await db();
    const message = findOne("messages", { where: { id: data.id } });
    if (message) (await auditLib()).audit("message_deleted", { user_id: context.user.id, reference: message.reference });
    return remove("messages", data.id);
  });
