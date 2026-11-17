import { createFileRoute, Link } from "@tanstack/react-router";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { PROJECTS } from "@/lib/work";

export const Route = createFileRoute("/_site/compare")({
  head: () => ({
    meta: [
      { title: "Compare projects | Vukosi Rikhotso" },
      {
        name: "description",
        content: "Vukosi Rikhotso's projects side by side: platform, technology and what each one does.",
      },
    ],
  }),
  component: ComparePage,
});

function ComparePage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">Compare</p>
            <h1 className="mt-3 max-w-[22ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              Every project, side by side.
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              Different platforms, different problems. Here is what each project runs on, what it is built with, and what it does.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Reveal>
            <div className="overflow-x-auto rounded-[26px] border border-border bg-card">
              <table className="w-full min-w-[860px] border-collapse text-left">
                <thead>
                  <tr className="border-b border-border">
                    <th className="w-[22%] px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Project</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Platform</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">Built with</th>
                    <th className="px-6 py-5 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">What it does</th>
                  </tr>
                </thead>
                <tbody>
                  {PROJECTS.map((project) => (
                    <tr key={project.slug} className="border-b border-border/70 last:border-0">
                      <th scope="row" className="px-6 py-6 align-top">
                        <Link to="/projects/$slug" params={{ slug: project.slug }} className="font-heading text-base font-semibold tracking-tight underline-offset-4 hover:underline">
                          {project.name}
                        </Link>
                        <p className="mt-1 text-xs font-normal text-muted-foreground">{project.eyebrow}</p>
                      </th>
                      <td className="px-6 py-6 align-top text-sm">{project.platform}</td>
                      <td className="px-6 py-6 align-top">
                        <ul className="space-y-1 text-sm text-foreground/80">
                          {project.stack.slice(0, 5).map((tool) => (
                            <li key={tool}>{tool}</li>
                          ))}
                        </ul>
                      </td>
                      <td className="px-6 py-6 align-top text-sm text-muted-foreground">{project.summary}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-4 text-xs text-muted-foreground">
              Skyline Weather and the HP 12C calculator are live on the web. Code for several projects is public on GitHub.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {PROJECTS.map((project, index) => (
              <Reveal key={project.slug} delay={index * 0.04}>
                <Link
                  to="/projects/$slug"
                  params={{ slug: project.slug }}
                  className="flex h-full flex-col justify-between rounded-2xl border border-border p-5 transition-colors hover:border-foreground/25"
                >
                  <span className="font-heading text-base font-semibold tracking-tight">{project.name}</span>
                  <span className="mt-6 text-[11px] uppercase tracking-[0.12em] text-muted-foreground">{project.meta}</span>
                </Link>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
