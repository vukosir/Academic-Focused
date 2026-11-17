import { useCallback, useEffect, useMemo, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useServerFn } from "@tanstack/react-start";
import { Loader2, LogOut, ShieldAlert } from "lucide-react";
import OrderCard from "@/components/admin/OrderCard";
import { useAuth } from "@/lib/AuthContext";
import { listOrders } from "@/lib/server-fns";

export const Route = createFileRoute("/_authed/admin")({
  head: () => ({ meta: [{ title: "Order requests | Indulge" }] }),
  component: AdminOrders,
});

const FILTERS = ["all", "new", "quoted", "confirmed", "completed", "cancelled"];

function AdminOrders() {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === "admin";
  const needsMfa = isAdmin && !user?.mfa_enabled;
  const fetchOrders = useServerFn(listOrders);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("all");

  const load = useCallback(async () => {
    setError("");
    try {
      const rows = await fetchOrders();
      // Earliest date needed first.
      setOrders([...rows].sort((a, b) => String(a.date_needed).localeCompare(String(b.date_needed))));
    } catch {
      setError("The order requests could not be loaded. Try again.");
    } finally {
      setLoading(false);
    }
  }, [fetchOrders]);

  useEffect(() => {
    if (isAdmin && !needsMfa) load();
  }, [isAdmin, needsMfa, load]);

  const counts = useMemo(
    () =>
      FILTERS.reduce((acc, key) => {
        acc[key] = key === "all" ? orders.length : orders.filter((order) => order.status === key).length;
        return acc;
      }, {}),
    [orders]
  );

  const shown = filter === "all" ? orders : orders.filter((order) => order.status === filter);

  return (
    <div className="min-h-screen bg-[#0b0b0d] text-white">
      <header className="sticky top-0 z-20 border-b border-white/10 bg-[#0b0b0d]/90 backdrop-blur-xl">
        <div className="mx-auto flex w-full max-w-5xl items-center justify-between gap-4 px-5 py-4 sm:px-8">
          <div>
            <Link to="/" className="inline-flex items-baseline gap-1.5">
              <span className="font-heading text-lg font-semibold tracking-[-0.04em]">indulge</span>
              <span className="text-[10px] uppercase tracking-[0.24em] text-white/40">cakes</span>
            </Link>
            <p className="mt-0.5 text-[10px] uppercase tracking-[0.18em] text-white/40">Owner · order requests</p>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/" className="hidden text-[11px] uppercase tracking-[0.1em] text-white/50 hover:text-white sm:block">
              View site
            </Link>
            <button
              type="button"
              onClick={() => logout()}
              className="inline-flex items-center gap-1.5 rounded-full border border-white/15 px-4 py-2 text-[11px] uppercase tracking-[0.08em] text-white/70 transition-colors hover:border-white/40 hover:text-white"
            >
              <LogOut className="h-3.5 w-3.5" /> Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl px-5 py-10 sm:px-8">
        {needsMfa ? (
          <div className="rounded-2xl border border-white/10 bg-[#141416] p-8 text-center">
            <ShieldAlert className="mx-auto h-6 w-6 text-amber-300" />
            <h1 className="mt-4 font-heading text-xl font-semibold tracking-tight">Turn on two-step verification first.</h1>
            <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-white/60">
              The orders inbox holds customers' names, numbers and photos, so it needs a code from your authenticator app as well as your password.
            </p>
            <Link to="/dashboard" hash="security" className="mt-6 inline-flex rounded-full bg-white px-5 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-[#141416]">
              Set it up now
            </Link>
          </div>
        ) : !isAdmin ? (
          <div className="rounded-2xl border border-white/10 bg-[#141416] p-8 text-center">
            <ShieldAlert className="mx-auto h-6 w-6 text-amber-300" />
            <h1 className="mt-4 font-heading text-xl font-semibold tracking-tight">This page is for the bakery owner.</h1>
            <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-white/60">
              Your account is signed in, but it does not have admin access to the order requests.
            </p>
            <Link to="/" className="mt-6 inline-flex rounded-full bg-white px-5 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-[#141416]">
              Back to the bakery
            </Link>
          </div>
        ) : (
          <>
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <h1 className="font-heading text-3xl font-semibold tracking-[-0.035em]">Order requests</h1>
                <p className="mt-1.5 text-sm text-white/50">Sorted by the date the cake is needed, earliest first.</p>
              </div>
              <p className="text-xs text-white/40">
                {counts.new} new · {orders.length} total
              </p>
            </div>

            <div className="mt-6 flex flex-wrap gap-2">
              {FILTERS.map((key) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setFilter(key)}
                  aria-pressed={filter === key}
                  className={`rounded-full border px-4 py-2 text-[11px] uppercase tracking-[0.08em] transition-colors ${
                    filter === key ? "border-white bg-white text-[#141416]" : "border-white/15 text-white/60 hover:border-white/40 hover:text-white"
                  }`}
                >
                  {key} {counts[key] > 0 ? `(${counts[key]})` : ""}
                </button>
              ))}
            </div>

            {loading ? (
              <div className="mt-16 flex justify-center">
                <Loader2 className="h-5 w-5 animate-spin text-white/40" />
              </div>
            ) : error ? (
              <div className="mt-10 rounded-2xl border border-red-500/20 bg-red-500/5 p-6 text-sm text-red-200">
                {error}
                <button type="button" onClick={load} className="ml-3 underline underline-offset-4">
                  Try again
                </button>
              </div>
            ) : shown.length === 0 ? (
              <p className="mt-16 text-center text-sm text-white/45">No {filter === "all" ? "" : filter} requests right now.</p>
            ) : (
              <div className="mt-8 space-y-4">
                {shown.map((order) => (
                  <OrderCard key={order.id} order={order} onChanged={load} />
                ))}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
