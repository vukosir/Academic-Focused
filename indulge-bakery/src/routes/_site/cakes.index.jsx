import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { CAKE_RANGES } from "@/lib/cakes";

export const Route = createFileRoute("/_site/cakes/")({
  head: () => ({
    meta: [
      { title: "All cakes | Indulge Cakes Bakery" },
      {
        name: "description",
        content:
          "Bento cakes from R400, celebration cakes from R750, kids' theme cakes from R400, cupcakes, biscuits and scones, made to order in Johannesburg & Pretoria.",
      },
    ],
  }),
  component: AllCakesPage,
});

function AllCakesPage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">The range</p>
            <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              Cakes, made to order.
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              Five ranges, every one baked to order in the design you choose. Only the starting prices below are published. Everything else is quoted once we
              know your design, size and date.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-16 pt-6 md:pb-24 md:pt-8">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {CAKE_RANGES.map((range, index) => (
              <Reveal key={range.slug} delay={index * 0.05} className="h-full">
                <Link
                  to="/cakes/$slug"
                  params={{ slug: range.slug }}
                  className="group flex h-full flex-col overflow-hidden rounded-[26px] bg-card ring-1 ring-border transition-shadow duration-500 hover:shadow-[0_30px_70px_-40px_rgba(29,29,31,0.45)]"
                >
                  <div className="relative aspect-[4/3] overflow-hidden bg-secondary">
                    <Image
                      src={range.hero.src}
                      alt={range.hero.alt}
                      className="absolute inset-0 h-full w-full transition-transform duration-700 group-hover:scale-[1.04]"
                      fittingType="fill"
                      sizes="(min-width: 1280px) 260px, (min-width: 1024px) 400px, (min-width: 640px) 50vw, 100vw"
                    />
                  </div>
                  <div className="flex flex-1 flex-col justify-between gap-5 p-5">
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.16em] text-muted-foreground">{range.eyebrow}</p>
                      <h2 className="mt-2 font-heading text-xl font-semibold leading-tight tracking-[-0.03em]">{range.name}</h2>
                      <p className="mt-1 font-serif text-base italic text-[hsl(var(--gold-ink))]">{range.tagline}</p>
                      <p className="mt-2.5 line-clamp-3 text-[13px] leading-relaxed text-muted-foreground">{range.summary}</p>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] uppercase tracking-[0.14em] text-muted-foreground">{range.priceLabel}</span>
                      <span className="inline-flex items-center gap-1.5 text-[11px] font-medium uppercase tracking-[0.08em]">
                        View range
                        <ArrowRight className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-1" />
                      </span>
                    </div>
                  </div>
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
