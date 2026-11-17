import { Link } from "@tanstack/react-router";
import { ArrowRight, GraduationCap, Languages, Mail, MapPin } from "lucide-react";
import { Image } from "@/components/ui/image";
import { PROFILE } from "@/lib/site";
import { PROJECTS } from "@/lib/work";

// A portrait, then screens from real projects, each linking to its page.
const MOSAIC = [
  { src: "/images/work/vukosi-rikhotso.jpg", alt: `Portrait of ${PROFILE.name}`, label: `${PROFILE.name} · About me`, to: "/about", focalY: 0.35 },
  { src: "/images/work/kinplus-safety.jpg", alt: "Kin+ safety app on two phones", label: "Kin+", to: "/projects/kinplus", focalY: 0.45 },
  { src: "/images/work/skyline-forecast.jpg", alt: "Skyline Weather forecast for Pretoria", label: "Skyline · Live", to: "/projects/skyline-weather", focalY: 0.2 },
];

const FACTS = [
  { value: String(PROJECTS.length), label: "Projects built" },
  { value: "8", label: "Programming languages" },
  { value: "2026", label: "Diploma completes" },
];

const STRIP = [
  { icon: GraduationCap, title: "Diploma in IT", body: `Software Development, ${PROFILE.college}.` },
  { icon: MapPin, title: PROFILE.location, body: "South Africa." },
  { icon: Languages, title: "Three languages", body: PROFILE.languages + "." },
  { icon: Mail, title: PROFILE.email, body: "The quickest way to reach me.", href: `mailto:${PROFILE.email}` },
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
    <section className="border-b border-border bg-background" aria-label={PROFILE.name}>
      <div className="u-container grid items-center gap-10 pb-12 pt-10 md:pb-16 md:pt-14 lg:grid-cols-[1fr_1.05fr] lg:gap-14">
        {/* Text */}
        <div className="animate-fade-up">
          <p className="u-eyebrow">
            {PROFILE.role} · {PROFILE.location}
          </p>
          <h1 className="mt-4 max-w-[14ch] font-heading text-[2.6rem] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-[3.4rem] lg:text-[4rem]">
            Software built to <span className="font-serif font-normal italic text-[hsl(var(--gold-ink))]">matter.</span>
          </h1>
          <p className="mt-5 max-w-[46ch] text-[15px] leading-relaxed text-muted-foreground sm:text-base">
            I&rsquo;m {PROFILE.name}, a final-year Diploma in IT (Software Development) student at {PROFILE.college}. I build Android apps, web apps and
            APIs in Kotlin, C#, Java, Python and TypeScript.
          </p>

          <div className="mt-7 flex flex-wrap items-center gap-3">
            <Link
              to="/projects"
              className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              View my work <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <Link
              to="/contact"
              className="inline-flex items-center gap-2 rounded-full border border-border px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
            >
              <Mail className="h-3.5 w-3.5" /> Get in touch
            </Link>
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

        {/* Screen mosaic */}
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
                <span className="min-w-0">
                  <span className="block truncate text-sm font-medium text-foreground">{item.title}</span>
                  <span className="mt-0.5 block text-xs leading-relaxed text-muted-foreground">{item.body}</span>
                </span>
              </>
            );
            return (
              <li key={item.title}>
                {item.href ? (
                  <a href={item.href} className="flex gap-3 rounded-xl transition-opacity hover:opacity-75">
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
