/**
 * Time handling.
 *
 * Open-Meteo returns times as wall-clock strings in the place's own timezone
 * ("2026-10-09T18:45", no offset). To format them without the viewer's
 * timezone getting involved, each string is read as if it were UTC and then
 * formatted with timeZone "UTC". The resulting Date is only a carrier for the
 * wall-clock digits and must not be compared with real timestamps.
 */

const INVALID = Number.NaN;

/** Wall-clock string to a carrier Date. Returns an invalid Date for bad input. */
export function wallDate(local: string | null | undefined): Date {
  if (!local) return new Date(INVALID);
  const iso = local.length === 10 ? `${local}T00:00` : local;
  return new Date(`${iso}Z`);
}

/** The current wall-clock time at a place, as a carrier Date. */
export function wallNow(utcOffsetSeconds: number, now: number = Date.now()): Date {
  return new Date(now + utcOffsetSeconds * 1000);
}

const valid = (date: Date) => !Number.isNaN(date.getTime());

function format(date: Date, options: Intl.DateTimeFormatOptions, locale?: string): string {
  if (!valid(date)) return '--';
  return new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' }).format(date);
}

/** "18:45" or "6:45 PM", following the viewer's locale. */
export function formatClock(local: string | Date | null, locale?: string): string {
  const date = local instanceof Date ? local : wallDate(local);
  return format(date, { hour: 'numeric', minute: '2-digit' }, locale);
}

/** "18" / "6 PM" for hourly columns. */
export function formatHour(local: string, locale?: string): string {
  return format(wallDate(local), { hour: 'numeric' }, locale);
}

/** "Fri" */
export function formatWeekday(local: string, style: 'short' | 'long' = 'short', locale?: string): string {
  return format(wallDate(local), { weekday: style }, locale);
}

/** "9 Oct" */
export function formatDayMonth(local: string | Date, locale?: string): string {
  const date = local instanceof Date ? local : wallDate(local);
  return format(date, { day: 'numeric', month: 'short' }, locale);
}

/** "Friday 9 October, 18:45" */
export function formatLongDateTime(date: Date, locale?: string): string {
  return format(date, { weekday: 'long', day: 'numeric', month: 'long', hour: 'numeric', minute: '2-digit' }, locale);
}

/** Minutes between two wall-clock strings, or null if either is missing. */
export function minutesBetween(start: string | null, end: string | null): number | null {
  const a = wallDate(start);
  const b = wallDate(end);
  if (!valid(a) || !valid(b)) return null;
  return Math.round((b.getTime() - a.getTime()) / 60_000);
}

/** 754 -> "12 h 34 min" */
export function formatDuration(minutes: number | null): string {
  if (minutes === null || !Number.isFinite(minutes)) return '--';
  const total = Math.max(0, Math.round(minutes));
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

/** "just now", "4 min ago", "2 h ago" from a real timestamp. */
export function formatAge(timestamp: number, now: number = Date.now()): string {
  const minutes = Math.floor((now - timestamp) / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  return `${Math.floor(hours / 24)} d ago`;
}

/**
 * Where the sun is between sunrise and sunset, from 0 (rising) to 1 (setting).
 * Returns null outside daylight or when the times are unknown.
 */
export function daylightProgress(sunrise: string | null, sunset: string | null, now: Date): number | null {
  const rise = wallDate(sunrise);
  const set = wallDate(sunset);
  if (!valid(rise) || !valid(set) || !valid(now) || set <= rise) return null;
  const t = (now.getTime() - rise.getTime()) / (set.getTime() - rise.getTime());
  return t >= 0 && t <= 1 ? t : null;
}
