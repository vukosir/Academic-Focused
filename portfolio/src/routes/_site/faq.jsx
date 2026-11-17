import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowUpRight, Mail } from "lucide-react";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import Reveal from "@/components/site/Reveal";
import { FAQS, HOW_STEPS, PROFILE } from "@/lib/site";

export const Route = createFileRoute("/_site/faq")({
  head: () => ({
    meta: [
      { title: "FAQ | Vukosi Rikhotso" },
      {
        name: "description",
        content: "Questions recruiters ask Vukosi Rikhotso: roles wanted, studies, languages and tools, code samples, references and how to get in touch.",
      },
    ],
  }),
  component: FaqPage,
});

function FaqPage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-10 pt-8 md:pb-14 md:pt-12">
          <Reveal>
            <p className="u-eyebrow">FAQ</p>
            <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">Questions, answered.</h1>
            <p className="mt-4 max-w-xl text-[15px] leading-relaxed text-muted-foreground">
              What recruiters and teams usually want to know, and how I approach the work.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {HOW_STEPS.map((step, index) => (
              <Reveal key={step.step} delay={index * 0.06}>
                <div className="border-t border-border pt-6">
                  <span className="font-heading text-xs font-semibold tracking-[0.16em] text-[hsl(var(--gold-ink))]">{step.step}</span>
                  <h2 className="mt-4 font-heading text-lg font-semibold tracking-tight">{step.title}</h2>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.body}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container grid gap-12 py-16 md:py-24 lg:grid-cols-[1.3fr_1fr] lg:gap-20">
          <Reveal>
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">Frequently asked</h2>
            <Accordion type="single" collapsible className="mt-6">
              {FAQS.map((faq, index) => (
                <AccordionItem key={faq.q} value={`faq-${index}`} className="border-border">
                  <AccordionTrigger className="py-4 text-left text-[15px] font-medium hover:no-underline">{faq.q}</AccordionTrigger>
                  <AccordionContent className="pb-4 text-sm leading-relaxed text-muted-foreground">{faq.a}</AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </Reveal>

          <Reveal delay={0.08}>
            <div className="rounded-[26px] border border-border bg-card p-6 md:p-8">
              <p className="u-eyebrow">Talk to me</p>
              <h2 className="mt-3 font-heading text-2xl font-semibold tracking-[-0.03em]">Email is the quickest way.</h2>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">Or send a message through the site and I&rsquo;ll reply by email.</p>

              <div className="mt-6 space-y-3">
                <a
                  href={`mailto:${PROFILE.email}`}
                  className="flex items-center gap-3 rounded-2xl bg-foreground px-5 py-4 text-background transition-colors hover:bg-accent hover:text-accent-foreground"
                >
                  <Mail className="h-4 w-4" />
                  <span className="truncate text-sm font-medium">{PROFILE.email}</span>
                </a>
                <a
                  href={PROFILE.linkedin}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-3 rounded-2xl border border-border px-5 py-4 transition-colors hover:bg-secondary"
                >
                  <ArrowUpRight className="h-4 w-4" />
                  <span className="truncate text-sm font-medium">LinkedIn</span>
                </a>
              </div>

              <dl className="mt-7 space-y-3 border-t border-border pt-6 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Based in</dt>
                  <dd className="text-right font-medium">{PROFILE.location}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Diploma completes</dt>
                  <dd className="text-right font-medium">2026</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">References</dt>
                  <dd className="text-right font-medium">On request</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">GitHub</dt>
                  <dd className="text-right font-medium">
                    <a href={PROFILE.github} target="_blank" rel="noreferrer" className="underline-offset-4 hover:underline">
                      {PROFILE.githubHandle}
                    </a>
                  </dd>
                </div>
              </dl>

              <Link
                to="/contact"
                className="mt-7 flex items-center justify-center rounded-full bg-foreground px-5 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
              >
                Send a message
              </Link>
            </div>
          </Reveal>
        </div>
      </section>
    </>
  );
}
