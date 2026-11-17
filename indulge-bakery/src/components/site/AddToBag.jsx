import { useState } from "react";
import { toast } from "sonner";
import { Check, Plus } from "lucide-react";
import { useBag } from "@/components/site/BagProvider";
import { PALETTES } from "@/lib/cakes";

export default function AddToBag({ range }) {
  const { add } = useBag();
  const [palette, setPalette] = useState(PALETTES[0].name);
  const [justAdded, setJustAdded] = useState(null);

  function handleAdd(option) {
    add({
      slug: range.slug,
      name: range.name,
      image: range.hero.src,
      option: option.name,
      palette,
      price: option.price,
    });
    setJustAdded(option.name);
    toast.success(`${option.name} added to your order bag`, {
      description: `${palette} · ${range.name}`,
    });
    window.setTimeout(() => setJustAdded(null), 1800);
  }

  return (
    <div className="rounded-[26px] border border-border bg-card p-6 md:p-8">
      <h2 className="font-heading text-xl font-semibold tracking-tight">Choose your build</h2>
      <p className="mt-1.5 text-sm text-muted-foreground">Pick an option and a palette. Add as many as you need. Everything is quoted per order.</p>

      <div className="mt-6">
        <p className="text-[11px] font-medium uppercase tracking-[0.16em] text-muted-foreground">Colour palette</p>
        <div className="mt-3 flex flex-wrap gap-2">
          {PALETTES.map((item) => {
            const selected = item.name === palette;
            return (
              <button
                key={item.name}
                type="button"
                onClick={() => setPalette(item.name)}
                aria-pressed={selected}
                className={`inline-flex items-center gap-2 rounded-full border px-3 py-2 text-xs transition-colors ${
                  selected ? "border-foreground bg-foreground text-background" : "border-border bg-background text-foreground hover:border-foreground/40"
                }`}
              >
                <span className="flex -space-x-1">
                  {item.swatches.map((swatch) => (
                    <span key={swatch} className="h-3.5 w-3.5 rounded-full ring-1 ring-black/10" style={{ backgroundColor: swatch }} />
                  ))}
                </span>
                {item.name}
              </button>
            );
          })}
        </div>
      </div>

      <ul className="mt-7 divide-y divide-border border-t border-border">
        {range.options.map((option) => (
          <li key={option.name} className="flex flex-wrap items-center justify-between gap-4 py-4">
            <div>
              <p className="text-sm font-medium">{option.name}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{option.price}</p>
            </div>
            <button
              type="button"
              onClick={() => handleAdd(option)}
              className="inline-flex items-center gap-1.5 rounded-full border border-foreground px-4 py-2 text-[11px] font-medium uppercase tracking-[0.08em] transition-colors hover:bg-foreground hover:text-background"
            >
              {justAdded === option.name ? (
                <>
                  <Check className="h-3.5 w-3.5" /> Added
                </>
              ) : (
                <>
                  <Plus className="h-3.5 w-3.5" /> Add
                </>
              )}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
