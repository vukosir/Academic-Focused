import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft, ArrowRight, ArrowUpRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import GalleryGrid from "@/components/site/GalleryGrid";
import RevealLinks from "@/components/site/RevealLinks";
import { PROJECTS, SKILLS, getProject } from "@/lib/work";

export const Route = createFileRoute("/_site/projects/$slug")({
  loader: ({ params }) => {
    const project = getProject(params.slug);
    if (!project) throw notFound();
    return { project, related: PROJECTS.filter((item) => item.slug !== params.slug).slice(0, 3) };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.project?.name ?? "Project"} | Vukosi Rikhotso` },
      { name: "description", content: loaderData?.project?.summary ?? "A project by Vukosi Rikhotso." },
    ],
  }),
  component: ProjectPage,
});

function ProjectPage() {
  const { project, related } = Route.useLoaderData();
  const skills = SKILLS.filter((skill) => skill.projects.includes(project.slug));
  const hasLinks = project.links.live || project.links.code;

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Link to="/projects" className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> All projects
          </Link>

          <div className="mt-8 grid gap-10 lg:grid-cols-[1.05fr_1fr] lg:items-center lg:gap-16">
            <Reveal>
              <p className="u-eyebrow">{project.eyebrow}</p>
              <h1 className="mt-4 max-w-[16ch] font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-[56px]">{project.name}</h1>
              <p className="mt-3 font-serif text-2xl italic text-[hsl(var(--gold-ink))]">{project.tagline}</p>
              <p className="mt-6 max-w-lg text-[15px] leading-relaxed text-muted-foreground">{project.intro}</p>
              <p className="mt-6 inline-flex rounded-full bg-secondary px-4 py-2 text-[11px] font-medium uppercase tracking-[0.12em]">{project.meta}</p>
              <ul className="mt-8 grid gap-2.5">
                {project.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-3 text-sm text-foreground/80">
                    <span className="mt-[7px] h-1 w-1 shrink-0 rounded-full bg-accent" />
                    {feature}
                  </li>
                ))}
              </ul>
            </Reveal>

            <Reveal delay={0.08}>
              <div className="relative mx-auto aspect-[4/5] w-full max-w-[320px] overflow-hidden rounded-[28px] bg-secondary shadow-[0_40px_90px_-50px_rgba(29,29,31,0.55)] sm:max-w-[380px] lg:mr-0 lg:max-w-[420px]">
                <Image src={project.hero.src} alt={project.hero.alt} className="h-full w-full" fittingType="fill" priority sizes="(min-width: 640px) 420px, 320px" />
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="bg-secondary/40">
        <div className="u-container grid gap-8 py-16 md:py-20 lg:grid-cols-[1fr_1fr] lg:items-start lg:gap-16">
          <Reveal>
            <div className="rounded-[26px] border border-border bg-card p-6 md:p-8">
              <p className="u-eyebrow">Built with</p>
              <div className="mt-5 flex flex-wrap gap-2">
                {project.stack.map((tool) => (
                  <span key={tool} className="rounded-full border border-border px-4 py-2 text-xs">
                    {tool}
                  </span>
                ))}
              </div>
              <ul className="mt-7 divide-y divide-border border-t border-border">
                {project.highlights.map((item) => (
                  <li key={item.label} className="flex items-baseline justify-between gap-6 py-3">
                    <span className="text-sm font-medium">{item.label}</span>
                    <span className="shrink-0 text-xs text-muted-foreground">{item.detail}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.08} className="lg:pt-4">
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em]">See it for yourself</h2>
            {hasLinks ? (
              <div className="mt-6 flex flex-wrap gap-3">
                {project.links.live && (
                  <a
                    href={project.links.live}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
                  >
                    Open live app <ArrowUpRight className="h-3.5 w-3.5" />
                  </a>
                )}
                {project.links.code && (
                  <a
                    href={project.links.code}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-2 rounded-full border border-border px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-secondary"
                  >
                    View the code <ArrowUpRight className="h-3.5 w-3.5" />
                  </a>
                )}
              </div>
            ) : (
              <p className="mt-4 max-w-md text-sm leading-relaxed text-muted-foreground">
                The code for this project isn&rsquo;t public. I&rsquo;m happy to walk you through it, or show a demo, in an interview.
              </p>
            )}

            {skills.length > 0 && (
              <>
                <h3 className="mt-10 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Skills it shows</h3>
                <div className="mt-3 flex flex-wrap gap-2">
                  {skills.map((skill) => (
                    <Link
                      key={skill.slug}
                      to="/skills/$slug"
                      params={{ slug: skill.slug }}
                      className="rounded-full border border-border px-4 py-2 text-xs transition-colors hover:border-foreground/30 hover:bg-secondary"
                    >
                      {skill.name}
                    </Link>
                  ))}
                </div>
              </>
            )}

            <Link
              to="/contact"
              className="mt-10 inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-foreground underline-offset-8 hover:underline"
            >
              Ask me about {project.shortName} <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container py-16 md:py-24">
          <Reveal>
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">Screens from {project.shortName}</h2>
          </Reveal>
          <div className="mt-8">
            <GalleryGrid images={project.images} />
          </div>
        </div>
      </section>

      <RevealLinks
        title="Other projects"
        items={related.map((item) => ({
          to: `/projects/${item.slug}`,
          label: item.name,
          hint: item.meta,
          image: item.hero,
        }))}
      />
    </>
  );
}
