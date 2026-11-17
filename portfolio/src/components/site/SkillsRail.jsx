import { Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import { SKILLS } from "@/lib/work";

export default function SkillsRail() {
  return (
    <section className="bg-background">
      <div className="u-container pt-14 md:pt-20">
        <Reveal>
          <div className="flex flex-wrap items-end justify-between gap-6">
            <div>
              <p className="u-eyebrow">Skills</p>
              <h2 className="mt-4 max-w-[20ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl md:text-[44px]">
                From the phone to the server.
              </h2>
            </div>
            <Link
              to="/skills"
              className="text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground underline-offset-8 hover:text-foreground hover:underline"
            >
              All skills
            </Link>
          </div>
        </Reveal>
      </div>

      <div className="u-no-scrollbar mt-10 overflow-x-auto pb-14 md:pb-20">
        <div className="flex gap-5 px-6 sm:px-8 lg:px-12">
          {SKILLS.map((skill, index) => (
            <Reveal key={skill.slug} delay={index * 0.05} className="shrink-0">
              <Link to="/skills/$slug" params={{ slug: skill.slug }} className="group block w-[260px] sm:w-[300px]">
                <div className="relative aspect-[4/5] overflow-hidden rounded-[26px] bg-secondary">
                  <Image
                    src={skill.image.src}
                    alt={skill.image.alt}
                    className="h-full w-full transition-transform duration-700 group-hover:scale-[1.05]"
                    fittingType="fill"
                    sizes="300px"
                  />
                </div>
                <h3 className="mt-4 font-heading text-lg font-semibold tracking-tight">{skill.name}</h3>
                <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{skill.blurb}</p>
              </Link>
            </Reveal>
          ))}
          <div className="w-6 shrink-0" aria-hidden="true" />
        </div>
      </div>
    </section>
  );
}
