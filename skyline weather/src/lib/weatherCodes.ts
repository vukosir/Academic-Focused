/**
 * WMO weather interpretation codes, as used by Open-Meteo, mapped to a label,
 * an icon and a broad group that drives the background scene.
 */

export type IconName =
  | 'clear-day'
  | 'clear-night'
  | 'partly-day'
  | 'partly-night'
  | 'cloudy'
  | 'fog'
  | 'drizzle'
  | 'rain'
  | 'heavy-rain'
  | 'sleet'
  | 'snow'
  | 'thunder'
  | 'unknown';

export type ConditionGroup = 'clear' | 'cloudy' | 'fog' | 'rain' | 'snow' | 'thunder' | 'unknown';

export interface Condition {
  label: string;
  icon: IconName;
  group: ConditionGroup;
}

interface Entry {
  label: string;
  day: IconName;
  night?: IconName;
  group: ConditionGroup;
}

const CODES: Record<number, Entry> = {
  0: { label: 'Clear sky', day: 'clear-day', night: 'clear-night', group: 'clear' },
  1: { label: 'Mostly clear', day: 'clear-day', night: 'clear-night', group: 'clear' },
  2: { label: 'Partly cloudy', day: 'partly-day', night: 'partly-night', group: 'cloudy' },
  3: { label: 'Overcast', day: 'cloudy', group: 'cloudy' },
  45: { label: 'Fog', day: 'fog', group: 'fog' },
  48: { label: 'Freezing fog', day: 'fog', group: 'fog' },
  51: { label: 'Light drizzle', day: 'drizzle', group: 'rain' },
  53: { label: 'Drizzle', day: 'drizzle', group: 'rain' },
  55: { label: 'Heavy drizzle', day: 'drizzle', group: 'rain' },
  56: { label: 'Light freezing drizzle', day: 'sleet', group: 'rain' },
  57: { label: 'Freezing drizzle', day: 'sleet', group: 'rain' },
  61: { label: 'Light rain', day: 'rain', group: 'rain' },
  63: { label: 'Rain', day: 'rain', group: 'rain' },
  65: { label: 'Heavy rain', day: 'heavy-rain', group: 'rain' },
  66: { label: 'Light freezing rain', day: 'sleet', group: 'rain' },
  67: { label: 'Freezing rain', day: 'sleet', group: 'rain' },
  71: { label: 'Light snow', day: 'snow', group: 'snow' },
  73: { label: 'Snow', day: 'snow', group: 'snow' },
  75: { label: 'Heavy snow', day: 'snow', group: 'snow' },
  77: { label: 'Snow grains', day: 'snow', group: 'snow' },
  80: { label: 'Light showers', day: 'rain', group: 'rain' },
  81: { label: 'Showers', day: 'rain', group: 'rain' },
  82: { label: 'Violent showers', day: 'heavy-rain', group: 'rain' },
  85: { label: 'Light snow showers', day: 'snow', group: 'snow' },
  86: { label: 'Snow showers', day: 'snow', group: 'snow' },
  95: { label: 'Thunderstorm', day: 'thunder', group: 'thunder' },
  96: { label: 'Thunderstorm with hail', day: 'thunder', group: 'thunder' },
  99: { label: 'Thunderstorm with heavy hail', day: 'thunder', group: 'thunder' },
};

const UNKNOWN: Condition = { label: 'Conditions unavailable', icon: 'unknown', group: 'unknown' };

/** Describe a WMO code. Unknown or missing codes get a neutral fallback. */
export function describeWeather(code: number | null, isDay = true): Condition {
  if (code === null) return UNKNOWN;
  const entry = CODES[code];
  if (!entry) return UNKNOWN;
  return { label: entry.label, icon: isDay ? entry.day : (entry.night ?? entry.day), group: entry.group };
}
