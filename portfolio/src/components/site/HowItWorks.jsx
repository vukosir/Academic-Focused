import Reveal from "@/components/site/Reveal";
import { HOW_STEPS } from "@/lib/site";

export default function HowItWorks() {
  return (
    <section className="bg-background">
      <div className="u-container py-14 md:py-20">
        <Reveal>
          <p className="u-eyebrow">How I work</p>
          <h2 className="mt-4 max-w-[24ch] font-heading text-3xl font-semibold leading-[1.05] tracking-[-0.035em] sm:text-4xl md:text-[44px]">
            Three steps from a problem to working software.
          </h2>
        </Reveal>

        <div className="mt-14 grid gap-10 md:grid-cols-3 md:gap-8">
          {HOW_STEPS.map((step, index) => (
            <Reveal key={step.step} delay={index * 0.08}>
              <div className="border-t border-border pt-6">
                <span className="font-heading text-xs font-semibold tracking-[0.16em] text-[hsl(var(--gold-ink))]">{step.step}</span>
                <h3 className="mt-4 font-heading text-xl font-semibold tracking-tight">{step.title}</h3>
                <p className="mt-2.5 max-w-sm text-sm leading-relaxed text-muted-foreground">{step.body}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
