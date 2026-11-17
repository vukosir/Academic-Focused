import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import Reveal from "@/components/site/Reveal";
import GalleryGrid from "@/components/site/GalleryGrid";
import ClosingCta from "@/components/site/ClosingCta";
import { GALLERY } from "@/lib/work";

export const Route = createFileRoute("/_site/gallery")({
  head: () => ({
    meta: [
      { title: "Gallery | Vukosi Rikhotso" },
      { name: "description", content: "Screens from Vukosi Rikhotso's projects: Android apps, web apps and full-stack websites." },
    ],
  }),
  component: GalleryPage,
});

function GalleryPage() {
  const filters = ["All", ...Array.from(new Set(GALLERY.map((image) => image.project)))];
  const [active, setActive] = useState("All");
  const shown = active === "All" ? GALLERY : GALLERY.filter((image) => image.project === active);

  return (
    <>
      <section className="border-b border-border bg-background">
        <div className="u-container pb-6 pt-8 md:pb-8 md:pt-10">
          <Reveal>
            <p className="u-eyebrow">Gallery</p>
            <h1 className="mt-3 max-w-[18ch] font-heading text-[32px] font-semibold leading-[1] tracking-[-0.04em] sm:text-4xl md:text-5xl">
              Every screen started as a problem.
            </h1>
            <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              Screenshots from the apps and websites I have built. StudyTime is shown with a cover in the app&rsquo;s own colours.
            </p>
          </Reveal>

          <div className="mt-9 flex flex-wrap gap-2">
            {filters.map((filter) => (
              <button
                key={filter}
                type="button"
                onClick={() => setActive(filter)}
                aria-pressed={active === filter}
                className={`rounded-full border px-4 py-2 text-xs transition-colors ${
                  active === filter ? "border-foreground bg-foreground text-background" : "border-border text-foreground/70 hover:border-foreground/30 hover:text-foreground"
                }`}
              >
                {filter}
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-background">
        <div className="u-container pb-12 pt-6 md:pb-16 md:pt-8">
          <GalleryGrid images={shown} showProject />
        </div>
      </section>

      <ClosingCta />
    </>
  );
}
