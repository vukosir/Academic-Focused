import { useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { ArrowRight, Check, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { MESSAGE_REASONS, sendMessage } from "@/lib/server-fns";
import { PROFILE, mailtoLink } from "@/lib/site";

const EMPTY = {
  name: "",
  email: "",
  organisation: "",
  reason: MESSAGE_REASONS[0],
  message: "",
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
  return message || `Your message couldn't be sent just now. Please try again, or email ${PROFILE.email}.`;
}

export default function ContactForm() {
  const send = useServerFn(sendMessage);
  const [form, setForm] = useState(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (form.name.trim().length < 2) return setError("Please add your name.");
    if (!form.email.trim()) return setError("Please add your email so I can reply.");
    if (form.message.trim().length < 10) return setError("Please write a short message (at least 10 characters).");

    setSubmitting(true);
    try {
      const { reference } = await send({ data: form });
      setConfirmation({ ...form, reference });
      setForm(EMPTY);
    } catch (err) {
      setError(readError(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (confirmation) {
    return (
      <div className="rounded-[26px] border border-border bg-card p-8 md:p-10" role="status">
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-secondary">
          <Check className="h-5 w-5" />
        </span>
        <h2 className="mt-6 font-heading text-2xl font-semibold tracking-[-0.03em]">Thanks, {confirmation.name.split(" ")[0]}. Your message is with me.</h2>
        <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
          Your reference is <strong className="text-foreground">{confirmation.reference}</strong>. I&rsquo;ll reply to{" "}
          <strong className="text-foreground">{confirmation.email}</strong>. If it&rsquo;s urgent, you can also email me directly.
        </p>
        <div className="mt-7 flex flex-wrap gap-3">
          <a
            href={mailtoLink(`${confirmation.reason} (ref ${confirmation.reference})`)}
            className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            Email me <ArrowRight className="h-3.5 w-3.5" />
          </a>
          <button
            type="button"
            onClick={() => setConfirmation(null)}
            className="inline-flex items-center rounded-full border border-border px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
          >
            Send another message
          </button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-[26px] border border-border bg-card p-6 md:p-8" noValidate>
      <h2 className="font-heading text-xl font-semibold tracking-tight">Send me a message</h2>
      <p className="mt-1.5 text-sm text-muted-foreground">It goes straight to my inbox. I reply by email.</p>

      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        <div>
          <Label htmlFor="name">Name *</Label>
          <Input id="name" value={form.name} onChange={(event) => set("name", event.target.value)} className="mt-1.5" autoComplete="name" maxLength={80} />
        </div>
        <div>
          <Label htmlFor="email">Email *</Label>
          <Input id="email" type="email" value={form.email} onChange={(event) => set("email", event.target.value)} className="mt-1.5" autoComplete="email" maxLength={200} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="organisation">Company or organisation</Label>
          <Input
            id="organisation"
            value={form.organisation}
            onChange={(event) => set("organisation", event.target.value)}
            placeholder="Optional"
            className="mt-1.5"
            autoComplete="organization"
            maxLength={120}
          />
        </div>
      </div>

      <div className="mt-6">
        <Label>What is it about? *</Label>
        <div className="mt-2 flex flex-wrap gap-2">
          {MESSAGE_REASONS.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => set("reason", option)}
              aria-pressed={form.reason === option}
              className={`rounded-full border px-4 py-2.5 text-xs transition-colors ${
                form.reason === option ? "border-foreground bg-foreground text-background" : "border-border hover:bg-secondary"
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <Label htmlFor="message">Message *</Label>
        <Textarea
          id="message"
          value={form.message}
          onChange={(event) => set("message", event.target.value)}
          rows={6}
          placeholder="The role or project, the team, timing, and anything else that would help."
          className="mt-1.5"
          maxLength={3000}
        />
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
        disabled={submitting}
        className="mt-7 inline-flex w-full items-center justify-center gap-2 rounded-full bg-foreground px-6 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground disabled:opacity-60"
      >
        {submitting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
        {submitting ? "Sending…" : "Send message"}
      </button>
      <p className="mt-3 text-center text-[11px] text-muted-foreground">Your message is stored privately so I can reply. See the privacy page for details.</p>
    </form>
  );
}
