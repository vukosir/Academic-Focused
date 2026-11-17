import { Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";

export default function RevealLinks({ title, items }) {
  return (
    <section className="border-t border-border bg-background">
      <div className="u-container py-16 md:py-20">
        <Reveal>
          <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em]">{title}</h2>
        </Reveal>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item, index) => (
            <Reveal key={item.to} delay={index * 0.05}>
              <Link to={item.to} className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-3 transition-colors hover:border-foreground/25">
                <div className="h-16 w-14 shrink-0 overflow-hidden rounded-xl bg-secondary">
                  <Image
                    src={item.image.src}
                    alt={item.image.alt}
                    className="h-full w-full transition-transform duration-500 group-hover:scale-105"
                    fittingType="fill"
                    sizes="56px"
                  />
                </div>
                <div>
                  <p className="text-sm font-medium">{item.label}</p>
                  <p className="mt-0.5 text-xs text-muted-foreground">{item.hint}</p>
                </div>
              </Link>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
