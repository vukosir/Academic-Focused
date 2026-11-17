import { Link } from "@tanstack/react-router";
import { ArrowRight, Clock3, MapPin, MessageCircle, Palette } from "lucide-react";
import { Image } from "@/components/ui/image";
import { BAKERY, whatsappLink } from "@/lib/site";

// Real cakes, each linking to its range.
const MOSAIC = [
  { src: "/images/cakes/custom-masterpiece.jpg", alt: "White and gold line-art birthday cake", label: "Celebration · from R750", to: "/cakes/celebration", focalY: 0.72 },
  { src: "/images/cakes/november-bento.jpg", alt: "November birthday bento cake set", label: "Bento · R400", to: "/cakes/bento", focalY: 0.5 },
  { src: "/images/cakes/frozen-kids-cake.jpg", alt: "Frozen castle kids' birthday cake", label: "Kids' theme", to: "/cakes/kids", focalY: 0.45 },
];

const FACTS = [
  { value: "R400", label: "Bento cakes from" },
  { value: "R750", label: "Celebration cakes from" },
  { value: "5", label: "Ranges, any design" },
];

const STRIP = [
  { icon: Palette, title: "Any design you choose", body: "Theme, colours and message, or send a photo." },
  { icon: Clock3, title: "Made to order", body: "Nothing is baked before you order it." },
  { icon: MapPin, title: BAKERY.areas, body: "Collection or delivery, confirmed per order." },
  { icon: MessageCircle, title: `WhatsApp ${BAKERY.phone}`, body: "The quickest way to order.", href: whatsappLink("Hi Indulge, I'd like to order a cake.") },
];

function Tile({ item, className, sizes, priority }) {
  return (
    <Link to={item.to} className={`group relative block overflow-hidden rounded-[22px] bg-secondary ${className}`}>
      <Image
        src={item.src}
        alt={item.alt}
        className="absolute inset-0 h-full w-full transition-transform duration-700 group-hover:scale-[1.05]"
        fittingType="fill"
        focalPointY={item.focalY}
        sizes={sizes}
        priority={priority}
      />
      <span className="absolute bottom-2.5 left-2.5 max-w-[calc(100%-1.25rem)] truncate rounded-full bg-white/90 px-2.5 py-1 text-[10px] font-medium uppercase tracking-[0.1em] text-foreground shadow-sm backdrop-blur">
        {item.label}
      </span>
    </Link>
  );
}

export default function Hero() {
  const [big, ...rest] = MOSAIC;

  return (
    <section className="border-b border-border bg-background" aria-label="Indulge Cakes Bakery">
      <div className="u-container grid items-center gap-10 pb-12 pt-10 md:pb-16 md:pt-14 lg:grid-cols-[1fr_1.05fr] lg:gap-14">
        {/* Text */}
        <div className="animate-fade-up">
          <p className="u-eyebrow">Custom cakes · {BAKERY.areas}</p>
          <h1 className="mt-4 max-w-[14ch] font-heading text-[2.6rem] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-[3.4rem] lg:text-[4rem]">
            The art of baking <span className="font-serif font-normal italic text-[hsl(var(--gold-ink))]">happiness.</span>
          </h1>
          <p className="mt-5 max-w-[46ch] text-[15px] leading-relaxed text-muted-foreground sm:text-base">
            Bento cakes, celebration cakes, kids&rsquo; theme cakes, cupcakes, biscuits and scones. Every one is made to order by {BAKERY.baker}, in any design
            you choose.
          </p>

          <div className="mt-7 flex flex-wrap items-center gap-3">
            <Link
              to="/cakes"
              className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              Start your creation <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <a
              href={whatsappLink("Hi Indulge, I'd like to order a cake.")}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-full border border-border px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
            >
              <MessageCircle className="h-3.5 w-3.5" /> Order on WhatsApp
            </a>
          </div>

          <dl className="mt-9 grid max-w-md grid-cols-3 gap-4 border-t border-border pt-6">
            {FACTS.map((fact) => (
              <div key={fact.label}>
                <dt className="sr-only">{fact.label}</dt>
                <dd className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">{fact.value}</dd>
                <dd className="mt-1 text-[11px] leading-snug text-muted-foreground">{fact.label}</dd>
              </div>
            ))}
          </dl>
        </div>

        {/* Photo mosaic */}
        <div className="animate-fade-up mx-auto grid w-full max-w-[520px] grid-cols-3 gap-3 sm:gap-4 lg:mr-0" style={{ animationDelay: "0.15s" }}>
          <Tile item={big} className="col-span-2 row-span-2" sizes="(min-width: 640px) 340px, 66vw" priority />
          <Tile item={rest[0]} className="aspect-[4/5]" sizes="(min-width: 640px) 170px, 33vw" priority />
          <Tile item={rest[1]} className="aspect-[4/5]" sizes="(min-width: 640px) 170px, 33vw" />
        </div>
      </div>

      {/* Key facts strip */}
      <div className="border-t border-border bg-secondary/50">
        <ul className="u-container grid gap-6 py-7 sm:grid-cols-2 lg:grid-cols-4">
          {STRIP.map((item) => {
            const inner = (
              <>
                <item.icon className="mt-0.5 h-5 w-5 shrink-0 text-[hsl(var(--gold-ink))]" strokeWidth={1.6} />
                <span>
                  <span className="block text-sm font-medium text-foreground">{item.title}</span>
                  <span className="mt-0.5 block text-xs leading-relaxed text-muted-foreground">{item.body}</span>
                </span>
              </>
            );
            return (
              <li key={item.title}>
                {item.href ? (
                  <a href={item.href} target="_blank" rel="noreferrer" className="flex gap-3 rounded-xl transition-opacity hover:opacity-75">
                    {inner}
                  </a>
                ) : (
                  <div className="flex gap-3">{inner}</div>
                )}
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}
