/**
 * The sun's path for today: a half circle from sunrise to sunset with the sun
 * placed where it currently is. After dark the arc dims and the caption counts
 * down to the next sunrise.
 */
import type { DailyPoint } from '../types';
import { daylightProgress, formatClock, formatDuration, wallDate } from '../lib/time';

interface Props {
  today: DailyPoint | undefined;
  tomorrow: DailyPoint | undefined;
  /** Wall-clock "now" at the place (see lib/time.ts). */
  now: Date;
}

// Geometry of the half circle inside a 320 x 176 viewBox.
const CX = 160;
const CY = 150;
const R = 138;
const ARC = `M${CX - R} ${CY} A${R} ${R} 0 0 1 ${CX + R} ${CY}`;

function minutesUntil(target: string | null | undefined, now: Date): number | null {
  const date = wallDate(target);
  if (Number.isNaN(date.getTime())) return null;
  const minutes = (date.getTime() - now.getTime()) / 60_000;
  return minutes >= 0 ? minutes : null;
}

export function SunArc({ today, tomorrow, now }: Props) {
  const sunrise = today?.sunrise ?? null;
  const sunset = today?.sunset ?? null;

  if (!sunrise || !sunset) {
    return <p className="sun-arc__missing">Sunrise and sunset times are not available for this place.</p>;
  }

  const progress = daylightProgress(sunrise, sunset, now);
  const isUp = progress !== null;

  let caption: string;
  if (isUp) {
    caption = `Sunset in ${formatDuration(minutesUntil(sunset, now))}`;
  } else {
    const untilRise = minutesUntil(sunrise, now) ?? minutesUntil(tomorrow?.sunrise, now);
    caption = untilRise === null ? 'The sun is down' : `Sunrise in ${formatDuration(untilRise)}`;
  }

  const summary = `Sunrise at ${formatClock(sunrise)}, sunset at ${formatClock(sunset)}. ${caption}.`;

  return (
    <figure className={`sun-arc${isUp ? '' : ' sun-arc--night'}`}>
      <svg viewBox="0 0 320 176" role="img" aria-label={summary} className="sun-arc__svg">
        {/* Full path, dotted */}
        <path d={ARC} className="sun-arc__track" pathLength={1} />
        {/* Part already travelled. pathLength=1 lets the dash length equal the progress. */}
        {isUp ? (
          <path
            d={ARC}
            className="sun-arc__done"
            pathLength={1}
            strokeDasharray={`${progress} 1`}
            style={{ ['--progress' as string]: progress }}
          />
        ) : null}
        <line x1="4" y1={CY} x2="316" y2={CY} className="sun-arc__horizon" />
        {isUp ? (
          // Rotating around the arc's centre moves the sun along the path, which
          // lets CSS animate it from sunrise to its current position on load.
          <g
            className="sun-arc__sun"
            style={{ ['--angle' as string]: `${(progress ?? 0) * 180}deg`, transformOrigin: `${CX}px ${CY}px` }}
          >
            <circle cx={CX - R} cy={CY} r="17" className="sun-arc__halo" />
            <circle cx={CX - R} cy={CY} r="9" className="sun-arc__disc" />
          </g>
        ) : null}
      </svg>
      <figcaption className="sun-arc__caption" aria-hidden="true">
        <span className="sun-arc__time">
          <span className="sun-arc__label">Sunrise</span>
          {formatClock(sunrise)}
        </span>
        <span className="sun-arc__status">{caption}</span>
        <span className="sun-arc__time sun-arc__time--end">
          <span className="sun-arc__label">Sunset</span>
          {formatClock(sunset)}
        </span>
      </figcaption>
    </figure>
  );
}
