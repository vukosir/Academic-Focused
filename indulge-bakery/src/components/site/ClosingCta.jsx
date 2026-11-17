import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import Reveal from "@/components/site/Reveal";
import { whatsappLink } from "@/lib/site";

export default function ClosingCta() {
  return (
    <section className="bg-[#111112] text-white">
      <div className="u-container py-16 md:py-20">
        <Reveal>
          <p className="text-[11px] uppercase tracking-[0.2em] text-white/45">Where the sweetness meets creativity</p>
          <h2 className="mt-5 max-w-[18ch] font-heading text-4xl font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-6xl">
            Life&rsquo;s sweeter with <span className="font-serif font-normal italic text-[#d9c4a9]">Indulge.</span>
          </h2>
          <p className="mt-6 max-w-md text-[15px] leading-relaxed text-white/60">
            Send your design brief with the date, the occasion and any inspiration you have. Indulge comes back with the design, the date and the final price.
          </p>
          <div className="mt-9 flex flex-wrap items-center gap-4">
            <Link
              to="/order"
              className="inline-flex items-center gap-2 rounded-full bg-white px-7 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-[#111112] transition-colors hover:bg-[#b89c7d]"
            >
              Request a quote <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <a
              href={whatsappLink("Hi Indulge, I'd like to order a cake.")}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-full border border-white/25 px-7 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-white transition-colors hover:border-white/60 hover:bg-white/5"
            >
              Order on WhatsApp
            </a>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
