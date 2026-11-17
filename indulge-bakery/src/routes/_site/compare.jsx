import { createFileRoute, Link } from "@tanstack/react-router";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { CAKE_RANGES } from "@/lib/cakes";

export const Route = createFileRoute("/_site/compare")({
  head: () => ({
    meta: [
      { title: "Compare the range | Indulge Cakes Bakery" },
      {
        name: "description",
        content: "Compare Indulge's bento, celebration and kids' cakes, cupcakes, biscuits and scones side by side: starting prices and options.",
      },
    ],
  }),
  component: ComparePage,
});

function ComparePage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">Compare</p>
            <h1 className="mt-3 max-w-[22ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              Which cake is yours?
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              Every cake is made to order in any design you choose. The difference is size, the moment it is made for, and what gets built around it.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Reveal>
            <div className="overflow-x-auto rounded-[26px] border border-border bg-card">
              <table className="w-full min-w-[860px] border-collapse text-left">
                <thead>
                  <tr className="border-b border-border">
                    <th className="w-[24%] px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Cake</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">From</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Options</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Made for</th>
                  </tr>
                </thead>
                <tbody>
                  {CAKE_RANGES.map((range) => (
                    <tr key={range.slug} className="border-b border-border/70 last:border-0">
                      <th scope="row" className="px-6 py-6 align-top">
                        <Link to="/cakes/$slug" params={{ slug: range.slug }} className="font-heading text-base font-semibold tracking-tight underline-offset-4 hover:underline">
                          {range.name}
                        </Link>
                        <p className="mt-1 text-xs font-normal text-muted-foreground">{range.eyebrow}</p>
                      </th>
                      <td className="px-6 py-6 align-top text-sm">{range.priceLabel}</td>
                      <td className="px-6 py-6 align-top">
                        <ul className="space-y-1 text-sm text-foreground/80">
                          {range.options.map((option) => (
                            <li key={option.name}>{option.name}</li>
                          ))}
                        </ul>
                      </td>
                      <td className="px-6 py-6 align-top text-sm text-muted-foreground">{range.summary}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-4 text-xs text-muted-foreground">
              Starting prices are published for bento and celebration cakes. Every other option is quoted per order once size, design and servings are confirmed.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {CAKE_RANGES.map((range, index) => (
              <Reveal key={range.slug} delay={index * 0.04}>
                <Link
                  to="/cakes/$slug"
                  params={{ slug: range.slug }}
                  className="flex h-full flex-col justify-between rounded-2xl border border-border p-5 transition-colors hover:border-foreground/25"
                >
                  <span className="font-heading text-base font-semibold tracking-tight">{range.name}</span>
                  <span className="mt-6 text-[11px] uppercase tracking-[0.12em] text-muted-foreground">{range.priceLabel}</span>
                </Link>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
