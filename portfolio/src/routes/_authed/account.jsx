import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import SiteHeader from "@/components/site/SiteHeader";
import SiteFooter from "@/components/site/SiteFooter";
import SecuritySettings from "@/components/account/SecuritySettings";
import { useAuth } from "@/lib/AuthContext";

export const Route = createFileRoute("/_authed/account")({
  ssr: false,
  head: () => ({ meta: [{ title: "Account | Vukosi Rikhotso" }] }),
  component: AccountPage,
});

function AccountPage() {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === "admin";

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="flex-1">
        <div className="u-container max-w-4xl pb-24 pt-8 md:pt-12">
          <p className="u-eyebrow">Account</p>
          <div className="mt-5 flex flex-wrap items-end justify-between gap-4">
            <h1 className="font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl">
              Hi, {user?.full_name || user?.email?.split("@")[0]}.
            </h1>
            <button onClick={() => logout()} className="text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
              Sign out
            </button>
          </div>

          {isAdmin && (
            <div className="mt-10 flex flex-wrap items-center justify-between gap-4 rounded-[26px] border border-border bg-card p-6">
              <div>
                <h2 className="font-heading text-lg font-semibold tracking-tight">Messages inbox</h2>
                <p className="mt-1 text-sm text-muted-foreground">Messages sent through the contact page. Needs two-step verification.</p>
              </div>
              <Link
                to="/admin"
                className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-background"
              >
                Open inbox <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          )}

          <SecuritySettings />
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
