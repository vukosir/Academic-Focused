import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { Image } from "@/components/ui/image";
import Reveal from "@/components/site/Reveal";
import GalleryGrid from "@/components/site/GalleryGrid";
import RevealLinks from "@/components/site/RevealLinks";
import ClosingCta from "@/components/site/ClosingCta";
import { CAKE_RANGES, COLLECTIONS, getCollection } from "@/lib/cakes";

export const Route = createFileRoute("/_site/collections/$slug")({
  loader: ({ params }) => {
    const collection = getCollection(params.slug);
    if (!collection) throw notFound();
    const ranges = CAKE_RANGES.filter((range) => collection.ranges.includes(range.slug));
    return { collection, ranges };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.collection?.name ?? "Collection"} | Indulge Cakes Bakery` },
      { name: "description", content: loaderData?.collection?.blurb ?? "Custom cakes by occasion, made to order in Johannesburg & Pretoria." },
    ],
  }),
  component: CollectionPage,
});

function CollectionPage() {
  const { collection, ranges } = Route.useLoaderData();
  const images = ranges.flatMap((range) => range.images).slice(0, 8);
  const others = COLLECTIONS.filter((item) => item.slug !== collection.slug).slice(0, 3);

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <Link to="/collections" className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.1em] text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> Collections
          </Link>

          <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_1.1fr] lg:items-center lg:gap-16">
            <Reveal>
              <p className="u-eyebrow">Collection</p>
              <h1 className="mt-4 font-heading text-[38px] font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-[56px]">{collection.name}</h1>
              <p className="mt-5 max-w-md text-[15px] leading-relaxed text-muted-foreground">{collection.blurb}</p>
              <div className="mt-7 flex flex-wrap gap-2">
                {ranges.map((range) => (
                  <Link
                    key={range.slug}
                    to="/cakes/$slug"
                    params={{ slug: range.slug }}
                    className="rounded-full border border-border px-4 py-2 text-xs transition-colors hover:border-foreground/30 hover:bg-secondary"
                  >
                    {range.name} · {range.priceLabel}
                  </Link>
                ))}
              </div>
              <Link
                to="/order"
                className="mt-8 inline-flex rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
              >
                Request a quote
              </Link>
            </Reveal>

            <Reveal delay={0.08}>
              <div className="relative mx-auto aspect-[4/5] w-full max-w-[320px] overflow-hidden rounded-[28px] bg-secondary shadow-[0_40px_90px_-50px_rgba(29,29,31,0.55)] sm:max-w-[380px] lg:mr-0 lg:max-w-[420px]">
                <Image src={collection.image.src} alt={collection.image.alt} className="h-full w-full" fittingType="fill" priority sizes="(min-width: 640px) 420px, 320px" />
              </div>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container py-16 md:py-24">
          <Reveal>
            <h2 className="font-heading text-2xl font-semibold tracking-[-0.03em] sm:text-3xl">From the {collection.name.toLowerCase()} collection</h2>
          </Reveal>
          <div className="mt-8">
            <GalleryGrid images={images} />
          </div>
        </div>
      </section>

      <RevealLinks
        title="Other collections"
        items={others.map((item) => ({
          to: `/collections/${item.slug}`,
          label: item.name,
          hint: "Occasion",
          image: item.image,
        }))}
      />

      <ClosingCta />
    </>
  );
}
