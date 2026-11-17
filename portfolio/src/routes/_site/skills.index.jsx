import { createFileRoute, Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import LanguageStrip from "@/components/site/LanguageStrip";
import ClosingCta from "@/components/site/ClosingCta";
import { SKILLS } from "@/lib/work";

export const Route = createFileRoute("/_site/skills/")({
  head: () => ({
    meta: [
      { title: "Skills | Vukosi Rikhotso" },
      {
        name: "description",
        content: "Android development, web development, APIs and back end, databases, and security: the skills behind Vukosi Rikhotso's projects.",
      },
    ],
  }),
  component: SkillsPage,
});

function SkillsPage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">Skills</p>
            <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              What I build with.
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              The same projects, grouped by the skills they show. Each skill links to the work where I used it.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-16 pt-6 md:pb-24 md:pt-8">
          <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 sm:gap-x-5 lg:grid-cols-5">
            {SKILLS.map((skill, index) => (
              <Reveal key={skill.slug} delay={index * 0.06}>
                <Link to="/skills/$slug" params={{ slug: skill.slug }} className="group block">
                  <div className="relative aspect-[4/5] overflow-hidden rounded-[20px] bg-secondary">
                    <Image
                      src={skill.image.src}
                      alt={skill.image.alt}
                      className="absolute inset-0 h-full w-full transition-transform duration-700 group-hover:scale-[1.05]"
                      fittingType="fill"
                      sizes="(min-width: 1024px) 240px, (min-width: 640px) 33vw, 50vw"
                    />
                  </div>
                  <h2 className="mt-3 font-heading text-base font-semibold tracking-tight sm:text-lg">{skill.name}</h2>
                  <p className="mt-1 text-[13px] leading-relaxed text-muted-foreground">{skill.blurb}</p>
                </Link>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="border-t border-border bg-secondary/40">
        <div className="u-container grid gap-10 py-14 md:py-20 lg:grid-cols-[1fr_1.3fr] lg:gap-20">
          <Reveal>
            <p className="u-eyebrow">Languages</p>
            <h2 className="mt-4 max-w-[16ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl">Eight languages, one craft.</h2>
            <p className="mt-5 max-w-sm text-[15px] leading-relaxed text-muted-foreground">
              Also: Microsoft Office, social media management and analytics, and project management.
            </p>
          </Reveal>
          <Reveal delay={0.08}>
            <LanguageStrip />
          </Reveal>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
