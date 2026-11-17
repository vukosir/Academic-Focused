import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import AddToBag from "@/components/site/AddToBag";
import GalleryGrid from "@/components/site/GalleryGrid";
import RevealLinks from "@/components/site/RevealLinks";
import { CAKE_RANGES, getRange } from "@/lib/cakes";

export const Route = createFileRoute("/_site/cakes/$slug")({
  loader: ({ params }) => {
    const range = getRange(params.slug);
    if (!range) throw notFound();
    return { range, related: CAKE_RANGES.filter((item) => item.slug !== params.slug).slice(0, 3) };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.range?.name ?? "Cakes"} | Indulge Cakes Bakery` },
      { name: "description", content: loaderData?.range?.summary ?? "Custom cakes made to order in Johannesburg & Pretoria." },
    ],
  }),
  component: CakeRangePage,
});

function CakeRangePage() {
  const { range, related } = Route.useLoaderData();

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Link to="/cakes" className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> All cakes
          </Link>

          <div className="mt-8 grid gap-10 lg:grid-cols-[1.05fr_1fr] lg:items-center lg:gap-16">
            <Reveal>
              <p className="u-eyebrow">{range.eyebrow}</p>
              <h1 className="mt-4 max-w-[16ch] font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-[56px]">{range.name}</h1>
              <p className="mt-3 font-serif text-2xl italic text-[hsl(var(--gold-ink))]">{range.tagline}</p>
              <p className="mt-6 max-w-lg text-[15px] leading-relaxed text-muted-foreground">{range.intro}</p>
              <p className="mt-6 inline-flex rounded-full bg-secondary px-4 py-2 text-[11px] font-medium uppercase tracking-[0.12em]">{range.priceLabel}</p>
              <ul className="mt-8 grid gap-2.5">
                {range.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-3 text-sm text-foreground/80">
                    <span className="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-accent" />
                    {feature}
                  </li>
                ))}
              </ul>
            </Reveal>

            <Reveal delay={0.08}>
              <div className="relative mx-auto aspect-[4/5] w-full max-w-[320px] overflow-hidden rounded-[28px] bg-secondary shadow-[0_40px_90px_-50px_rgba(29,29,31,0.55)] sm:max-w-[380px] lg:mr-0 lg:max-w-[420px]">
                <Image src={range.hero.src} alt={range.hero.alt} className="h-full w-full" fittingType="fill" priority sizes="(min-width: 640px) 420px, 320px" />
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="bg-secondary/40">
        <div className="u-container grid gap-8 py-16 md:py-20 lg:grid-cols-[1fr_1fr] lg:items-start lg:gap-16">
          <Reveal>
            <AddToBag range={range} />
          </Reveal>
          <Reveal delay={0.08} className="lg:pt-4">
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em]">What happens next</h2>
            <ol className="mt-6 space-y-5">
              {[
                "Add your cakes and palette to the bag.",
                "Send your design brief with the date, occasion and any inspiration photos.",
                "Indulge replies with the design, date and final price, usually on WhatsApp.",
              ].map((line, index) => (
                <li key={line} className="flex gap-4">
                  <span className="font-heading text-xs font-semibold tracking-[0.14em] text-[hsl(var(--gold-ink))]">0{index + 1}</span>
                  <p className="text-sm leading-relaxed text-muted-foreground">{line}</p>
                </li>
              ))}
            </ol>
            <Link
              to="/order"
              className="mt-8 inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              Request a quote <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container py-16 md:py-24">
          <Reveal>
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">{range.shortName} we&rsquo;ve made</h2>
          </Reveal>
          <div className="mt-8">
            <GalleryGrid images={range.images} />
          </div>
        </div>
      </section>

      <RevealLinks
        title="Other ranges"
        items={related.map((item) => ({
          to: `/cakes/${item.slug}`,
          label: item.name,
          hint: item.priceLabel,
          image: item.hero,
        }))}
      />
    </>
  );
}
