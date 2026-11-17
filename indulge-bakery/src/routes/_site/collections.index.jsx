import { createFileRoute, Link } from "@tanstack/react-router";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import ClosingCta from "@/components/site/ClosingCta";
import { COLLECTIONS } from "@/lib/cakes";

export const Route = createFileRoute("/_site/collections/")({
  head: () => ({
    meta: [
      { title: "Collections | Indulge Cakes Bakery" },
      {
        name: "description",
        content:
          "Cakes by occasion: birthdays, milestones, kids' parties, love & new beginnings, and tea time. Custom made to order in Johannesburg & Pretoria.",
      },
    ],
  }),
  component: CollectionsPage,
});

function CollectionsPage() {
  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">Collections</p>
            <h1 className="mt-3 max-w-[20ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              Cakes by occasion.
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              The same cakes, gathered by the moment they are made for. Every design is still yours to choose.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-16 pt-6 md:pb-24 md:pt-8">
          <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 sm:gap-x-5 lg:grid-cols-5">
            {COLLECTIONS.map((collection, index) => (
              <Reveal key={collection.slug} delay={index * 0.06}>
                <Link to="/collections/$slug" params={{ slug: collection.slug }} className="group block">
                  <div className="relative aspect-[4/5] overflow-hidden rounded-[20px] bg-secondary">
                    <Image
                      src={collection.image.src}
                      alt={collection.image.alt}
                      className="absolute inset-0 h-full w-full transition-transform duration-700 group-hover:scale-[1.05]"
                      fittingType="fill"
                      sizes="(min-width: 1024px) 240px, (min-width: 640px) 33vw, 50vw"
                    />
                  </div>
                  <h2 className="mt-3 font-heading text-base font-semibold tracking-tight sm:text-lg">{collection.name}</h2>
                  <p className="mt-1 text-[13px] leading-relaxed text-muted-foreground">{collection.blurb}</p>
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
