import { createFileRoute, Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { CAKE_RANGES } from "@/lib/cakes";
import { BAKERY } from "@/lib/site";

export const Route = createFileRoute("/_site/about")({
  head: () => ({
    meta: [
      { title: "About | Indulge Cakes Bakery" },
      {
        name: "description",
        content: "Indulge Cakes Bakery is a custom cake bakery run by Innocentia N., baking made-to-order cakes for Johannesburg and Pretoria.",
      },
    ],
  }),
  component: AboutPage,
});

function AboutPage() {
  const celebration = CAKE_RANGES[1];
  const bento = CAKE_RANGES[0];

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-16 pt-8 md:pb-24 md:pt-12">
          <Reveal>
            <p className="u-eyebrow">About Indulge</p>
            <h1 className="mt-3 max-w-[18ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              A custom bakery, one cake at a time.
            </h1>
          </Reveal>

          <div className="mt-12 grid gap-12 lg:grid-cols-[1.15fr_1fr] lg:gap-20">
            <Reveal delay={0.05}>
              <div className="space-y-5 text-[15px] leading-relaxed text-muted-foreground">
                <p>
                  Indulge Cakes Bakery is run by {BAKERY.baker}, baking for {BAKERY.areas}. There is no shelf of ready-made cakes here. Every cake is made to
                  order, in the design the customer chooses.
                </p>
                <p>
                  That means the cake starts with a conversation: the occasion, the colours, the design you have in mind. From a boxed bento for two to a tiered
                  celebration cake finished with gold toppers, sugar flowers and pearls, it is built around your moment rather than picked from a catalogue.
                </p>
                <p>The design, date and final price are confirmed directly with Indulge, usually over WhatsApp. No payment is taken on this website.</p>
              </div>

              <blockquote className="mt-10 border-l border-accent pl-6">
                <p className="font-serif text-2xl italic leading-snug text-foreground">“Where the sweetness meets creativity.”</p>
                <footer className="mt-3 text-[11px] uppercase tracking-[0.16em] text-muted-foreground">Indulge Cakes Bakery</footer>
              </blockquote>

              <div className="mt-10 grid gap-4 sm:grid-cols-2">
                {[
                  { title: "Made to order", body: "Nothing is baked before you order it." },
                  { title: "Any design", body: "Your idea, your colours, your cake." },
                  { title: "Hand-finished", body: "Toppers, sugar flowers and pearls, done by hand." },
                  { title: "Direct contact", body: "You speak to the baker, not a call centre." },
                ].map((value) => (
                  <div key={value.title} className="rounded-2xl border border-border p-5">
                    <h2 className="font-heading text-base font-semibold tracking-tight">{value.title}</h2>
                    <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{value.body}</p>
                  </div>
                ))}
              </div>
            </Reveal>

            <Reveal delay={0.1}>
              <div className="mx-auto grid w-full max-w-[420px] gap-4 lg:mr-0">
                <div className="relative aspect-[4/5] overflow-hidden rounded-[28px] bg-secondary">
                  <Image src={celebration.hero.src} alt={celebration.hero.alt} className="h-full w-full" fittingType="fill" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="relative aspect-square overflow-hidden rounded-[22px] bg-secondary">
                    <Image src={bento.hero.src} alt={bento.hero.alt} className="h-full w-full" fittingType="fill" sizes="300px" />
                  </div>
                  <div className="relative aspect-square overflow-hidden rounded-[22px] bg-secondary">
                    <Image src={CAKE_RANGES[3].hero.src} alt={CAKE_RANGES[3].hero.alt} className="h-full w-full" fittingType="fill" sizes="300px" />
                  </div>
                </div>
              </div>

              <div className="mx-auto mt-8 max-w-[420px] rounded-[22px] bg-secondary/60 p-6 lg:mr-0">
                <p className="text-[11px] uppercase tracking-[0.16em] text-muted-foreground">Where we bake for</p>
                <p className="mt-2 font-heading text-lg font-semibold tracking-tight">{BAKERY.areas}</p>
                <p className="mt-1 text-sm text-muted-foreground">Collection or delivery is confirmed with your order.</p>
                <Link
                  to="/order"
                  className="mt-5 inline-flex rounded-full bg-foreground px-5 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
                >
                  Request a quote
                </Link>
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
