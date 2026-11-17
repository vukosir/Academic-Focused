import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import GalleryGrid from "@/components/site/GalleryGrid";
import RevealLinks from "@/components/site/RevealLinks";
import ClosingCta from "@/components/site/ClosingCta";
import { PROJECTS, SKILLS, getSkill } from "@/lib/work";

export const Route = createFileRoute("/_site/skills/$slug")({
  loader: ({ params }) => {
    const skill = getSkill(params.slug);
    if (!skill) throw notFound();
    const projects = skill.projects.map((slug) => PROJECTS.find((project) => project.slug === slug)).filter(Boolean);
    return { skill, projects };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.skill?.name ?? "Skill"} | Vukosi Rikhotso` },
      { name: "description", content: loaderData?.skill?.blurb ?? "A skill in Vukosi Rikhotso's software development work." },
    ],
  }),
  component: SkillPage,
});

function SkillPage() {
  const { skill, projects } = Route.useLoaderData();
  const images = projects.flatMap((project) => project.images).slice(0, 8);
  const others = SKILLS.filter((item) => item.slug !== skill.slug).slice(0, 3);

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Link to="/skills" className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> Skills
          </Link>

          <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_1.1fr] lg:items-center lg:gap-16">
            <Reveal>
              <p className="u-eyebrow">Skill</p>
              <h1 className="mt-4 font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-[56px]">{skill.name}</h1>
              <p className="mt-5 max-w-md text-[15px] leading-relaxed text-muted-foreground">{skill.blurb}</p>
              <div className="mt-6 flex flex-wrap gap-2">
                {skill.tools.map((tool) => (
                  <span key={tool} className="rounded-full bg-secondary px-3.5 py-1.5 text-xs">
                    {tool}
                  </span>
                ))}
              </div>
              <h2 className="mt-8 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Where I used it</h2>
              <div className="mt-3 flex flex-wrap gap-2">
                {projects.map((project) => (
                  <Link
                    key={project.slug}
                    to="/projects/$slug"
                    params={{ slug: project.slug }}
                    className="rounded-full border border-border px-4 py-2 text-xs transition-colors hover:border-foreground/30 hover:bg-secondary"
                  >
                    {project.name} · {project.platform}
                  </Link>
                ))}
              </div>
              <Link
                to="/contact"
                className="mt-8 inline-flex rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
              >
                Get in touch
              </Link>
            </Reveal>

            <Reveal delay={0.08}>
              <div className="relative mx-auto aspect-[4/5] w-full max-w-[320px] overflow-hidden rounded-[28px] bg-secondary shadow-[0_40px_90px_-50px_rgba(29,29,31,0.55)] sm:max-w-[380px] lg:mr-0 lg:max-w-[420px]">
                <Image src={skill.image.src} alt={skill.image.alt} className="h-full w-full" fittingType="fill" priority sizes="(min-width: 640px) 420px, 320px" />
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container py-16 md:py-24">
          <Reveal>
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">{skill.name} in my projects</h2>
          </Reveal>
          <div className="mt-8">
            <GalleryGrid images={images} />
          </div>
        </div>
      </section>

      <RevealLinks
        title="Other skills"
        items={others.map((item) => ({
          to: `/skills/${item.slug}`,
          label: item.name,
          hint: item.tools.slice(0, 3).join(", "),
          image: item.image,
        }))}
      />

      <ClosingCta />
    </>
  );
}
