import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import { Image } from "@/components/ui/image";
import { useBag } from "@/components/site/BagProvider";
import { PALETTES } from "@/lib/cakes";
import { whatsappLink } from "@/lib/site";

function paletteSwatches(name) {
  return PALETTES.find((palette) => palette.name === name)?.swatches || [];
}

export default function BagPanel({ onNavigate }) {
  const { items, setQuantity, remove } = useBag();

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border px-6 py-5">
        <h2 className="font-heading text-lg font-semibold tracking-tight">Your order bag</h2>
        <p className="mt-1 text-xs leading-relaxed text-muted-foreground">No payment is taken here. We confirm design, date and final price with you directly.</p>
      </div>

      {items.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="font-heading text-xl tracking-tight">Your bag is empty.</p>
          <p className="text-sm leading-relaxed text-muted-foreground">Pick a cake, an option and a palette, then send us your design brief.</p>
          <Link
            to="/cakes"
            onClick={onNavigate}
            className="mt-2 inline-flex items-center gap-2 rounded-full bg-foreground px-6 py-3 text-[11px] font-medium uppercase tracking-[0.08em] text-background"
          >
            Browse the cakes <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto px-6 py-5">
          <ul className="space-y-5">
            {items.map((item) => (
              <li key={item.key} className="flex gap-4">
                <div className="h-20 w-16 shrink-0 overflow-hidden rounded-xl bg-secondary">
                  <Image src={item.image} alt="" className="h-full w-full" fittingType="fill" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-heading text-sm font-semibold tracking-tight">{item.name}</p>
                  <p className="mt-0.5 text-xs text-muted-foreground">{item.option}</p>
                  <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                    <span className="flex gap-1">
                      {paletteSwatches(item.palette).map((swatch) => (
                        <span key={swatch} className="h-2.5 w-2.5 rounded-full ring-1 ring-black/10" style={{ backgroundColor: swatch }} />
                      ))}
                    </span>
                    <span className="truncate">{item.palette}</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <div className="inline-flex items-center rounded-full border border-border">
                      <button
                        type="button"
                        aria-label={`Decrease quantity of ${item.name}`}
                        onClick={() => setQuantity(item.key, item.quantity - 1)}
                        className="h-7 w-7 text-sm leading-none text-muted-foreground hover:text-foreground"
                      >
                        −
                      </button>
                      <span className="w-6 text-center text-xs font-medium">{item.quantity}</span>
                      <button
                        type="button"
                        aria-label={`Increase quantity of ${item.name}`}
                        onClick={() => setQuantity(item.key, item.quantity + 1)}
                        className="h-7 w-7 text-sm leading-none text-muted-foreground hover:text-foreground"
                      >
                        +
                      </button>
                    </div>
                    <button
                      type="button"
                      onClick={() => remove(item.key)}
                      className="text-[11px] uppercase tracking-[0.08em] text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="border-t border-border px-6 py-5">
        <p className="mb-3 text-xs text-muted-foreground">{items.length > 0 ? "Quoted per order once we have your design brief." : "Nothing in the bag yet."}</p>
        <Link
          to="/order"
          onClick={onNavigate}
          className="flex w-full items-center justify-center gap-2 rounded-full bg-foreground px-6 py-3.5 text-[11px] font-medium uppercase tracking-[0.08em] text-background transition-colors hover:bg-accent hover:text-accent-foreground"
        >
          Request a quote <ArrowRight className="h-3.5 w-3.5" />
        </Link>
        <a
          href={whatsappLink("Hi Indulge, I'd like to order a cake.")}
          target="_blank"
          rel="noreferrer"
          className="mt-3 block text-center text-xs text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
        >
          Or message us on WhatsApp
        </a>
      </div>
    </div>
  );
}
