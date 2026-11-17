import { createFileRoute, Link } from "@tanstack/react-router";
import { BAKERY } from "@/lib/site";

export const Route = createFileRoute("/_site/terms")({
  head: () => ({
    meta: [
      { title: "Terms & conditions | Indulge Cakes Bakery" },
      { name: "description", content: "How quotes, orders, payment, collection and delivery work at Indulge Cakes Bakery, Johannesburg & Pretoria." },
    ],
  }),
  component: TermsPage,
});

const TERMS = [
  [
    "Quotes and prices",
    "The starting prices shown on this site are for the base option of a range. Every other option is quoted per order, because the final price depends on size, servings and the design. A quote is only confirmed once Indulge has replied to your request.",
  ],
  [
    "No payment on this website",
    "This website takes no payment. Sending a quote request does not place a paid order. Payment is arranged directly with Indulge once the design, date and final price are agreed.",
  ],
  [
    "Confirming an order",
    "Every cake is made to order, so an order is confirmed only when the design, the date and the final price have been agreed with you. Send your date as early as you can. Availability is confirmed as part of your quote.",
  ],
  [
    "Design",
    "Cakes are made in the design the customer chooses. Inspiration photos are used to understand the direction you would like, and the final design is the one agreed with Indulge before baking. Every cake is finished by hand, so it may differ slightly from a reference picture.",
  ],
  [
    "Collection and delivery",
    "Collection and delivery are both possible. Choose one in your request, add your suburb for delivery, and Indulge will confirm what is available for your area as part of your quote.",
  ],
  ["Allergies and dietary needs", "If you have an allergy or dietary requirement, tell us before you order so we can discuss what is possible."],
  [
    "Changes and cancellations",
    "Because every cake is made to order, changes and cancellations depend on how far the order has progressed. Contact Indulge as soon as anything changes so we can work it out with you.",
  ],
];

function TermsPage() {
  return (
    <section className="bg-background">
      <div className="u-container max-w-3xl pb-24 pt-8 md:pt-12">
        <p className="u-eyebrow">Terms &amp; conditions</p>
        <h1 className="mt-5 font-heading text-[34px] font-semibold leading-[1.02] tracking-[-0.04em] sm:text-4xl md:text-5xl">Quoting, ordering and collecting.</h1>
        <p className="mt-5 text-[15px] leading-relaxed text-muted-foreground">
          These are the terms for ordering a cake from {BAKERY.name}, serving {BAKERY.areas}.
        </p>

        <div className="mt-10 space-y-8 text-[15px] leading-relaxed text-muted-foreground">
          {TERMS.map(([title, text]) => (
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
          <Link to="/privacy" className="text-foreground underline underline-offset-4">
            Privacy
          </Link>
          <Link to="/support" className="text-muted-foreground hover:text-foreground">
            Support
          </Link>
        </div>
      </div>
    </section>
  );
}
