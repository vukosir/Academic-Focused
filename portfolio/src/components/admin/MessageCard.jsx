import { useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { Building2, Check, Loader2, Mail, Trash2 } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { MESSAGE_STATUSES, deleteMessage, updateMessage } from "@/lib/server-fns";
import { formatDate } from "@/lib/site";

const STATUS_STYLES = {
  new: "bg-white/10 text-white",
  replied: "bg-emerald-400/15 text-emerald-200",
  archived: "bg-sky-400/15 text-sky-200",
};

export default function MessageCard({ message, onChanged }) {
  const save = useServerFn(updateMessage);
  const remove = useServerFn(deleteMessage);
  const [note, setNote] = useState(message.admin_note || "");
  const [noteSaved, setNoteSaved] = useState(false);
  const [savingNote, setSavingNote] = useState(false);
  const [savingStatus, setSavingStatus] = useState(false);
  const [error, setError] = useState("");

  async function changeStatus(status) {
    setSavingStatus(true);
    setError("");
    try {
      await save({ data: { id: message.id, status } });
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
      await save({ data: { id: message.id, admin_note: note } });
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
    if (!window.confirm(`Delete the message from ${message.name} (${message.reference})? This cannot be undone.`)) return;
    try {
      await remove({ data: { id: message.id } });
      onChanged();
    } catch {
      setError("Could not delete this message. Try again.");
    }
  }

  const replySubject = encodeURIComponent(`Re: ${message.reason} (ref ${message.reference})`);

  return (
    <article className="rounded-2xl border border-white/10 bg-[#141416] p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-heading text-lg font-semibold tracking-tight text-white">{message.name}</h2>
            <span className={`rounded-full px-2.5 py-1 text-[10px] uppercase tracking-[0.1em] ${STATUS_STYLES[message.status] || "bg-white/10 text-white"}`}>
              {message.status}
            </span>
          </div>
          <p className="mt-1 text-xs text-white/50">
            {message.reason} · {formatDate(message.created_date)}
            {message.reference ? ` · ${message.reference}` : ""}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {savingStatus && <Loader2 className="h-3.5 w-3.5 animate-spin text-white/50" />}
          <Select value={message.status} onValueChange={changeStatus}>
            <SelectTrigger className="h-9 w-[132px] border-white/15 bg-[#1c1c1f] text-xs capitalize text-white" aria-label="Status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {MESSAGE_STATUSES.map((status) => (
                <SelectItem key={status} value={status} className="text-xs capitalize">
                  {status}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <a
            href={`mailto:${message.email}?subject=${replySubject}`}
            aria-label={`Reply to ${message.name} by email`}
            className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-300 transition-colors hover:bg-emerald-500/25"
          >
            <Mail className="h-4 w-4" />
          </a>
        </div>
      </div>

      <div className="mt-4 grid gap-1 text-xs text-white/60">
        <p className="truncate">
          <span className="text-white/40">Email </span>
          <a href={`mailto:${message.email}`} className="text-white/90 hover:underline">
            {message.email}
          </a>
        </p>
        {message.organisation && (
          <p className="flex items-center gap-1.5">
            <Building2 className="h-3.5 w-3.5 text-white/40" />
            {message.organisation}
          </p>
        )}
      </div>

      <p className="mt-4 whitespace-pre-wrap rounded-xl bg-white/[0.04] p-3 text-sm leading-relaxed text-white/75">{message.message}</p>

      <div className="mt-4">
        <label htmlFor={`note-${message.id}`} className="text-[10px] uppercase tracking-[0.14em] text-white/40">
          Private note
        </label>
        <Textarea
          id={`note-${message.id}`}
          value={note}
          onChange={(event) => setNote(event.target.value)}
          rows={2}
          maxLength={1000}
          placeholder="Replied on the 12th, interview booked…"
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
