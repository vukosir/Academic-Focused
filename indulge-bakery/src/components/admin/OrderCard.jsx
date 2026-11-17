import { useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { Check, Loader2, MessageCircle, Store, Trash2, Truck } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import InspirationPhotos from "@/components/admin/InspirationPhotos";
import { deleteOrder, updateOrder } from "@/lib/server-fns";
import { getRange } from "@/lib/cakes";
import { formatDate, waNumber } from "@/lib/site";

const STATUSES = ["new", "quoted", "confirmed", "completed", "cancelled"];
const STATUS_STYLES = {
  new: "bg-white/10 text-white",
  quoted: "bg-amber-400/15 text-amber-200",
  confirmed: "bg-emerald-400/15 text-emerald-200",
  completed: "bg-sky-400/15 text-sky-200",
  cancelled: "bg-red-400/15 text-red-200",
};

export default function OrderCard({ order, onChanged }) {
  const save = useServerFn(updateOrder);
  const remove = useServerFn(deleteOrder);
  const [note, setNote] = useState(order.admin_note || "");
  const [noteSaved, setNoteSaved] = useState(false);
  const [savingNote, setSavingNote] = useState(false);
  const [savingStatus, setSavingStatus] = useState(false);
  const [error, setError] = useState("");

  async function changeStatus(status) {
    setSavingStatus(true);
    setError("");
    try {
      await save({ data: { id: order.id, status } });
      onChanged();
    } catch {
      setError("Could not change the status. Try again.");
    } finally {
      setSavingStatus(false);
    }
  }

  async function saveNote() {
    setSavingNote(true);
    setError("");
    try {
      await save({ data: { id: order.id, admin_note: note } });
      setNoteSaved(true);
      window.setTimeout(() => setNoteSaved(false), 1800);
      onChanged();
    } catch {
      setError("Could not save the note. Try again.");
    } finally {
      setSavingNote(false);
    }
  }

  async function destroy() {
    if (!window.confirm(`Delete the request from ${order.name} (${order.reference})? Its photos are deleted too. This cannot be undone.`)) return;
    try {
      await remove({ data: { id: order.id } });
      onChanged();
    } catch {
      setError("Could not delete this request. Try again.");
    }
  }

  const items = Array.isArray(order.items) ? order.items : [];
  const delivery = order.fulfilment === "delivery";
  const whatsappText = encodeURIComponent(`Hi ${order.name}, it's Indulge Cakes about your cake request for ${formatDate(order.date_needed)} (ref ${order.reference}).`);

  return (
    <article className="rounded-2xl border border-white/10 bg-[#141416] p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-heading text-lg font-semibold tracking-tight text-white">{order.name}</h2>
            <span className={`rounded-full px-2.5 py-1 text-[10px] uppercase tracking-[0.1em] ${STATUS_STYLES[order.status] || "bg-white/10 text-white"}`}>
              {order.status}
            </span>
          </div>
          <p className="mt-1 text-xs text-white/50">
            Needed {formatDate(order.date_needed)}
            {order.occasion ? ` · ${order.occasion}` : ""}
            {order.servings ? ` · ${order.servings}` : ""}
            {order.reference ? ` · ${order.reference}` : ""}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {savingStatus && <Loader2 className="h-3.5 w-3.5 animate-spin text-white/50" />}
          <Select value={order.status} onValueChange={changeStatus}>
            <SelectTrigger className="h-9 w-[132px] border-white/15 bg-[#1c1c1f] text-xs capitalize text-white" aria-label="Status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {STATUSES.map((status) => (
                <SelectItem key={status} value={status} className="text-xs capitalize">
                  {status}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <a
            href={`https://wa.me/${waNumber(order.phone)}?text=${whatsappText}`}
            target="_blank"
            rel="noreferrer"
            aria-label={`WhatsApp ${order.name}`}
            className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-300 transition-colors hover:bg-emerald-500/25"
          >
            <MessageCircle className="h-4 w-4" />
          </a>
        </div>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div className="text-xs text-white/60">
          <p>
            <span className="text-white/40">Phone </span>
            <a href={`tel:${order.phone}`} className="text-white/90 hover:underline">
              {order.phone}
            </a>
          </p>
          {order.email && (
            <p className="mt-1 truncate">
              <span className="text-white/40">Email </span>
              <a href={`mailto:${order.email}`} className="text-white/90 hover:underline">
                {order.email}
              </a>
            </p>
          )}
          <p className="mt-1 flex items-center gap-1.5">
            {delivery ? <Truck className="h-3.5 w-3.5 text-white/40" /> : <Store className="h-3.5 w-3.5 text-white/40" />}
            {delivery ? "Delivery" : "Collection"}
            {order.area ? ` · ${order.area}` : ""}
          </p>
        </div>

        <div className="text-xs text-white/70">
          {items.length > 0 ? (
            <ul className="space-y-1">
              {items.map((item, index) => (
                <li key={`${item.option}-${index}`}>
                  <span className="text-white/90">
                    {item.quantity} × {item.option}
                  </span>
                  <span className="text-white/40">
                    {" "}
                    · {getRange(item.slug)?.name ?? item.slug} · {item.palette}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-white/40">No cake picked in the bag. Design to be discussed directly.</p>
          )}
        </div>
      </div>

      {order.design && <p className="mt-4 whitespace-pre-wrap rounded-xl bg-white/[0.04] p-3 text-sm leading-relaxed text-white/75">{order.design}</p>}

      <InspirationPhotos names={order.photos || []} customer={order.name} />

      <div className="mt-4">
        <label htmlFor={`note-${order.id}`} className="text-[10px] uppercase tracking-[0.14em] text-white/40">
          Private note
        </label>
        <Textarea
          id={`note-${order.id}`}
          value={note}
          onChange={(event) => setNote(event.target.value)}
          rows={2}
          maxLength={1000}
          placeholder="Quoted R… confirmed for the 12th, topper ordered…"
          className="mt-1.5 border-white/10 bg-[#1c1c1f] text-sm text-white placeholder:text-white/25"
        />
        <div className="mt-2 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={saveNote}
            disabled={savingNote}
            className="inline-flex items-center gap-1.5 rounded-full bg-white px-4 py-2 text-[10px] font-medium uppercase tracking-[0.08em] text-[#141416] transition-colors hover:bg-[#b89c7d] disabled:opacity-60"
          >
            {savingNote ? <Loader2 className="h-3 w-3 animate-spin" /> : noteSaved ? <Check className="h-3 w-3" /> : null}
            {noteSaved ? "Saved" : "Save note"}
          </button>
          <button
            type="button"
            onClick={destroy}
            className="ml-auto inline-flex items-center gap-1.5 text-[10px] uppercase tracking-[0.08em] text-white/40 transition-colors hover:text-red-300"
          >
            <Trash2 className="h-3 w-3" /> Delete
          </button>
          {error && <span className="w-full text-xs text-red-300">{error}</span>}
        </div>
      </div>
    </article>
  );
}
