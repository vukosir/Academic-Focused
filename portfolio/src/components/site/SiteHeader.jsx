import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { Menu, Search } from "lucide-react";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Dialog, DialogContent, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { NAV_LINKS, PROFILE } from "@/lib/site";
import { SEARCH_ITEMS } from "@/lib/work";
import { useAuth } from "@/lib/AuthContext";
import SearchResults from "@/components/site/SearchResults";

export function Wordmark() {
  return (
    <Link to="/" className="group inline-flex items-baseline gap-1.5" aria-label={`${PROFILE.name}, home`}>
      <span className="font-heading text-[23px] font-semibold tracking-[-0.04em] text-foreground">vukosi</span>
      <span className="text-[12px] font-medium uppercase tracking-[0.24em] text-muted-foreground transition-colors group-hover:text-foreground">
        rikhotso
      </span>
    </Link>
  );
}

export default function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const { user, isAuthenticated } = useAuth();

  const results = query.trim()
    ? SEARCH_ITEMS.filter((item) => `${item.label} ${item.hint} ${item.kind}`.toLowerCase().includes(query.trim().toLowerCase()))
    : SEARCH_ITEMS.slice(0, 6);

  // Only the site owner signs in, so these links only appear once signed in.
  const accountLinks = isAuthenticated
    ? [...(user?.role === "admin" ? [{ to: "/admin", label: "Messages" }] : []), { to: "/account", label: "Account" }]
    : [];

  return (
    <header className="sticky top-0 z-50 border-b border-border/70 bg-background/80 backdrop-blur-xl">
      <div className="u-container flex h-16 items-center justify-between gap-6">
        <div className="flex items-center gap-8">
          <Wordmark />
          <nav aria-label="Primary" className="hidden items-center gap-7 lg:flex">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className="text-[15.7px] font-medium text-muted-foreground transition-colors hover:text-foreground"
                activeProps={{ className: "text-foreground" }}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-1.5">
          {accountLinks.map((link) => (
            <Link key={link.to} to={link.to} className="mr-2 hidden text-[13px] font-medium text-muted-foreground hover:text-foreground xl:inline">
              {link.label}
            </Link>
          ))}

          <Dialog open={searchOpen} onOpenChange={setSearchOpen}>
            <DialogTrigger asChild>
              <button
                type="button"
                aria-label="Search the site"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full text-foreground/80 transition-colors hover:bg-secondary hover:text-foreground"
              >
                <Search className="h-[18px] w-[18px]" strokeWidth={1.7} />
              </button>
            </DialogTrigger>
            <DialogContent className="top-[15%] max-w-lg translate-y-0 gap-0 rounded-3xl p-0">
              <DialogTitle className="sr-only">Search the site</DialogTitle>
              <div className="border-b border-border p-4">
                <Input
                  autoFocus
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search projects, skills, pages"
                  className="border-0 bg-transparent px-0 text-base shadow-none focus-visible:ring-0"
                />
              </div>
              <SearchResults results={results} onNavigate={() => setSearchOpen(false)} />
            </DialogContent>
          </Dialog>

          <Link
            to="/contact"
            className="ml-2 hidden rounded-full bg-foreground px-5 py-2.5 text-[12px] font-medium uppercase tracking-[0.06em] text-background transition-colors hover:bg-accent hover:text-accent-foreground sm:inline-flex"
          >
            Contact
          </Link>

          <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger asChild>
              <button
                type="button"
                aria-label="Open menu"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full text-foreground transition-colors hover:bg-secondary lg:hidden"
              >
                <Menu className="h-5 w-5" strokeWidth={1.7} />
              </button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[86%] max-w-sm p-0">
              <SheetTitle className="sr-only">Menu</SheetTitle>
              <div className="flex h-full flex-col overflow-y-auto px-7 pb-10 pt-8">
                <Wordmark />
                <nav aria-label="Mobile" className="mt-10 flex flex-col gap-1">
                  <Link to="/" onClick={() => setMenuOpen(false)} className="border-b border-border/70 py-4 font-heading text-2xl tracking-tight">
                    Home
                  </Link>
                  {NAV_LINKS.map((link) => (
                    <Link
                      key={link.to}
                      to={link.to}
                      onClick={() => setMenuOpen(false)}
                      className="border-b border-border/70 py-4 font-heading text-2xl tracking-tight"
                    >
                      {link.label}
                    </Link>
                  ))}
                  {accountLinks.map((link) => (
                    <Link key={link.to} to={link.to} onClick={() => setMenuOpen(false)} className="py-3 text-sm text-muted-foreground">
                      {link.label}
                    </Link>
                  ))}
                </nav>
                <Link
                  to="/contact"
                  onClick={() => setMenuOpen(false)}
                  className="mt-8 inline-flex items-center justify-center rounded-full bg-foreground px-6 py-4 text-[12px] font-medium uppercase tracking-[0.08em] text-background"
                >
                  Get in touch
                </Link>
                <p className="mt-auto pt-8 text-xs leading-relaxed text-muted-foreground">
                  {PROFILE.location} · {PROFILE.email}
                </p>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
