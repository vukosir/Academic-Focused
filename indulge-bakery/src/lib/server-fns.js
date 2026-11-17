import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireUser } from "@/lib/auth-middleware";
import { CAKE_RANGES, PALETTES } from "@/lib/cakes";

const ORDER_STATUSES = ["new", "quoted", "confirmed", "completed", "cancelled"];
const MAX_ORDER_PHOTOS = 5;
const MAX_BAG_ITEMS = 10;
const RANGE_SLUGS = CAKE_RANGES.map((r) => r.slug);

// The store is server-only; load it lazily so it never reaches the browser bundle.
const db = () => import("@/lib/db.server");
const uploads = () => import("@/lib/uploads.server");
const sessionUser = async () => (await import("@/lib/session.server")).getSessionUser();
const auditLib = () => import("@/lib/audit.server");
const forbidden = (message = "Forbidden") => Object.assign(new Error(message), { status: 403 });

// The owner's inbox needs the admin role AND two-step verification switched on:
// a stolen password alone can't open customers' details.
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

// "bag" = several ranges in the bag; "other" = no bag, design described in words.
const ITEM_VALUES = [...RANGE_SLUGS, "bag", "other"];
const text = (max) => z.string().trim().max(max);

// One line from the order bag; the option must belong to that cake range.
const bagItem = z
  .object({
    slug: z.enum(RANGE_SLUGS),
    option: z.string().max(60),
    palette: z.enum(PALETTES.map((p) => p.name)),
    quantity: z.number().int().min(1).max(20),
  })
  .refine((i) => CAKE_RANGES.find((r) => r.slug === i.slug).options.some((o) => o.name === i.option), "Unknown cake option");

const orderInput = z.object({
  name: text(80).min(2, "Please tell us your name"),
  phone: text(20).regex(/^\+?[0-9 ()-]{9,20}$/, "Please enter a valid phone number"),
  email: z.union([z.literal(""), z.string().trim().toLowerCase().email("Please enter a valid email").max(200)]).optional(),
  item: z.enum(ITEM_VALUES),
  occasion: text(80).optional(),
  date_needed: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Please choose a date"),
  servings: text(40).optional(),
  design: text(2000).optional(),
  items: z.array(bagItem).max(MAX_BAG_ITEMS).optional(),
  fulfilment: z.enum(["collection", "delivery", "not-sure"]),
  area: text(80).optional(),
  // Inspiration photos, shrunk to JPEG in the browser (each at most 1.5 MB).
  photos: z
    .array(z.string().max(2_100_000).regex(/^data:image\/jpeg;base64,[A-Za-z0-9+/]+=*$/, "Photos must be JPEG images"))
    .max(MAX_ORDER_PHOTOS, `Please attach up to ${MAX_ORDER_PHOTOS} photos`)
    .optional(),
  // Honeypot: hidden from people, filled in by bots.
  website: z.string().max(200).optional(),
}).refine((d) => (d.items?.length ?? 0) > 0 || (d.design?.length ?? 0) >= 5, {
  message: "Tell us a little about the design you have in mind",
  path: ["design"],
});

// Fields a customer may see about their own order.
const customerView = (o) => ({
  id: o.id,
  reference: o.reference,
  item: o.item,
  occasion: o.occasion,
  date_needed: o.date_needed,
  servings: o.servings,
  design: o.design,
  items: o.items ?? [],
  fulfilment: o.fulfilment,
  area: o.area,
  photos: o.photos ?? [],
  status: o.status,
  created_date: o.created_date,
});

// === Order requests ===
// Anyone can send one; a signed-in customer's request is also linked to their account.
export const createOrder = createServerFn({ method: "POST" })
  .validator(orderInput)
  .handler(async ({ data }) => {
    const [{ hit }, { clientIp }] = await Promise.all([import("@/lib/rate-limit.server"), import("@/lib/request-meta.server")]);
    // 6 requests per network per hour, and at most 200 a day site-wide (disk and inbox flooding).
    if (!hit(`order:${clientIp()}`, 6, 60 * 60 * 1000).ok || !hit("order:all", 200, 24 * 60 * 60 * 1000).ok) {
      (await auditLib()).audit("rate_limited", { key: "order" });
      throw Object.assign(new Error("You've sent several requests already. Please WhatsApp us instead, or try again in an hour."), { status: 429 });
    }
    const { website, photos = [], items = [], ...order } = data;
    const reference = `IC-${Date.now().toString(36).slice(-5).toUpperCase()}`;
    if (website) return { reference, photos: 0 };
    const { decodeJpeg, savePhoto } = await uploads();
    const buffers = photos.map(decodeJpeg); // validate all before writing any
    const user = await sessionUser();
    const { insert } = await db();
    insert("orders", {
      ...order,
      photos: buffers.map(savePhoto),
      items: items.map(({ slug, option, palette, quantity }) => ({ slug, option, palette, quantity })),
      design: order.design || "",
      email: order.email || "",
      reference,
      status: "new",
      admin_note: "",
      created_by_id: user?.id ?? null,
    });
    (await auditLib()).audit("order_created", { reference, photos: buffers.length, signed_in: !!user });
    return { reference, photos: buffers.length };
  });

export const listMyOrders = createServerFn({ method: "GET" })
  .middleware([requireUser])
  .handler(async ({ context }) => (await db()).find("orders", { where: { created_by_id: context.user.id }, sort: "-created_date" }).map(customerView));

// === Bakery owner (admin) ===
export const listOrders = createServerFn({ method: "GET" })
  .middleware([requireUser])
  .handler(async ({ context }) => {
    await requireAdmin(context.user);
    return (await db()).find("orders", { sort: "-created_date", limit: 500 });
  });

export const updateOrder = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ id: z.string().max(64), status: z.enum(ORDER_STATUSES).optional(), admin_note: text(1000).optional() }))
  .handler(async ({ data, context }) => {
    await requireAdmin(context.user);
    const { id, ...patch } = data;
    const row = (await db()).update("orders", id, patch);
    if (row) (await auditLib()).audit("order_updated", { user_id: context.user.id, reference: row.reference, fields: Object.keys(patch) });
    return row;
  });

export const deleteOrder = createServerFn({ method: "POST" })
  .middleware([requireUser])
  .validator(z.object({ id: z.string().max(64) }))
  .handler(async ({ data, context }) => {
    await requireAdmin(context.user);
    const { findOne, remove } = await db();
    const order = findOne("orders", { where: { id: data.id } });
    if (order) (await uploads()).deletePhotos(order.photos);
    if (order) (await auditLib()).audit("order_deleted", { user_id: context.user.id, reference: order.reference });
    return remove("orders", data.id);
  });
