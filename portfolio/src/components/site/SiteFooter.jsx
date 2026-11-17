import { Link } from "@tanstack/react-router";
import { ArrowUpRight } from "lucide-react";
import { FOOTER_LINKS, PROFILE } from "@/lib/site";

export default function SiteFooter() {
  return (
    <footer className="border-t border-border bg-secondary/60">
      <div className="u-container py-16 md:py-20">
        <div className="grid gap-12 md:grid-cols-2 lg:grid-cols-[1.3fr_2fr]">
          <div>
            <Link to="/" className="inline-flex items-baseline gap-1.5">
              <span className="font-heading text-[22px] font-semibold tracking-[-0.04em]">vukosi</span>
              <span className="text-[10px] font-medium uppercase tracking-[0.24em] text-muted-foreground">rikhotso</span>
            </Link>
            <p className="mt-5 max-w-xs font-heading text-xl leading-snug tracking-tight text-foreground">{PROFILE.tagline}</p>
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-muted-foreground">
              {PROFILE.role} and final-year IT student at {PROFILE.college}, based in {PROFILE.location}.
            </p>
            <div className="mt-6 flex flex-wrap gap-x-5 gap-y-2 text-sm">
              <a href={`mailto:${PROFILE.email}`} className="text-foreground underline-offset-4 hover:underline">
                {PROFILE.email}
              </a>
            </div>
            <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm">
              <a href={PROFILE.linkedin} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-foreground underline-offset-4 hover:underline">
                LinkedIn <ArrowUpRight className="h-3.5 w-3.5" />
              </a>
              <a href={PROFILE.github} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-foreground underline-offset-4 hover:underline">
                GitHub <ArrowUpRight className="h-3.5 w-3.5" />
              </a>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-8 sm:grid-cols-3">
            {FOOTER_LINKS.map((column) => (
              <div key={column.title}>
                <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-muted-foreground">{column.title}</h3>
                <ul className="mt-4 space-y-2.5">
                  {column.links.map((link, index) => (
                    <li key={`${link.label}-${index}`}>
                      <Link to={link.to} className="text-[13px] text-foreground/80 underline-offset-4 transition-colors hover:text-foreground hover:underline">
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-14 flex flex-col gap-3 border-t border-border pt-6 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>
            © {new Date().getFullYear()} {PROFILE.fullName}. Built in Gauteng.
          </p>
          <div className="flex flex-wrap items-center gap-5">
            <Link to="/privacy" className="hover:text-foreground">
              Privacy
            </Link>
            <Link to="/faq" className="hover:text-foreground">
              FAQ
            </Link>
            <Link to="/contact" className="hover:text-foreground">
              Contact
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
