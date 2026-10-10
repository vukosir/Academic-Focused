/**
 * Day length and the moon. Sun times come from the forecast. The moon phase is
 * calculated on the device (see lib/moon.ts) because the API does not provide it.
 */
import { getMoonPhase, type MoonPhase } from '../lib/moon';
import { describeUv } from '../lib/scales';
import { formatClock, formatDayMonth, formatDuration, minutesBetween } from '../lib/time';
import type { DailyPoint } from '../types';

interface Props {
  today: DailyPoint | undefined;
  /** Southern-hemisphere viewers see the moon flipped left to right. */
  latitude: number;
  /** Real timestamp to compute the phase for. Injectable for tests. */
  now?: number;
}

const R = 34;
const C = 40;

/** Outline of the lit part of the disc for a phase fraction (0 new, 0.5 full). */
function litPath(phase: MoonPhase): string {
  // Width of the terminator ellipse: full radius at new and full moon, zero at the quarters.
  const rx = Math.abs(Math.cos(2 * Math.PI * phase.fraction)) * R;
  // Before first quarter and after last quarter the lit part is a crescent.
  const crescent = phase.fraction < 0.25 || phase.fraction > 0.75;
  const outer = `M${C} ${C - R} A${R} ${R} 0 0 1 ${C} ${C + R}`;
  const terminator = `A${rx.toFixed(2)} ${R} 0 0 ${crescent ? 0 : 1} ${C} ${C - R}`;
  return `${outer} ${terminator} Z`;
}

export function SunMoonPanel({ today, latitude, now = Date.now() }: Props) {
  const moon = getMoonPhase(now);
  const peakUv = today?.uvIndexMax ?? null;
  const dayLength = minutesBetween(today?.sunrise ?? null, today?.sunset ?? null);
  const nextFull = new Date(moon.nextFull);
  const nextNew = new Date(moon.nextNew);

  // The path above is drawn with the right side lit (waxing, northern view).
  // Waning mirrors it, and so does viewing from the southern hemisphere.
  const mirrored = moon.waxing === latitude < 0;
  const percent = Math.round(moon.illumination * 100);

  return (
    <div className="sunmoon">
      <dl className="sunmoon__sun">
        <div>
          <dt>Sunrise</dt>
          <dd>{formatClock(today?.sunrise ?? null)}</dd>
        </div>
        <div>
          <dt>Sunset</dt>
          <dd>{formatClock(today?.sunset ?? null)}</dd>
        </div>
        <div>
          <dt>Daylight</dt>
          <dd>{formatDuration(dayLength)}</dd>
        </div>
        <div>
          <dt>Peak UV today</dt>
          <dd>
            {peakUv === null ? '--' : peakUv.toFixed(1)}
            {peakUv === null ? null : <span className="sunmoon__qualifier">{describeUv(peakUv).label}</span>}
          </dd>
        </div>
      </dl>

      <div className="sunmoon__moon">
        <svg
          className="moon"
          width="80"
          height="80"
          viewBox="0 0 80 80"
          role="img"
          aria-label={`${moon.name}, ${percent} percent lit`}
        >
          <circle cx={C} cy={C} r={R} className="moon__dark" />
          <path d={litPath(moon)} className="moon__lit" transform={mirrored ? `translate(${2 * C} 0) scale(-1 1)` : undefined} />
          <circle cx={C} cy={C} r={R} className="moon__rim" />
        </svg>
        <div>
          <p className="sunmoon__phase">{moon.name}</p>
          <p className="sunmoon__detail">{percent}% of the disc is lit</p>
          <p className="sunmoon__detail">
            Full moon {formatDayMonth(nextFull)}, new moon {formatDayMonth(nextNew)}
          </p>
        </div>
      </div>
    </div>
  );
}
