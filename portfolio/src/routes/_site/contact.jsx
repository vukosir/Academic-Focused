import { createFileRoute } from "@tanstack/react-router";
import { ArrowUpRight, Mail, MapPin } from "lucide-react";
import Reveal from "@/components/site/Reveal";
import ContactForm from "@/components/site/ContactForm";
import { PROFILE } from "@/lib/site";

export const Route = createFileRoute("/_site/contact")({
  head: () => ({
    meta: [
      { title: "Contact | Vukosi Rikhotso" },
      {
        name: "description",
        content: "Get in touch with Vukosi Rikhotso about a job, internship, learnership or project. Send a message, email, or connect on LinkedIn.",
      },
    ],
  }),
  component: ContactPage,
});

const CHANNELS = [
  { icon: Mail, label: "Email", value: PROFILE.email, href: `mailto:${PROFILE.email}` },
  { icon: ArrowUpRight, label: "LinkedIn", value: PROFILE.linkedinHandle, href: PROFILE.linkedin, external: true },
  { icon: ArrowUpRight, label: "GitHub", value: PROFILE.githubHandle, href: PROFILE.github, external: true },
  { icon: MapPin, label: "Based in", value: PROFILE.location },
];

function ContactPage() {
  return (
    <section className="bg-background">
      <div className="u-container pb-20 pt-8 md:pb-28 md:pt-12">
        <Reveal>
          <p className="u-eyebrow">Contact</p>
          <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
            Tell me what you&rsquo;re building.
          </h1>
          <p className="mt-4 max-w-xl text-[15px] leading-relaxed text-muted-foreground">
            A role, an internship or learnership, a project or a question: send it below and I&rsquo;ll reply by email.
          </p>
        </Reveal>

        <div className="mt-12 grid gap-8 lg:grid-cols-[1fr_1.15fr] lg:items-start lg:gap-14">
          <Reveal>
            <div className="rounded-[26px] border border-border bg-secondary/40 p-6 md:p-7">
              <h2 className="font-heading text-lg font-semibold tracking-tight">Other ways to reach me</h2>

              <ul className="mt-5 divide-y divide-border">
                {CHANNELS.map((channel) => (
                  <li key={channel.label} className="flex items-center justify-between gap-4 py-3.5">
                    <span className="flex items-center gap-3 text-sm text-muted-foreground">
                      <channel.icon className="h-4 w-4 text-[hsl(var(--gold-ink))]" strokeWidth={1.7} />
                      {channel.label}
                    </span>
                    {channel.href ? (
                      <a
                        href={channel.href}
                        target={channel.external ? "_blank" : undefined}
                        rel={channel.external ? "noreferrer" : undefined}
                        className="truncate text-sm font-medium underline-offset-4 hover:underline"
                      >
                        {channel.value}
                      </a>
                    ) : (
                      <span className="truncate text-sm font-medium">{channel.value}</span>
                    )}
                  </li>
                ))}
              </ul>

              <div className="mt-6 rounded-2xl bg-card p-4 text-xs leading-relaxed text-muted-foreground ring-1 ring-border">
                <p className="font-medium text-foreground">Looking for internships, learnerships and junior developer roles.</p>
                <p className="mt-1">Final-year Diploma in IT (Software Development) student at {PROFILE.college}. References are available on request.</p>
              </div>
            </div>
          </Reveal>

          <Reveal delay={0.06}>
            <ContactForm />
          </Reveal>
        </div>
      </div>
    </section>
  );
}
