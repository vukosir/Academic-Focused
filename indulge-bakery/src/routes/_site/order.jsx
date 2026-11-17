import { createFileRoute, Link } from "@tanstack/react-router";
import Reveal from "@/components/site/Reveal";
import QuoteForm from "@/components/site/QuoteForm";
import { useBag } from "@/components/site/BagProvider";
import { BAKERY, whatsappLink } from "@/lib/site";

export const Route = createFileRoute("/_site/order")({
  head: () => ({
    meta: [
      { title: "Request a quote | Indulge Cakes Bakery" },
      {
        name: "description",
        content: "Send Indulge your cake, palette and design brief. No payment is taken online. The design, date and final price are confirmed with you directly.",
      },
    ],
  }),
  component: OrderPage,
});

function OrderPage() {
  const { items, count } = useBag();

  return (
    <section className="bg-background">
      <div className="u-container pb-20 pt-8 md:pb-28 md:pt-12">
        <Reveal>
          <p className="u-eyebrow">Request a quote</p>
          <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
            Tell us what you&rsquo;re celebrating.
          </h1>
          <p className="mt-4 max-w-xl text-[15px] leading-relaxed text-muted-foreground">
            Add your cakes to the bag, then send the design brief below. Indulge confirms the design, the date and the final price with you before anything is
            baked.
          </p>
        </Reveal>

        <div className="mt-12 grid gap-8 lg:grid-cols-[1fr_1.15fr] lg:items-start lg:gap-14">
          <Reveal>
            <div className="rounded-[26px] border border-border bg-secondary/40 p-6 md:p-7">
              <div className="flex items-baseline justify-between">
                <h2 className="font-heading text-lg font-semibold tracking-tight">Your order bag</h2>
                <Link to="/cakes" className="text-[11px] uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
                  Add more
                </Link>
              </div>

              {items.length === 0 ? (
                <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
                  Your bag is empty. You can still send a design brief and we will work out the cake together. Or{" "}
                  <Link to="/cakes" className="text-foreground underline underline-offset-4">
                    browse the range
                  </Link>
                  .
                </p>
              ) : (
                <ul className="mt-5 divide-y divide-border">
                  {items.map((item) => (
                    <li key={item.key} className="flex items-baseline justify-between gap-4 py-3">
                      <div>
                        <p className="text-sm font-medium">{item.name}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {item.option} · {item.palette}
                        </p>
                      </div>
                      <span className="shrink-0 text-xs text-muted-foreground">× {item.quantity}</span>
                    </li>
                  ))}
                </ul>
              )}

              <div className="mt-6 flex items-baseline justify-between border-t border-border pt-5 text-sm">
                <span className="text-muted-foreground">{count > 0 ? `${count} item${count === 1 ? "" : "s"}` : "Nothing added yet"}</span>
                <span className="font-medium">Quoted per order</span>
              </div>

              <div className="mt-6 rounded-2xl bg-card p-4 text-xs leading-relaxed text-muted-foreground ring-1 ring-border">
                <p className="font-medium text-foreground">No payment is taken online.</p>
                <p className="mt-1">Design, date and final price are confirmed by {BAKERY.baker}, usually on WhatsApp.</p>
                <a
                  href={whatsappLink("Hi Indulge, I'd like to order a cake.")}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-3 inline-block text-foreground underline underline-offset-4"
                >
                  Or order on WhatsApp
                </a>
              </div>
            </div>
          </Reveal>

          <Reveal delay={0.06}>
            <QuoteForm />
          </Reveal>
        </div>
      </div>
    </section>
  );
}
