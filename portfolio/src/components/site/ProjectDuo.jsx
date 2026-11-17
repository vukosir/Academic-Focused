import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import { PROJECTS } from "@/lib/work";

export default function ProjectDuo({ slugs = ["studytime", "hp12c-calculator"], eyebrow = "Also built", title = "Study tools and a classic calculator." }) {
  const projects = slugs.map((slug) => PROJECTS.find((project) => project.slug === slug)).filter(Boolean);

  return (
    <section className="border-t border-border bg-secondary/40">
      <div className="u-container py-14 md:py-20">
        <Reveal>
          <p className="u-eyebrow">{eyebrow}</p>
          <h2 className="mt-4 max-w-[22ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl md:text-[42px]">{title}</h2>
        </Reveal>

        <div className="mt-12 grid gap-6 md:grid-cols-2">
          {projects.map((project, index) => (
            <Reveal key={project.slug} delay={index * 0.08}>
              <Link
                to="/projects/$slug"
                params={{ slug: project.slug }}
                className="group block overflow-hidden rounded-[26px] bg-card ring-1 ring-border transition-shadow duration-500 hover:shadow-[0_30px_60px_-30px_rgba(29,29,31,0.35)]"
              >
                <div className="relative aspect-[16/11] overflow-hidden bg-secondary">
                  <Image
                    src={project.hero.src}
                    alt={project.hero.alt}
                    className="h-full w-full transition-transform duration-700 group-hover:scale-[1.04]"
                    fittingType="fill"
                    focalPointY={0.3}
                  />
                </div>
                <div className="flex items-start justify-between gap-6 p-6 md:p-7">
                  <div>
                    <h3 className="font-heading text-xl font-semibold tracking-tight">{project.name}</h3>
                    <p className="mt-2 max-w-sm text-sm leading-relaxed text-muted-foreground">{project.summary}</p>
                    <p className="mt-4 text-[11px] uppercase tracking-[0.16em] text-muted-foreground">{project.meta}</p>
                  </div>
                  <ArrowRight className="mt-1 h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-300 group-hover:translate-x-1 group-hover:text-foreground" />
                </div>
              </Link>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
