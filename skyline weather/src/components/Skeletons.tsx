/**
 * Loading placeholders. Each one mirrors the layout of the content it stands
 * in for, so the page does not jump when data arrives.
 */

function Bar({ w, h = 14 }: { w: string | number; h?: number }) {
  return <span className="skeleton" style={{ width: w, height: h }} />;
}

export function HeroSkeleton() {
  return (
    <div className="hero hero--loading" aria-busy="true">
      <span className="visually-hidden" role="status">
        Loading the forecast
      </span>
      <div className="hero__main" aria-hidden="true">
        <Bar w="min(60%, 260px)" h={34} />
        <Bar w="min(40%, 180px)" h={16} />
        <Bar w="min(52%, 230px)" h={132} />
        <Bar w="min(70%, 300px)" h={22} />
      </div>
      <div className="hero__arc" aria-hidden="true">
        <Bar w="100%" h={150} />
      </div>
    </div>
  );
}

export function PanelSkeleton({ rows = 4, label }: { rows?: number; label: string }) {
  return (
    <div className="panel-skeleton" aria-busy="true">
      <span className="visually-hidden" role="status">
        {label}
      </span>
      <div aria-hidden="true">
        {Array.from({ length: rows }, (_, i) => (
          <Bar key={i} w={`${92 - ((i * 17) % 40)}%`} h={18} />
        ))}
      </div>
    </div>
  );
}

/** The whole sheet while the first forecast for a place is loading. */
export function SheetSkeleton() {
  return (
    <div className="sheet" aria-hidden="true">
      <div className="sheet__grid">
        <div className="panel panel--wide">
          <div className="panel-skeleton panel-skeleton--strip">
            {Array.from({ length: 12 }, (_, i) => (
              <Bar key={i} w={52} h={118} />
            ))}
          </div>
        </div>
        {[7, 6, 4, 4, 4].map((rows, i) => (
          <div className={`panel${i < 2 ? ' panel--half' : ''}`} key={i}>
            <div className="panel-skeleton">
              <div>
                <Bar w="38%" h={20} />
                {Array.from({ length: rows }, (_, row) => (
                  <Bar key={row} w={`${94 - ((row * 13) % 36)}%`} h={18} />
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
