import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import SiteHeader from "@/components/site/SiteHeader";
import SiteFooter from "@/components/site/SiteFooter";
import SecuritySettings from "@/components/account/SecuritySettings";
import { listMyOrders } from "@/lib/server-fns";
import { useAuth } from "@/lib/AuthContext";
import { getRange } from "@/lib/cakes";
import { formatDate, whatsappLink } from "@/lib/site";

export const Route = createFileRoute("/_authed/dashboard")({
  ssr: false,
  loader: async () => ({ orders: await listMyOrders() }),
  head: () => ({ meta: [{ title: "My orders | Indulge Cakes Bakery" }] }),
  component: DashboardPage,
});

const STATUS_TEXT = {
  new: "Received. We'll be in touch",
  quoted: "Quote sent",
  confirmed: "Confirmed",
  completed: "Completed",
  cancelled: "Cancelled",
};

function DashboardPage() {
  const { orders } = Route.useLoaderData();
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="flex-1">
        <div className="u-container max-w-4xl pb-24 pt-8 md:pt-12">
          <p className="u-eyebrow">My account</p>
          <div className="mt-5 flex flex-wrap items-end justify-between gap-4">
            <h1 className="font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl">
              Hi, {user?.full_name || user?.email?.split("@")[0]}.
            </h1>
            <button onClick={() => logout()} className="text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
              Sign out
            </button>
          </div>

          <h2 className="mt-12 font-heading text-2xl font-semibold tracking-[-0.03em]">My quote requests</h2>
          {orders.length === 0 ? (
            <div className="mt-6 rounded-[26px] border border-dashed border-border px-6 py-14 text-center">
              <p className="text-sm text-muted-foreground">You haven't sent a quote request while signed in yet.</p>
              <Link
                to="/cakes"
                className="mt-5 inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-background"
              >
                Browse the cakes <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          ) : (
            <ul className="mt-6 space-y-4">
              {orders.map((o) => (
                <li key={o.id} className="rounded-[26px] border border-border bg-card p-6">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="font-heading text-lg font-semibold tracking-tight">
                        {formatDate(o.date_needed)}
                        {o.occasion ? ` · ${o.occasion}` : ""}
                      </p>
                      <p className="mt-0.5 text-xs text-muted-foreground">Ref {o.reference}</p>
                    </div>
                    <span className="rounded-full bg-secondary px-3 py-1.5 text-[10px] font-medium uppercase tracking-[0.1em]">{STATUS_TEXT[o.status] ?? o.status}</span>
                  </div>

                  {o.items?.length > 0 && (
                    <ul className="mt-4 divide-y divide-border border-t border-border">
                      {o.items.map((item, index) => (
                        <li key={index} className="flex items-baseline justify-between gap-4 py-2.5 text-sm">
                          <span>
                            {item.option} <span className="text-muted-foreground">· {getRange(item.slug)?.name} · {item.palette}</span>
                          </span>
                          <span className="text-xs text-muted-foreground">× {item.quantity}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                  {o.design && <p className="mt-4 whitespace-pre-wrap rounded-xl bg-secondary/60 p-3 text-sm leading-relaxed text-muted-foreground">{o.design}</p>}

                  {o.photos?.length > 0 && (
                    <div className="mt-4 flex flex-wrap gap-2">
                      {o.photos.map((name, index) => (
                        <a key={name} href={`/api/order-photos/${name}`} target="_blank" rel="noreferrer" className="block h-20 w-20 overflow-hidden rounded-xl bg-secondary">
                          <img src={`/api/order-photos/${name}`} alt={`Inspiration photo ${index + 1}`} loading="lazy" className="h-full w-full object-cover" />
                        </a>
                      ))}
                    </div>
                  )}

                  <a
                    href={whatsappLink(`Hi Indulge, I'm following up on my quote request ${o.reference}.`)}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-5 inline-flex rounded-full border border-border px-5 py-2.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
                  >
                    Ask about this request
                  </a>
                </li>
              ))}
            </ul>
          )}

          <SecuritySettings />
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
