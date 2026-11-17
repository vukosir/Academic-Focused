import { createFileRoute, Link } from "@tanstack/react-router";
import { BAKERY } from "@/lib/site";

export const Route = createFileRoute("/_site/privacy")({
  head: () => ({
    meta: [
      { title: "Privacy | Indulge Cakes Bakery" },
      { name: "description", content: "What Indulge Cakes Bakery collects when you send a quote request, why it is collected, and how to ask for it to be removed." },
    ],
  }),
  component: PrivacyPage,
});

const SECTIONS = [
  [
    "What we collect",
    "When you send a quote request you give us your name, a WhatsApp or phone number, an optional email address, the date you need the cake, the occasion, size or servings, the design you have in mind, whether you would like collection or delivery (and your suburb), and any inspiration photos you choose to attach. If you create an account, we also keep your name, email and a securely hashed password.",
  ],
  [
    "Why we collect it",
    "Only to quote your cake and make it: to understand the design, confirm the date and price, and reach you about your order. Your inspiration photos are stored privately: only the bakery can open them, and you can too if you were signed in when you sent them.",
  ],
  ["Payments", "No payment is taken on this website, and no card details are collected here. Payment is arranged directly with Indulge once your order is confirmed."],
  [
    "Sharing",
    "Your details are not sold or passed on. If you message us on WhatsApp, email or TikTok, that conversation is held by those services under their own terms.",
  ],
  ["Cookies", "We only use an essential cookie to keep you signed in. Your order bag is kept in your own browser. There are no advertising or tracking cookies."],
  [
    "Keeping and removing your details",
    "Request details are kept so orders can be quoted, baked and referred back to. If you would like your request or photos removed, message us and we will delete them.",
  ],
];

function PrivacyPage() {
  return (
    <section className="bg-background">
      <div className="u-container max-w-3xl pb-24 pt-8 md:pt-12">
        <p className="u-eyebrow">Privacy</p>
        <h1 className="mt-5 font-heading text-[34px] font-semibold leading-[1.02] tracking-[-0.04em] sm:text-4xl md:text-5xl">Your details, and what we do with them.</h1>
        <p className="mt-5 text-[15px] leading-relaxed text-muted-foreground">
          This site is run by {BAKERY.name} ({BAKERY.areas}), run by {BAKERY.baker}. It exists only to take cake enquiries and quote requests.
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
              WhatsApp {BAKERY.phone} or email{" "}
              <a href={`mailto:${BAKERY.email}`} className="text-foreground underline underline-offset-4">
                {BAKERY.email}
              </a>
              .
            </p>
          </div>
        </div>

        <div className="mt-12 flex flex-wrap gap-4 border-t border-border pt-6 text-sm">
          <Link to="/terms" className="text-foreground underline underline-offset-4">
            Terms &amp; conditions
          </Link>
          <Link to="/support" className="text-muted-foreground hover:text-foreground">
            Support
          </Link>
        </div>
      </div>
    </section>
  );
}
