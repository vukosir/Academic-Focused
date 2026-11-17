import { useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { ArrowRight, Check, Loader2, Paperclip, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { useBag } from "@/components/site/BagProvider";
import { createOrder } from "@/lib/server-fns";
import { MAX_INPUT_BYTES, isImageFile, toJpeg } from "@/lib/jpeg";
import { whatsappLink } from "@/lib/site";

const MAX_PHOTOS = 5;

const EMPTY = {
  customer_name: "",
  phone: "",
  email: "",
  date_needed: "",
  occasion: "",
  size_servings: "",
  design_description: "",
  fulfilment: "Collection",
  suburb: "",
  website: "", // honeypot
};

// Validation errors arrive as zod issues (JSON in the message); show the first one plainly.
function readError(err) {
  const message = err?.message || "";
  try {
    const issues = JSON.parse(message);
    if (Array.isArray(issues) && issues[0]?.message) return issues[0].message;
  } catch {
    /* not JSON */
  }
  return message || "We couldn't send your request just now. Please try again, or message us on WhatsApp.";
}

export default function QuoteForm() {
  const { items, clear } = useBag();
  const send = useServerFn(createOrder);
  const [form, setForm] = useState(EMPTY);
  const [photos, setPhotos] = useState([]); // { name, data }
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  // Photos are shrunk to JPEG in the browser and sent with the request.
  async function handlePhotos(event) {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    if (!files.length) return;
    const room = MAX_PHOTOS - photos.length;
    if (room <= 0) {
      setError("You can attach up to five inspiration photos.");
      return;
    }
    setUploading(true);
    setError("");
    const added = [];
    const problems = [];
    for (const file of files.slice(0, room)) {
      if (!isImageFile(file)) {
        problems.push(`“${file.name}” isn't a picture.`);
        continue;
      }
      if (file.size > MAX_INPUT_BYTES) {
        problems.push(`“${file.name}” is too large.`);
        continue;
      }
      try {
        added.push({ name: file.name, data: await toJpeg(file) });
      } catch {
        problems.push(`We couldn't open “${file.name}”. Try a JPG or PNG.`);
      }
    }
    if (files.length > room) problems.push("You can attach up to five inspiration photos.");
    setPhotos((prev) => [...prev, ...added]);
    if (problems.length) setError(problems.join(" "));
    setUploading(false);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (!form.customer_name.trim()) return setError("Please add your name.");
    if (!form.phone.trim()) return setError("Please add a WhatsApp or phone number.");
    if (!form.date_needed) return setError("Please choose the date you need the cake.");
    if (!form.design_description.trim() && items.length === 0) return setError("Tell us a little about the design you have in mind.");
    if (form.fulfilment === "Delivery" && !form.suburb.trim()) return setError("Please add your suburb for delivery.");

    const bagItems = items.map((item) => ({ slug: item.slug, option: item.option, palette: item.palette, quantity: item.quantity }));
    const kinds = new Set(bagItems.map((item) => item.slug));

    setSubmitting(true);
    try {
      const { reference } = await send({
        data: {
          name: form.customer_name,
          phone: form.phone,
          email: form.email,
          item: bagItems.length ? (kinds.size === 1 ? [...kinds][0] : "bag") : "other",
          occasion: form.occasion,
          date_needed: form.date_needed,
          servings: form.size_servings,
          design: form.design_description,
          fulfilment: form.fulfilment === "Delivery" ? "delivery" : "collection",
          area: form.fulfilment === "Delivery" ? form.suburb : "",
          items: bagItems,
          photos: photos.map((photo) => photo.data),
          website: form.website,
        },
      });
      setConfirmation({ ...form, items, reference, photoCount: photos.length });
      setForm(EMPTY);
      setPhotos([]);
      clear();
    } catch (err) {
      setError(readError(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (confirmation) {
    const message = [
      `Hi Indulge, I've just sent a quote request through your website (ref ${confirmation.reference}).`,
      `Name: ${confirmation.customer_name}`,
      confirmation.items.length
        ? `Cakes: ${confirmation.items.map((item) => `${item.quantity} × ${item.option} (${item.palette})`).join(", ")}`
        : "Cakes: to be discussed",
      `Date needed: ${confirmation.date_needed}`,
      confirmation.occasion ? `Occasion: ${confirmation.occasion}` : null,
      confirmation.photoCount ? `(${confirmation.photoCount} inspiration photo${confirmation.photoCount > 1 ? "s" : ""} attached to my request)` : null,
    ]
      .filter(Boolean)
      .join("\n");

    return (
      <div className="rounded-[26px] border border-border bg-card p-8 md:p-10" role="status">
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-secondary">
          <Check className="h-5 w-5" />
        </span>
        <h2 className="mt-6 font-heading text-2xl font-semibold tracking-[-0.03em]">Your brief is with Indulge.</h2>
        <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
          Your reference is <strong className="text-foreground">{confirmation.reference}</strong>. No payment has been taken. Indulge will come back to you
          with the design, the date and the final price, usually on WhatsApp. If you would like to send it through right away, use the button below.
        </p>
        <div className="mt-7 flex flex-wrap gap-3">
          <a
            href={whatsappLink(message)}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            Send it on WhatsApp <ArrowRight className="h-3.5 w-3.5" />
          </a>
          <button
            type="button"
            onClick={() => setConfirmation(null)}
            className="inline-flex items-center rounded-full border border-border px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
          >
            Send another request
          </button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-[26px] border border-border bg-card p-6 md:p-8" noValidate>
      <h2 className="font-heading text-xl font-semibold tracking-tight">Your design brief</h2>
      <p className="mt-1.5 text-sm text-muted-foreground">No payment is taken here. Everything is confirmed with Indulge first.</p>

      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        <div className="sm:col-span-1">
          <Label htmlFor="customer_name">Name *</Label>
          <Input id="customer_name" value={form.customer_name} onChange={(event) => set("customer_name", event.target.value)} className="mt-1.5" autoComplete="name" maxLength={80} />
        </div>
        <div className="sm:col-span-1">
          <Label htmlFor="phone">WhatsApp / phone *</Label>
          <Input id="phone" value={form.phone} onChange={(event) => set("phone", event.target.value)} className="mt-1.5" inputMode="tel" autoComplete="tel" maxLength={20} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="email">Email (optional)</Label>
          <Input id="email" type="email" value={form.email} onChange={(event) => set("email", event.target.value)} className="mt-1.5" autoComplete="email" maxLength={200} />
        </div>
        <div>
          <Label htmlFor="date_needed">Date needed *</Label>
          <Input id="date_needed" type="date" value={form.date_needed} onChange={(event) => set("date_needed", event.target.value)} className="mt-1.5" />
        </div>
        <div>
          <Label htmlFor="occasion">Occasion</Label>
          <Input
            id="occasion"
            value={form.occasion}
            onChange={(event) => set("occasion", event.target.value)}
            placeholder="Birthday, baby shower, engagement…"
            className="mt-1.5"
            maxLength={80}
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="size_servings">Size / servings</Label>
          <Input
            id="size_servings"
            value={form.size_servings}
            onChange={(event) => set("size_servings", event.target.value)}
            placeholder="e.g. one or two people, 20 guests, 12 cupcakes"
            className="mt-1.5"
            maxLength={40}
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="design_description">The design you have in mind {items.length ? "" : "*"}</Label>
          <Textarea
            id="design_description"
            value={form.design_description}
            onChange={(event) => set("design_description", event.target.value)}
            rows={4}
            placeholder="Colours, theme, any detail you would like on the cake…"
            className="mt-1.5"
            maxLength={2000}
          />
        </div>
      </div>

      <div className="mt-6">
        <Label>Collection or delivery *</Label>
        <div className="mt-2 flex gap-2">
          {["Collection", "Delivery"].map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => set("fulfilment", option)}
              aria-pressed={form.fulfilment === option}
              className={`rounded-full border px-5 py-2.5 text-xs transition-colors ${
                form.fulfilment === option ? "border-foreground bg-foreground text-background" : "border-border hover:bg-secondary"
              }`}
            >
              {option}
            </button>
          ))}
        </div>
        {form.fulfilment === "Delivery" && (
          <div className="mt-4">
            <Label htmlFor="suburb">Suburb</Label>
            <Input id="suburb" value={form.suburb} onChange={(event) => set("suburb", event.target.value)} placeholder="e.g. Sandton, Centurion" className="mt-1.5" maxLength={80} />
          </div>
        )}
      </div>

      <div className="mt-6">
        <Label>Inspiration photos (up to 5)</Label>
        <p className="mt-1 text-xs text-muted-foreground">Stored privately. Only Indulge can open them.</p>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <label
            htmlFor="inspiration"
            className={`inline-flex items-center gap-2 rounded-full border border-border px-4 py-2.5 text-xs transition-colors hover:bg-secondary ${
              photos.length >= MAX_PHOTOS ? "pointer-events-none opacity-50" : "cursor-pointer"
            }`}
          >
            {uploading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Paperclip className="h-3.5 w-3.5" />}
            {uploading ? "Adding…" : "Add photos"}
          </label>
          <input
            id="inspiration"
            type="file"
            accept="image/*,.heic,.heif"
            multiple
            onChange={handlePhotos}
            className="sr-only"
            disabled={uploading || photos.length >= MAX_PHOTOS}
          />
          {photos.map((photo, index) => (
            <span key={`${photo.name}-${index}`} className="inline-flex items-center gap-1.5 rounded-full bg-secondary py-1 pl-1 pr-3 text-xs">
              <img src={photo.data} alt="" className="h-7 w-7 rounded-full object-cover" />
              <span className="max-w-[140px] truncate">{photo.name}</span>
              <button
                type="button"
                aria-label={`Remove ${photo.name}`}
                onClick={() => setPhotos((prev) => prev.filter((_, i) => i !== index))}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      </div>

      <div className="hidden" aria-hidden="true">
        <label htmlFor="website">Website</label>
        <input id="website" tabIndex={-1} autoComplete="off" value={form.website} onChange={(event) => set("website", event.target.value)} />
      </div>

      {error && (
        <p role="alert" className="mt-6 rounded-xl bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting || uploading}
        className="mt-7 inline-flex w-full items-center justify-center gap-2 rounded-full bg-foreground px-6 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground disabled:opacity-60"
      >
        {submitting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
        {submitting ? (photos.length ? "Uploading photos…" : "Sending…") : "Send quote request"}
      </button>
      <p className="mt-3 text-center text-[11px] text-muted-foreground">Your request is stored with Indulge so they can quote your cake.</p>
    </form>
  );
}
