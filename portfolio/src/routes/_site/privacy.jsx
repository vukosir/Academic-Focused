import { createFileRoute, Link } from "@tanstack/react-router";
import { PROFILE } from "@/lib/site";

export const Route = createFileRoute("/_site/privacy")({
  head: () => ({
    meta: [
      { title: "Privacy | Vukosi Rikhotso" },
      { name: "description", content: "What this site collects when you send a message, why, and how to ask for it to be removed." },
    ],
  }),
  component: PrivacyPage,
});

const SECTIONS = [
  [
    "What I collect",
    "When you send a message through the contact page, I receive your name, your email address, your company or organisation if you add one, the reason for your message and the message itself. Nothing else is collected about you.",
  ],
  ["Why I collect it", "Only to read your message and reply to it. Messages are stored privately, and only I can open them."],
  [
    "Sharing",
    "Your details are not sold or passed on. If you contact me by email or on LinkedIn, that conversation is held by those services under their own terms.",
  ],
  ["Cookies", "There are no advertising, analytics or tracking cookies. The only cookie is an essential one that keeps the site owner signed in."],
  ["Keeping and removing your details", "Messages are kept so I can refer back to them. If you would like your message removed, email me and I will delete it."],
];

function PrivacyPage() {
  return (
    <section className="bg-background">
      <div className="u-container max-w-3xl pb-24 pt-8 md:pt-12">
        <p className="u-eyebrow">Privacy</p>
        <h1 className="mt-5 font-heading text-[34px] font-semibold leading-[1.02] tracking-[-0.04em] sm:text-4xl md:text-5xl">Your details, and what I do with them.</h1>
        <p className="mt-5 text-[15px] leading-relaxed text-muted-foreground">
          This is the personal website of {PROFILE.fullName} ({PROFILE.location}). It shows my work and lets you get in touch.
        </p>

        <div className="mt-10 space-y-8 text-[15px] leading-relaxed text-muted-foreground">
          {SECTIONS.map(([title, text]) => (
            <div key={title}>
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">{title}</h2>
              <p className="mt-2">{text}</p>
            </div>
          ))}
          <div>
            <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">Contact</h2>
            <p className="mt-2">
              Email{" "}
              <a href={`mailto:${PROFILE.email}`} className="text-foreground underline underline-offset-4">
                {PROFILE.email}
              </a>
              .
            </p>
          </div>
        </div>

        <div className="mt-12 flex flex-wrap gap-4 border-t border-border pt-6 text-sm">
          <Link to="/contact" className="text-foreground underline underline-offset-4">
            Contact
          </Link>
          <Link to="/faq" className="text-muted-foreground hover:text-foreground">
            FAQ
          </Link>
        </div>
      </div>
    </section>
  );
}
