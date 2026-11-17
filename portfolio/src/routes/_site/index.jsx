import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import Hero from "@/components/site/Hero";
import ProjectFeature from "@/components/site/ProjectFeature";
import ProjectDuo from "@/components/site/ProjectDuo";
import SkillsRail from "@/components/site/SkillsRail";
import GalleryGrid from "@/components/site/GalleryGrid";
import HowItWorks from "@/components/site/HowItWorks";
import LanguageStrip from "@/components/site/LanguageStrip";
import ClosingCta from "@/components/site/ClosingCta";
import Reveal from "@/components/site/Reveal";
import { GALLERY, getProject } from "@/lib/work";
import { PROFILE } from "@/lib/site";

export const Route = createFileRoute("/_site/")({
  head: () => ({
    meta: [
      { title: `${PROFILE.name} | ${PROFILE.role}` },
      {
        name: "description",
        content:
          "Vukosi Rikhotso, a software developer in Pretoria finishing a Diploma in IT (Software Development) at Rosebank College. Android apps, web apps and REST APIs in Kotlin, C#, Java, Python and TypeScript.",
      },
    ],
  }),
  component: HomePage,
});

// A mix of projects for the home gallery, rather than the first project's screens only.
const HOME_GALLERY = ["kinplus-fake-call", "skyline-week", "indulge-home", "spendsmart-budget", "hp12c-calculator", "explore-sa-home", "kinplus-help", "studytime-cover"]
  .map((name) => GALLERY.find((image) => image.src.endsWith(`/${name}.jpg`)))
  .filter(Boolean);

function HomePage() {
  return (
    <>
      <Hero />

      <section className="bg-background">
        <div className="u-container pb-2 pt-12 md:pb-4 md:pt-16">
          <Reveal>
            <div className="grid gap-6 md:grid-cols-[1.4fr_1fr] md:items-end md:gap-16">
              <h2 className="max-w-[24ch] font-heading text-[32px] font-semibold leading-[1.04] tracking-[-0.035em] sm:text-4xl md:text-5xl">
                Every project solves a real problem.
                <br />
                <span className="text-muted-foreground">From safety to study time.</span>
              </h2>
              <p className="max-w-sm text-[15px] leading-relaxed text-muted-foreground">
                I&rsquo;m a final-year software development student in {PROFILE.location}. Here is what I have built: Android apps, full-stack websites and the
                APIs behind them.
              </p>
            </div>
          </Reveal>
        </div>
      </section>

      <ProjectFeature project={getProject("kinplus")} tone="light" />
      <ProjectFeature project={getProject("indulge-cakes")} tone="dark" reverse />
      <ProjectFeature project={getProject("skyline-weather")} tone="light" />

      <ProjectDuo slugs={["spendsmart", "studytime"]} eyebrow="Android apps" title="Budgets and study plans, in your pocket." />
      <SkillsRail />

      <section className="border-t border-border bg-background">
        <div className="u-container py-14 md:py-20">
          <Reveal>
            <div className="flex flex-wrap items-end justify-between gap-6">
              <div>
                <p className="u-eyebrow">Gallery</p>
                <h2 className="mt-4 max-w-[20ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl md:text-[44px]">
                  Screens from the work.
                </h2>
              </div>
              <Link
                to="/gallery"
                className="text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground underline-offset-8 hover:text-foreground hover:underline"
              >
                See every screen
              </Link>
            </div>
          </Reveal>
          <Reveal delay={0.08} className="mt-10">
            <GalleryGrid images={HOME_GALLERY} />
          </Reveal>
        </div>
      </section>

      <HowItWorks />

      <section className="border-t border-border bg-secondary/40">
        <div className="u-container py-14 md:py-20">
          <div className="grid gap-10 lg:grid-cols-[1fr_1.3fr] lg:gap-20">
            <Reveal>
              <p className="u-eyebrow">Languages</p>
              <h2 className="mt-4 max-w-[16ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl">
                The languages I write in.
              </h2>
              <p className="mt-5 max-w-sm text-[15px] leading-relaxed text-muted-foreground">
                Proficient in Java, Python and C#, and building most of my recent work in Kotlin and TypeScript. Tools: GitHub, Visual Studio, VS Code, MySQL,
                SQL Server and Postman.
              </p>
              <Link
                to="/skills"
                className="mt-7 inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-foreground underline-offset-8 hover:underline"
              >
                See my skills <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Reveal>
            <Reveal delay={0.08}>
              <LanguageStrip />
            </Reveal>
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
