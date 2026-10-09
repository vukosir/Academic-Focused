/**
 * Moon phase, computed locally.
 *
 * Open-Meteo does not publish moon data, so the phase is derived from the
 * positions of the sun and moon along the ecliptic, using the low-precision
 * series from the Astronomical Almanac. The angle between them (the
 * elongation) is the phase: 0 degrees at new moon, 180 at full. The result is
 * good to about half a degree, which puts phase times within roughly an hour.
 */

export const SYNODIC_MONTH_DAYS = 29.530588853;
const DAY_MS = 86_400_000;
/** 1 January 2000, 12:00 UTC: the epoch the formulae count from. */
const J2000 = Date.UTC(2000, 0, 1, 12);

const sinDeg = (degrees: number) => Math.sin((degrees * Math.PI) / 180);
const wrap360 = (degrees: number) => ((degrees % 360) + 360) % 360;

export type MoonPhaseName =
  | 'New moon'
  | 'Waxing crescent'
  | 'First quarter'
  | 'Waxing gibbous'
  | 'Full moon'
  | 'Waning gibbous'
  | 'Last quarter'
  | 'Waning crescent';

export interface MoonPhase {
  /** 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter. */
  fraction: number;
  /** Share of the visible disc that is lit, 0 to 1. */
  illumination: number;
  name: MoonPhaseName;
  waxing: boolean;
  /** Timestamps (epoch ms) of the next full moon and the next new moon. */
  nextFull: number;
  nextNew: number;
}

const NAMES: MoonPhaseName[] = [
  'New moon',
  'Waxing crescent',
  'First quarter',
  'Waxing gibbous',
  'Full moon',
  'Waning gibbous',
  'Last quarter',
  'Waning crescent',
];

/** Elongation of the moon from the sun at a moment in time, as a fraction of a full circle. */
export function phaseFraction(time: number): number {
  const days = (time - J2000) / DAY_MS;
  const t = days / 36_525; // Julian centuries

  // Sun: mean longitude plus the equation of centre.
  const g = 357.528 + 0.9856003 * days;
  const sun = 280.46 + 0.9856474 * days + 1.915 * sinDeg(g) + 0.02 * sinDeg(2 * g);

  // Moon: mean longitude plus the six largest periodic terms.
  const moon =
    218.32 +
    481_267.881 * t +
    6.29 * sinDeg(135.0 + 477_198.87 * t) -
    1.27 * sinDeg(259.3 - 413_335.36 * t) +
    0.66 * sinDeg(235.7 + 890_534.22 * t) +
    0.21 * sinDeg(269.9 + 954_397.74 * t) -
    0.19 * sinDeg(357.5 + 35_999.05 * t) -
    0.11 * sinDeg(186.5 + 966_404.03 * t);

  return wrap360(moon - sun) / 360;
}

/**
 * When the moon next reaches `target` (a phase fraction) after `from`.
 * Starts from the average speed of the moon and refines the estimate a few
 * times, because the moon speeds up and slows down along its orbit.
 */
export function nextPhaseTime(target: number, from: number): number {
  let remaining = (target - phaseFraction(from) + 1) % 1;
  let time = from + remaining * SYNODIC_MONTH_DAYS * DAY_MS;
  for (let i = 0; i < 4; i += 1) {
    // Signed distance to the target, in the range -0.5 to 0.5 of a cycle.
    remaining = ((target - phaseFraction(time) + 1.5) % 1) - 0.5;
    time += remaining * SYNODIC_MONTH_DAYS * DAY_MS;
  }
  return time;
}

export function getMoonPhase(date: Date | number = Date.now()): MoonPhase {
  const time = typeof date === 'number' ? date : date.getTime();
  const fraction = phaseFraction(time);
  // Eight equal slices, each centred on one of the named phases.
  const index = Math.floor(fraction * 8 + 0.5) % 8;

  return {
    fraction,
    illumination: (1 - Math.cos(2 * Math.PI * fraction)) / 2,
    name: NAMES[index] ?? 'New moon',
    waxing: fraction < 0.5,
    nextFull: nextPhaseTime(0.5, time),
    nextNew: nextPhaseTime(0, time),
  };
}
