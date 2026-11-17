import { LANGUAGES } from "@/lib/work";

export default function LanguageStrip() {
  return (
    <div className="grid gap-x-6 gap-y-8 sm:grid-cols-2 lg:grid-cols-3">
      {LANGUAGES.map((language) => (
        <div key={language.name} className="flex items-center gap-4 rounded-2xl bg-card p-4 ring-1 ring-border">
          <div className="flex -space-x-2" aria-hidden="true">
            {language.swatches.map((swatch) => (
              <span key={swatch} className="h-9 w-9 rounded-full ring-2 ring-card" style={{ backgroundColor: swatch }} />
            ))}
          </div>
          <span className="text-sm font-medium">{language.name}</span>
        </div>
      ))}
    </div>
  );
}
