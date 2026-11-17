import { createFileRoute, Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { getProject } from "@/lib/work";
import { COMPETENCIES, EDUCATION, PROFILE } from "@/lib/site";

export const Route = createFileRoute("/_site/about")({
  head: () => ({
    meta: [
      { title: "About | Vukosi Rikhotso" },
      {
        name: "description",
        content:
          "Vukosi Rikhotso is an IT student in Pretoria completing a Diploma in IT (Software Development) at Rosebank College, with skills in programming, database design and systems analysis.",
      },
    ],
  }),
  component: AboutPage,
});

function AboutPage() {
  const kinplus = getProject("kinplus");
  const skyline = getProject("skyline-weather");

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-16 pt-8 md:pb-24 md:pt-12">
          <Reveal>
            <p className="u-eyebrow">About me</p>
            <h1 className="mt-3 max-w-[18ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              A developer in the making, one project at a time.
            </h1>
          </Reveal>

          <div className="mt-12 grid gap-12 lg:grid-cols-[1.15fr_1fr] lg:gap-20">
            <Reveal delay={0.05}>
              <div className="space-y-5 text-[15px] leading-relaxed text-muted-foreground">
                <p>
                  I&rsquo;m {PROFILE.fullName}, an IT student in {PROFILE.location}, in the final year of a Diploma in IT (Software Development) at{" "}
                  {PROFILE.college}. I have strong skills in programming, database design and systems analysis, and I&rsquo;m proficient in Java, Python and C#.
                </p>
                <p>
                  Most of my recent work is in Kotlin for Android and in React and TypeScript for the web, with APIs in ASP.NET Core and Node.js behind them.
                  I like software that holds up in real life: apps that keep working offline, timers that stay correct in the background, and sites that are
                  secure by default.
                </p>
                <p>
                  Beyond code, I have hands-on experience in administrative operations, digital marketing and IT project coordination. I&rsquo;m a quick learner
                  who works well both independently and as part of a team, and I communicate in English, Xitsonga and isiZulu.
                </p>
              </div>

              <blockquote className="mt-10 border-l border-accent pl-6">
                <p className="font-serif text-2xl italic leading-snug text-foreground">
                  &ldquo;My goal is to build a successful career in software development while making a positive impact through innovative technology solutions.&rdquo;
                </p>
                <footer className="mt-3 text-[11px] uppercase tracking-[0.16em] text-muted-foreground">{PROFILE.name}</footer>
              </blockquote>

              <h2 className="mt-12 font-heading text-2xl font-semibold tracking-[-0.03em]">Key strengths</h2>
              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {COMPETENCIES.map((value) => (
                  <div key={value.title} className="rounded-2xl border border-border p-5">
                    <h3 className="font-heading text-base font-semibold tracking-tight">{value.title}</h3>
                    <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{value.body}</p>
                  </div>
                ))}
              </div>

              <h2 className="mt-12 font-heading text-2xl font-semibold tracking-[-0.03em]">Education</h2>
              <ol className="mt-6 divide-y divide-border border-y border-border">
                {EDUCATION.map((item) => (
                  <li key={item.title} className="grid gap-1 py-5 sm:grid-cols-[1fr_auto] sm:gap-6">
                    <div>
                      <h3 className="font-heading text-base font-semibold tracking-tight">{item.title}</h3>
                      <p className="mt-0.5 text-sm text-foreground/80">{item.place}</p>
                      <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{item.body}</p>
                    </div>
                    <p className="text-[11px] uppercase tracking-[0.14em] text-muted-foreground sm:pt-1">{item.years}</p>
                  </li>
                ))}
              </ol>
            </Reveal>

            <Reveal delay={0.1}>
              <div className="mx-auto grid w-full max-w-[420px] gap-4 lg:mr-0">
                <div className="relative aspect-[4/5] overflow-hidden rounded-[28px] bg-secondary">
                  <Image src="/images/work/vukosi-rikhotso.jpg" alt={`Portrait of ${PROFILE.name}`} className="h-full w-full" fittingType="fill" priority />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="relative aspect-square overflow-hidden rounded-[22px] bg-secondary">
                    <Image src={kinplus.hero.src} alt={kinplus.hero.alt} className="h-full w-full" fittingType="fill" sizes="300px" />
                  </div>
                  <div className="relative aspect-square overflow-hidden rounded-[22px] bg-secondary">
                    <Image src={skyline.hero.src} alt={skyline.hero.alt} className="h-full w-full" fittingType="fill" focalPointY={0.2} sizes="300px" />
                  </div>
                </div>
              </div>

              <div className="mx-auto mt-8 max-w-[420px] rounded-[22px] bg-secondary/60 p-6 lg:mr-0">
                <p className="text-[11px] uppercase tracking-[0.16em] text-muted-foreground">At a glance</p>
                <dl className="mt-4 space-y-3 text-sm">
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted-foreground">Based in</dt>
                    <dd className="text-right font-medium">{PROFILE.location}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted-foreground">Studying</dt>
                    <dd className="text-right font-medium">Diploma in IT, 2024–2026</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted-foreground">Languages</dt>
                    <dd className="text-right font-medium">{PROFILE.languages}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted-foreground">References</dt>
                    <dd className="text-right font-medium">On request</dd>
                  </div>
                </dl>
                <Link
                  to="/contact"
                  className="mt-6 inline-flex rounded-full bg-foreground px-5 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
                >
                  Get in touch
                </Link>
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
