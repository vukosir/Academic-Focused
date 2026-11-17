import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";

export default function RangeFeature({ range, tone = "light", reverse = false }) {
  const dark = tone === "dark";
  const [main, ...rest] = range.images;

  return (
    <section className={dark ? "bg-[#111112] text-white" : "bg-background text-foreground"}>
      <div className="u-container grid items-center gap-10 py-12 md:py-16 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1fr)] lg:gap-20">
        <Reveal className={reverse ? "lg:order-2" : ""}>
          <div className={`relative mx-auto w-full max-w-[320px] sm:max-w-[380px] lg:max-w-[420px] ${reverse ? "lg:ml-0" : "lg:mr-0"}`}>
            <div className="relative aspect-[4/5] overflow-hidden rounded-[28px] bg-secondary shadow-[0_40px_80px_-40px_rgba(29,29,31,0.5)]">
              <Image src={main.src} alt={main.alt} className="h-full w-full" fittingType="fill" sizes="(min-width: 640px) 420px, 320px" />
            </div>
            {rest[0] && (
              <div className={`absolute -bottom-6 -left-4 w-24 overflow-hidden rounded-2xl ring-4 sm:w-28 lg:-left-8 lg:w-32 ${dark ? "ring-[#111112]" : "ring-background"}`}>
                <div className="aspect-square">
                  <Image src={rest[0].src} alt={rest[0].alt} className="h-full w-full" fittingType="fill" sizes="128px" />
                </div>
              </div>
            )}
            <p className={`mt-10 text-[11px] uppercase tracking-[0.2em] lg:mt-8 ${dark ? "text-white/45" : "text-muted-foreground"}`}>{range.priceLabel}</p>
          </div>
        </Reveal>

        <Reveal delay={0.1} className={reverse ? "lg:order-1" : ""}>
          <p className={`u-eyebrow ${dark ? "!text-white/50" : ""}`}>{range.eyebrow}</p>
          <h2 className="mt-4 max-w-[16ch] font-heading text-4xl font-semibold leading-[1.02] tracking-[-0.035em] sm:text-5xl">{range.name}</h2>
          <p className={`mt-3 font-serif text-xl italic ${dark ? "text-[#d9c4a9]" : "text-[hsl(var(--gold-ink))]"}`}>{range.tagline}</p>
          <p className={`mt-5 max-w-md text-[15px] leading-relaxed ${dark ? "text-white/65" : "text-muted-foreground"}`}>{range.summary}</p>

          <ul className={`mt-7 divide-y ${dark ? "divide-white/12" : "divide-border"}`}>
            {range.options.map((option) => (
              <li key={option.name} className="flex items-baseline justify-between gap-6 py-3">
                <span className="text-sm font-medium">{option.name}</span>
                <span className={`shrink-0 text-xs ${dark ? "text-white/55" : "text-muted-foreground"}`}>{option.price}</span>
              </li>
            ))}
          </ul>

          <div className="mt-8 flex flex-wrap items-center gap-5">
            <Link
              to="/cakes/$slug"
              params={{ slug: range.slug }}
              className={`inline-flex items-center gap-2 rounded-full px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors ${
                dark ? "bg-white text-[#111112] hover:bg-[#b89c7d] hover:text-[#111112]" : "bg-foreground text-background hover:bg-accent hover:text-accent-foreground"
              }`}
            >
              Explore {range.shortName} <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <Link
              to="/compare"
              className={`text-[11px] font-medium uppercase tracking-[0.08em] underline-offset-8 hover:underline ${dark ? "text-white/60" : "text-muted-foreground"}`}
            >
              Compare
            </Link>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
