import { KeyRound, Lock, LogIn, Mail } from "lucide-react";
import { useState } from "react";
import { AuthLayout } from "@/components/auth/AuthLayout";
import { AuthError, AuthLink, SubmitButton } from "@/components/auth/parts";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login, verifyMfaLogin } from "@/lib/auth-fns";
import { safeReturnTo } from "@/lib/authReturnTo";

// The /login page. ?returnTo= is where the visitor goes after signing in.
// Accounts with two-step verification get a second step for a 6-digit code.
export function LoginPage() {
  const returnTo = safeReturnTo();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [step, setStep] = useState("password");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submitPassword(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const result = await login({ data: { email, password } });
      if (result?.mfaRequired) {
        setPassword("");
        setStep("code");
        setBusy(false);
        return;
      }
      // A hard redirect: the app re-initializes with the new session.
      window.location.href = returnTo;
    } catch (err) {
      setError(err.message || "Invalid email or password");
      setBusy(false);
    }
  }

  async function submitCode(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await verifyMfaLogin({ data: { code } });
      window.location.href = returnTo;
    } catch (err) {
      setError(err.message || "That code didn't work.");
      setBusy(false);
      if (/timed out|sign in again/i.test(err.message || "")) setStep("password");
    }
  }

  if (step === "code") {
    return (
      <AuthLayout icon={KeyRound} title="Two-step verification" subtitle="Enter the 6-digit code from your authenticator app">
        <AuthError>{error}</AuthError>
        <form onSubmit={submitCode} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="login-code">Code</Label>
            <Input
              id="login-code"
              inputMode="numeric"
              autoComplete="one-time-code"
              autoFocus
              placeholder="123 456"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              maxLength={20}
              className="h-12 text-center text-lg tracking-[0.3em]"
              required
            />
            <p className="text-xs text-muted-foreground">Lost your phone? Enter one of your recovery codes instead.</p>
          </div>
          <SubmitButton busy={busy} busyLabel="Checking...">
            Verify and sign in
          </SubmitButton>
        </form>
        <button type="button" onClick={() => setStep("password")} className="mt-6 w-full text-center text-sm text-muted-foreground hover:text-foreground">
          Back
        </button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout icon={LogIn} title="Welcome back" subtitle="Log in to your account">
      <AuthError>{error}</AuthError>
      <form onSubmit={submitPassword} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="login-email">Email</Label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input id="login-email" type="email" autoComplete="email" autoFocus placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} className="h-12 pl-10" required />
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="login-password">Password</Label>
            <AuthLink to="/forgot-password" returnTo={returnTo} className="text-xs text-primary hover:underline">
              Forgot password?
            </AuthLink>
          </div>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input id="login-password" type="password" autoComplete="current-password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} className="h-12 pl-10" required />
          </div>
        </div>
        <SubmitButton busy={busy} busyLabel="Logging in...">
          Log in
        </SubmitButton>
      </form>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        Don't have an account?{" "}
        <AuthLink to="/register" returnTo={returnTo}>
          Create one
        </AuthLink>
      </p>
    </AuthLayout>
  );
}
