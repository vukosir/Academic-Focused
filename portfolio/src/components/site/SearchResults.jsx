import { Link } from "@tanstack/react-router";
import { ArrowUpRight } from "lucide-react";

export default function SearchResults({ results, onNavigate }) {
  return (
    <div className="max-h-[52vh] overflow-y-auto p-2">
      {results.length === 0 ? (
        <p className="px-4 py-8 text-center text-sm text-muted-foreground">Nothing matched. Try “Kotlin”, “React” or “Android”.</p>
      ) : (
        <ul>
          {results.map((item) => (
            <li key={`${item.kind}-${item.label}`}>
              <Link
                to={item.to}
                onClick={onNavigate}
                className="flex items-center justify-between gap-4 rounded-2xl px-4 py-3 transition-colors hover:bg-secondary"
              >
                <span>
                  <span className="block text-sm font-medium text-foreground">{item.label}</span>
                  <span className="block text-xs text-muted-foreground">
                    {item.kind} · {item.hint}
                  </span>
                </span>
                <ArrowUpRight className="h-4 w-4 shrink-0 text-muted-foreground" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
