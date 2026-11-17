import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import Reveal from "@/components/site/Reveal";
import { PROFILE } from "@/lib/site";

export default function ClosingCta() {
  return (
    <section className="bg-[#111112] text-white">
      <div className="u-container py-16 md:py-20">
        <Reveal>
          <p className="text-[11px] uppercase tracking-[0.2em] text-white/45">Open to internships, learnerships and junior roles</p>
          <h2 className="mt-5 max-w-[18ch] font-heading text-4xl font-semibold leading-[0.98] tracking-[-0.04em] sm:text-5xl md:text-6xl">
            Let&rsquo;s build something <span className="font-serif font-normal italic text-[#d9c4a9]">together.</span>
          </h2>
          <p className="mt-6 max-w-md text-[15px] leading-relaxed text-white/60">
            Tell me about the role, the project or the team. I&rsquo;ll get back to you by email.
          </p>
          <div className="mt-9 flex flex-wrap items-center gap-4">
            <Link
              to="/contact"
              className="inline-flex items-center gap-2 rounded-full bg-white px-7 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-[#111112] transition-colors hover:bg-[#b89c7d]"
            >
              Get in touch <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <a
              href={PROFILE.linkedin}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-full border border-white/25 px-7 py-4 text-[11px] font-medium uppercase tracking-[0.08em] text-white transition-colors hover:border-white/60 hover:bg-white/5"
            >
              Connect on LinkedIn
            </a>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
