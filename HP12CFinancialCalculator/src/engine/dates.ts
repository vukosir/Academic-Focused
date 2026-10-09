/**
 * Calendar helpers. The HP 12C takes dates as numbers: M.DDYYYY in M.DY mode
 * and DD.MMYYYY in D.MY mode, valid from 15 Oct 1582 to 25 Nov 4046.
 */

export interface YMD {
  y: number;
  m: number;
  d: number;
}

const MIN = Date.UTC(1582, 9, 15);
const MAX = Date.UTC(4046, 10, 25);

function toUtc({ y, m, d }: YMD): number {
  const t = new Date(0);
  t.setUTCFullYear(y, m - 1, d);
  t.setUTCHours(0, 0, 0, 0);
  return t.getTime();
}

function isReal({ y, m, d }: YMD): boolean {
  if (m < 1 || m > 12 || d < 1 || d > 31) return false;
  const t = new Date(toUtc({ y, m, d }));
  const ok = t.getUTCFullYear() === y && t.getUTCMonth() === m - 1 && t.getUTCDate() === d;
  return ok && toUtc({ y, m, d }) >= MIN && toUtc({ y, m, d }) <= MAX;
}

/** Reads a calculator date number. Returns null when it is not a real date. */
export function parseDate(x: number, dmy: boolean): YMD | null {
  if (!Number.isFinite(x) || x < 0) return null;
  const whole = Math.floor(x + 1e-9);
  const frac = Math.round((x - whole) * 1e6);
  const dd = Math.floor(frac / 10_000);
  const y = frac % 10_000;
  const date: YMD = dmy ? { d: whole, m: dd, y } : { m: whole, d: dd, y };
  return isReal(date) ? date : null;
}

/** Writes a date back as a calculator number. */
export function dateToNumber({ y, m, d }: YMD, dmy: boolean): number {
  const first = dmy ? d : m;
  const second = dmy ? m : d;
  return Number(`${first}.${String(second).padStart(2, '0')}${String(y).padStart(4, '0')}`);
}

const DAY_MS = 86_400_000;

export function actualDays(a: YMD, b: YMD): number {
  return Math.round((toUtc(b) - toUtc(a)) / DAY_MS);
}

/** US 30/360 day count, as used for bonds. */
export function days360(a: YMD, b: YMD): number {
  const d1 = a.d === 31 ? 30 : a.d;
  const d2 = b.d === 31 && d1 >= 30 ? 30 : b.d;
  return 360 * (b.y - a.y) + 30 * (b.m - a.m) + (d2 - d1);
}

/** Adds days to a date. Returns null if the result leaves the supported range. */
export function addDays(a: YMD, days: number): YMD | null {
  const t = toUtc(a) + days * DAY_MS;
  if (t < MIN || t > MAX) return null;
  const r = new Date(t);
  return { y: r.getUTCFullYear(), m: r.getUTCMonth() + 1, d: r.getUTCDate() };
}

/** Day of week as the HP 12C shows it: 1 = Monday ... 7 = Sunday. */
export function dayOfWeek(a: YMD): number {
  const w = new Date(toUtc(a)).getUTCDay();
  return w === 0 ? 7 : w;
}

/** Moves a date by whole months, clamping the day to the end of the month. */
export function addMonths(a: YMD, months: number): YMD {
  const total = a.y * 12 + (a.m - 1) + months;
  const y = Math.floor(total / 12);
  const m = (total % 12) + 1;
  const last = new Date(Date.UTC(y, m, 0)).getUTCDate();
  return { y, m, d: Math.min(a.d, last) };
}

export function compare(a: YMD, b: YMD): number {
  return a.y - b.y || a.m - b.m || a.d - b.d;
}

export const DAY_NAMES = ['', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
