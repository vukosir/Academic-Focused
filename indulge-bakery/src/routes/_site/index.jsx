import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import Hero from "@/components/site/Hero";
import RangeFeature from "@/components/site/RangeFeature";
import RangeDuo from "@/components/site/RangeDuo";
import CollectionsRail from "@/components/site/CollectionsRail";
import GalleryGrid from "@/components/site/GalleryGrid";
import HowItWorks from "@/components/site/HowItWorks";
import PaletteStrip from "@/components/site/PaletteStrip";
import ClosingCta from "@/components/site/ClosingCta";
import Reveal from "@/components/site/Reveal";
import { CAKE_RANGES, GALLERY } from "@/lib/cakes";

export const Route = createFileRoute("/_site/")({
  head: () => ({
    meta: [
      { title: "Indulge Cakes Bakery | Custom cakes in Johannesburg & Pretoria" },
      {
        name: "description",
        content:
          "Custom cakes made to order in any design you choose. Bento cakes from R400, celebration cakes from R750, kids' theme cakes, cupcakes, biscuits and scones. Johannesburg & Pretoria.",
      },
    ],
  }),
  component: HomePage,
});

function HomePage() {
  const range = (slug) => CAKE_RANGES.find((item) => item.slug === slug);

  return (
    <>
      <Hero />

      <section className="bg-background">
        <div className="u-container pb-2 pt-12 md:pb-4 md:pt-16">
          <Reveal>
            <div className="grid gap-6 md:grid-cols-[1.4fr_1fr] md:items-end md:gap-16">
              <h2 className="max-w-[24ch] font-heading text-[32px] font-semibold leading-[1.04] tracking-[-0.035em] sm:text-4xl md:text-5xl">
                Every cake is made to order.
                <br />
                <span className="text-muted-foreground">In any design you choose.</span>
              </h2>
              <p className="max-w-sm text-[15px] leading-relaxed text-muted-foreground">
                Indulge is a small custom bakery run by Innocentia N., baking for Johannesburg and Pretoria. Pick a range, tell us the design, and we build it
                around your celebration.
              </p>
            </div>
          </Reveal>
        </div>
      </section>

      <RangeFeature range={range("bento")} tone="light" />
      <RangeFeature range={range("celebration")} tone="dark" reverse />
      <RangeFeature range={range("kids")} tone="light" />

      <RangeDuo />
      <CollectionsRail />

      <section className="border-t border-border bg-background">
        <div className="u-container py-14 md:py-20">
          <Reveal>
            <div className="flex flex-wrap items-end justify-between gap-6">
              <div>
                <p className="u-eyebrow">Gallery</p>
                <h2 className="mt-4 max-w-[20ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl md:text-[44px]">
                  Recently out of the kitchen.
                </h2>
              </div>
              <Link
                to="/gallery"
                className="text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground underline-offset-8 hover:text-foreground hover:underline"
              >
                See every cake
              </Link>
            </div>
          </Reveal>
          <Reveal delay={0.08} className="mt-10">
            <GalleryGrid images={GALLERY.slice(0, 8)} />
          </Reveal>
        </div>
      </section>

      <HowItWorks />

      <section className="border-t border-border bg-secondary/40">
        <div className="u-container py-14 md:py-20">
          <div className="grid gap-10 lg:grid-cols-[1fr_1.3fr] lg:gap-20">
            <Reveal>
              <p className="u-eyebrow">Colour palettes</p>
              <h2 className="mt-4 max-w-[16ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl">
                Choose the palette, we build the rest.
              </h2>
              <p className="mt-5 max-w-sm text-[15px] leading-relaxed text-muted-foreground">
                Every cake starts with a palette. Pick one of ours, or send your own colours with your design brief.
              </p>
              <Link
                to="/order"
                className="mt-7 inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-foreground underline-offset-8 hover:underline"
              >
                Send a design brief <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Reveal>
            <Reveal delay={0.08}>
              <PaletteStrip />
            </Reveal>
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
