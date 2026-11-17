import { useEffect, useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { Check, Copy, Loader2, Monitor, ShieldCheck, ShieldOff } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { listMySessions, mfaBeginSetup, mfaConfirmSetup, mfaDisable, mfaStatus, signOutOtherDevices } from "@/lib/auth-fns";
import { formatDate } from "@/lib/site";

const btn =
  "inline-flex items-center justify-center gap-2 rounded-full px-5 py-2.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors disabled:opacity-60";

function readError(err, fallback) {
  try {
    const issues = JSON.parse(err?.message);
    if (Array.isArray(issues) && issues[0]?.message) return issues[0].message;
  } catch {
    /* not JSON */
  }
  return err?.message || fallback;
}

// Two-step verification (authenticator app codes) and signed-in devices.
export default function SecuritySettings() {
  const getStatus = useServerFn(mfaStatus);
  const begin = useServerFn(mfaBeginSetup);
  const confirm = useServerFn(mfaConfirmSetup);
  const disable = useServerFn(mfaDisable);
  const getSessions = useServerFn(listMySessions);
  const signOutOthers = useServerFn(signOutOtherDevices);

  const [status, setStatus] = useState(null);
  const [setup, setSetup] = useState(null); // { secret, uri }
  const [code, setCode] = useState("");
  const [recovery, setRecovery] = useState(null);
  const [copied, setCopied] = useState(false);
  const [disabling, setDisabling] = useState(false);
  const [password, setPassword] = useState("");
  const [sessions, setSessions] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [note, setNote] = useState("");

  const refresh = async () => {
    const [s, list] = await Promise.all([getStatus(), getSessions()]);
    setStatus(s);
    setSessions(list);
  };

  useEffect(() => {
    refresh().catch(() => setError("Your security settings could not be loaded."));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function run(action) {
    setBusy(true);
    setError("");
    setNote("");
    try {
      await action();
    } catch (err) {
      setError(readError(err, "Something went wrong. Please try again."));
    } finally {
      setBusy(false);
    }
  }

  const startSetup = () => run(async () => setSetup(await begin()));
  const finishSetup = (e) => {
    e.preventDefault();
    run(async () => {
      const { recoveryCodes } = await confirm({ data: { code } });
      setRecovery(recoveryCodes);
      setSetup(null);
      setCode("");
      await refresh();
    });
  };
  const turnOff = (e) => {
    e.preventDefault();
    run(async () => {
      await disable({ data: { password, code } });
      setDisabling(false);
      setPassword("");
      setCode("");
      setNote("Two-step verification is off.");
      await refresh();
    });
  };
  const endOthers = () =>
    run(async () => {
      const { count } = await signOutOthers();
      setNote(count ? `Signed out of ${count} other device${count === 1 ? "" : "s"}.` : "No other devices were signed in.");
      await refresh();
    });

  async function copyCodes() {
    try {
      await navigator.clipboard.writeText(recovery.join("\n"));
      setCopied(true);
    } catch {
      /* clipboard blocked: the codes are on screen to write down */
    }
  }

  return (
    <section id="security" className="mt-14 scroll-mt-24">
      <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em]">Security</h2>

      <div className="mt-6 rounded-[26px] border border-border bg-card p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex gap-3">
            {status?.enabled ? <ShieldCheck className="mt-0.5 h-5 w-5 text-emerald-600" /> : <ShieldOff className="mt-0.5 h-5 w-5 text-muted-foreground" />}
            <div>
              <p className="font-medium">Two-step verification {status ? (status.enabled ? "is on" : "is off") : ""}</p>
              <p className="mt-1 max-w-md text-sm text-muted-foreground">
                {status?.enabled
                  ? `Signing in needs your password and a code from your authenticator app. ${status.recoveryCodesLeft} recovery code${status.recoveryCodesLeft === 1 ? "" : "s"} left.`
                  : "Add a 6-digit code from an authenticator app (Google Authenticator, Microsoft Authenticator or Apple Passwords) to every sign-in. Required to open the messages inbox."}
              </p>
            </div>
          </div>
          {status && !status.enabled && !setup && !recovery && (
            <button type="button" onClick={startSetup} disabled={busy} className={`${btn} bg-foreground text-background hover:bg-accent hover:text-accent-foreground`}>
              {busy && <Loader2 className="h-3.5 w-3.5 animate-spin" />} Turn on
            </button>
          )}
          {status?.enabled && !disabling && (
            <button type="button" onClick={() => setDisabling(true)} className={`${btn} border border-border hover:bg-secondary`}>
              Turn off
            </button>
          )}
        </div>

        {setup && (
          <form onSubmit={finishSetup} className="mt-6 space-y-4 border-t border-border pt-6">
            <ol className="list-decimal space-y-2 pl-5 text-sm text-muted-foreground">
              <li>Open your authenticator app and choose to add an account.</li>
              <li>
                Enter this key (time-based):{" "}
                <code className="select-all rounded-md bg-secondary px-2 py-1 font-mono text-[13px] tracking-wider text-foreground">{setup.secret}</code>
                <span className="mt-1 block">
                  On your phone you can also{" "}
                  <a href={setup.uri} className="text-foreground underline underline-offset-4">
                    open it in your authenticator app
                  </a>
                  .
                </span>
              </li>
              <li>Type the 6-digit code the app shows.</li>
            </ol>
            <div className="max-w-xs space-y-1.5">
              <Label htmlFor="mfa-code">Code from the app</Label>
              <Input id="mfa-code" inputMode="numeric" autoComplete="one-time-code" maxLength={10} value={code} onChange={(e) => setCode(e.target.value)} className="h-11 tracking-[0.25em]" required />
            </div>
            <div className="flex gap-3">
              <button type="submit" disabled={busy} className={`${btn} bg-foreground text-background hover:bg-accent hover:text-accent-foreground`}>
                {busy && <Loader2 className="h-3.5 w-3.5 animate-spin" />} Confirm
              </button>
              <button type="button" onClick={() => setSetup(null)} className={`${btn} border border-border hover:bg-secondary`}>
                Cancel
              </button>
            </div>
          </form>
        )}

        {recovery && (
          <div className="mt-6 rounded-2xl border border-amber-300 bg-amber-50 p-5 text-amber-950">
            <p className="font-medium">Save your recovery codes</p>
            <p className="mt-1 text-sm">
              Each code signs you in once if you lose your phone. They won't be shown again, so copy them somewhere safe (a password manager is ideal).
            </p>
            <ul className="mt-4 grid grid-cols-2 gap-2 font-mono text-sm sm:grid-cols-4">
              {recovery.map((c) => (
                <li key={c} className="rounded-lg bg-white px-2 py-1.5 text-center">
                  {c}
                </li>
              ))}
            </ul>
            <div className="mt-4 flex flex-wrap gap-3">
              <button type="button" onClick={copyCodes} className={`${btn} border border-amber-400 bg-white hover:bg-amber-100`}>
                {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />} {copied ? "Copied" : "Copy codes"}
              </button>
              <button type="button" onClick={() => setRecovery(null)} className={`${btn} bg-amber-900 text-white hover:bg-amber-950`}>
                I've saved them
              </button>
            </div>
          </div>
        )}

        {disabling && (
          <form onSubmit={turnOff} className="mt-6 grid max-w-md gap-4 border-t border-border pt-6">
            <p className="text-sm text-muted-foreground">To turn it off, confirm your password and a current code (or a recovery code).</p>
            <div className="space-y-1.5">
              <Label htmlFor="mfa-off-password">Password</Label>
              <Input id="mfa-off-password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} className="h-11" required />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="mfa-off-code">Code</Label>
              <Input id="mfa-off-code" inputMode="numeric" autoComplete="one-time-code" maxLength={20} value={code} onChange={(e) => setCode(e.target.value)} className="h-11" required />
            </div>
            <div className="flex gap-3">
              <button type="submit" disabled={busy} className={`${btn} bg-destructive text-destructive-foreground hover:opacity-90`}>
                {busy && <Loader2 className="h-3.5 w-3.5 animate-spin" />} Turn off
              </button>
              <button type="button" onClick={() => setDisabling(false)} className={`${btn} border border-border hover:bg-secondary`}>
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>

      <div className="mt-4 rounded-[26px] border border-border bg-card p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="font-medium">Where you're signed in</p>
            <p className="mt-1 text-sm text-muted-foreground">Don't recognise a device? Sign it out, then change your password.</p>
          </div>
          {sessions.length > 1 && (
            <button type="button" onClick={endOthers} disabled={busy} className={`${btn} border border-border hover:bg-secondary`}>
              Sign out other devices
            </button>
          )}
        </div>
        <ul className="mt-4 divide-y divide-border">
          {sessions.map((s) => (
            <li key={s.id} className="flex items-center gap-3 py-3 text-sm">
              <Monitor className="h-4 w-4 text-muted-foreground" />
              <span className="flex-1">{s.device}</span>
              <span className="text-xs text-muted-foreground">{s.current ? "This device" : `Since ${formatDate(s.created_date)}`}</span>
            </li>
          ))}
        </ul>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-xl bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </p>
      )}
      {note && (
        <p role="status" className="mt-4 rounded-xl bg-secondary px-4 py-3 text-sm">
          {note}
        </p>
      )}
    </section>
  );
}
